const fs = require("fs");
const path = require("path");

const dir = process.env.DIR_DADOS;
const dryRun = process.env.DRY_RUN === "1";
const lote = 500;

function ler(arquivo) {
  return fs
    .readFileSync(path.join(dir, arquivo), "utf8")
    .split("\n")
    .filter((linha) => linha.trim() !== "")
    .map((linha) => JSON.parse(linha));
}

function data(valor) {
  return valor === null || valor === undefined ? null : new Date(valor);
}

function faixa(valores) {
  if (valores.length === 0) {
    return { min: null, max: null, total: 0 };
  }
  let min = valores[0];
  let max = valores[0];
  for (const v of valores) {
    if (v < min) min = v;
    if (v > max) max = v;
  }
  return { min, max, total: valores.length };
}

function lotes(lista) {
  const saida = [];
  for (let i = 0; i < lista.length; i += lote) {
    saida.push(lista.slice(i, i + lote));
  }
  return saida;
}

function ocupados(colecao, ids) {
  let total = 0;
  for (const parte of lotes(ids)) {
    total += colecao.countDocuments({ _id: { $in: parte } });
  }
  return total;
}

function inserirPorId(colecao, docs) {
  let inseridos = 0;
  for (const parte of lotes(docs)) {
    const resultado = colecao.bulkWrite(
      parte.map((doc) => ({
        updateOne: { filter: { _id: doc._id }, update: { $setOnInsert: doc }, upsert: true },
      })),
      { ordered: false }
    );
    inseridos += resultado.upsertedCount;
  }
  return inseridos;
}

function unir(anterior, atual) {
  if (!anterior || anterior.min === null) return atual;
  if (atual.min === null) return anterior;
  return { min: Math.min(anterior.min, atual.min), max: Math.max(anterior.max, atual.max), total: atual.total };
}

if (!dir || !process.env.MONGO_URI) {
  print("defina DIR_DADOS e MONGO_URI");
  process.exit(2);
}

let db;
try {
  db = connect(process.env.MONGO_URI);
} catch (erro) {
  print("falha ao conectar no MongoDB");
  process.exit(3);
}

const posts = ler("mural_posts.ndjson").map((p) => ({
  _id: p.id,
  title: p.title,
  content: p.content,
  authorUserId: p.authorUserId,
  authorName: p.authorName,
  authorEmail: p.authorEmail,
  createdAt: data(p.createdAt),
  updatedAt: data(p.updatedAt),
  audienceRoles: p.audienceRoles,
  audienceSchoolroomIds: p.audienceSchoolroomIds,
  active: p.active,
}));

const views = ler("mural_views.ndjson").map((v) => ({
  postId: v.postId,
  userId: v.userId,
  viewedAt: data(v.viewedAt),
}));

const notificacoes = ler("notificacoes_usuario.ndjson").map((n) => ({
  _id: n.id,
  userId: n.userId,
  title: n.title,
  message: n.message,
  type: n.type,
  actionUrl: n.actionUrl,
  referenceKey: n.referenceKey,
  readAt: data(n.readAt),
  createdAt: data(n.createdAt),
}));

const colPosts = db.getCollection("mural_posts");
const colViews = db.getCollection("mural_views");
const colNotificacoes = db.getCollection("notificacoes_usuario");
const colCounters = db.getCollection("counters");

const idsPosts = posts.map((p) => p._id);
const idsNotificacoes = notificacoes.map((n) => n._id);

const manifestoPath = path.join(dir, "manifesto-mongo.json");
const manifestoAnterior = fs.existsSync(manifestoPath) ? JSON.parse(fs.readFileSync(manifestoPath, "utf8")) : null;

const postsOcupados = ocupados(colPosts, idsPosts);
const notificacoesOcupadas = ocupados(colNotificacoes, idsNotificacoes);
const conflito = manifestoAnterior === null && (postsOcupados > 0 || notificacoesOcupadas > 0);

print("modo: " + (dryRun ? "dry-run (nada sera gravado)" : "carga"));
print("mural_posts: origem=" + posts.length + " ids_ja_no_destino=" + postsOcupados);
print("mural_views: origem=" + views.length);
print("notificacoes_usuario: origem=" + notificacoes.length + " ids_ja_no_destino=" + notificacoesOcupadas);
print("manifesto anterior: " + (manifestoAnterior === null ? "nao" : "sim (reexecucao; documentos existentes nao sao alterados)"));

if (conflito) {
  print("abortado: o destino ja tem documentos com ids da faixa migrada e nao ha manifesto de carga anterior");
  process.exit(4);
}

if (dryRun) {
  process.exit(0);
}

function contadorAtual(nome) {
  const doc = colCounters.findOne({ _id: nome });
  return doc ? doc.seq : null;
}

const faixaPosts = faixa(idsPosts);
const faixaNotificacoes = faixa(idsNotificacoes);

const seqAnterior = manifestoAnterior
  ? manifestoAnterior.seqAnterior
  : { mural_posts: contadorAtual("mural_posts"), notificacoes_usuario: contadorAtual("notificacoes_usuario") };

const manifesto = {
  geradoEm: new Date().toISOString(),
  mural_posts: unir(manifestoAnterior && manifestoAnterior.mural_posts, faixaPosts),
  notificacoes_usuario: unir(manifestoAnterior && manifestoAnterior.notificacoes_usuario, faixaNotificacoes),
  mural_views: { postIdMin: faixaPosts.min, postIdMax: faixaPosts.max, total: views.length },
  seqAnterior,
  seqAplicado: { mural_posts: faixaPosts.max, notificacoes_usuario: faixaNotificacoes.max },
};
fs.writeFileSync(manifestoPath, JSON.stringify(manifesto, null, 2));

try {
  colViews.createIndex({ postId: 1, userId: 1 }, { unique: true });
} catch (erro) {
  print("aviso: indice unico de mural_views ja existe com outra definicao; mantido");
}

const insPosts = inserirPorId(colPosts, posts);
const insNotificacoes = inserirPorId(colNotificacoes, notificacoes);

let insViews = 0;
for (const parte of lotes(views)) {
  const resultado = colViews.bulkWrite(
    parte.map((doc) => ({
      updateOne: {
        filter: { postId: doc.postId, userId: doc.userId },
        update: { $setOnInsert: doc },
        upsert: true,
      },
    })),
    { ordered: false }
  );
  insViews += resultado.upsertedCount;
}

if (faixaPosts.max !== null) {
  colCounters.updateOne({ _id: "mural_posts" }, { $max: { seq: faixaPosts.max } }, { upsert: true });
}
if (faixaNotificacoes.max !== null) {
  colCounters.updateOne({ _id: "notificacoes_usuario" }, { $max: { seq: faixaNotificacoes.max } }, { upsert: true });
}

print("inseridos: mural_posts=" + insPosts + " mural_views=" + insViews + " notificacoes_usuario=" + insNotificacoes);
print("counters ajustados; manifesto em " + manifestoPath);
