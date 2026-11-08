const fs = require("fs");
const path = require("path");

const dir = process.env.DIR_DADOS;

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

function ler(arquivo) {
  return fs
    .readFileSync(path.join(dir, arquivo), "utf8")
    .split("\n")
    .filter((linha) => linha.trim() !== "")
    .map((linha) => JSON.parse(linha));
}

function linha(chave, valor) {
  print(chave + "|" + valor);
}

function amostra(lista) {
  if (lista.length <= 22) {
    return lista;
  }
  const passo = Math.ceil(lista.length / 20);
  const saida = [];
  for (let i = 0; i < lista.length; i += passo) {
    saida.push(lista[i]);
  }
  saida.push(lista[lista.length - 1]);
  return saida;
}

function iso(valor) {
  return valor === null || valor === undefined ? null : new Date(valor).toISOString();
}

const manifesto = JSON.parse(fs.readFileSync(path.join(dir, "manifesto-mongo.json"), "utf8"));
const faixaPosts = manifesto.mural_posts;
const faixaNotificacoes = manifesto.notificacoes_usuario;
const marcaPosts = { _id: { $gte: faixaPosts.min, $lte: faixaPosts.max } };
const marcaViews = { postId: { $gte: faixaPosts.min, $lte: faixaPosts.max } };
const marcaNotificacoes = { _id: { $gte: faixaNotificacoes.min, $lte: faixaNotificacoes.max } };
const posts = db.getCollection("mural_posts");
const views = db.getCollection("mural_views");
const notificacoes = db.getCollection("notificacoes_usuario");
const counters = db.getCollection("counters");

linha("mural_posts.total", posts.countDocuments(marcaPosts));
linha("mural_posts.ativos", posts.countDocuments({ ...marcaPosts, active: true }));
const maxPost = posts.find(marcaPosts).sort({ _id: -1 }).limit(1).toArray();
const maxPostId = maxPost.length > 0 ? maxPost[0]._id : 0;
linha("mural_posts.max_id", maxPostId);

linha("mural_views.total", views.countDocuments(marcaViews));
linha("mural_views.lidas", views.countDocuments({ ...marcaViews, viewedAt: { $ne: null } }));

const porPost = {};
const agregado = views
  .aggregate([
    { $match: marcaViews },
    { $group: { _id: "$postId", views: { $sum: 1 }, lidas: { $sum: { $cond: [{ $ne: ["$viewedAt", null] }, 1, 0] } } } },
  ])
  .toArray();
for (const g of agregado) {
  porPost[g._id] = g;
}
const docsPosts = posts.find(marcaPosts, { projection: { _id: 1 } }).toArray();
for (const p of docsPosts) {
  const g = porPost[p._id];
  linha("mural_post." + p._id + ".views", g ? g.views : 0);
  linha("mural_post." + p._id + ".lidas", g ? g.lidas : 0);
}

linha("notificacoes_usuario.total", notificacoes.countDocuments(marcaNotificacoes));
linha("notificacoes_usuario.lidas", notificacoes.countDocuments({ ...marcaNotificacoes, readAt: { $ne: null } }));
const maxNotificacao = notificacoes.find(marcaNotificacoes).sort({ _id: -1 }).limit(1).toArray();
const maxNotificacaoId = maxNotificacao.length > 0 ? maxNotificacao[0]._id : 0;
linha("notificacoes_usuario.max_id", maxNotificacaoId);
const porTipo = notificacoes.aggregate([{ $match: marcaNotificacoes }, { $group: { _id: "$type", n: { $sum: 1 } } }]).toArray();
for (const g of porTipo) {
  linha("notificacoes_usuario.tipo." + g._id, g.n);
}

const contadorPosts = counters.findOne({ _id: "mural_posts" });
const contadorNotificacoes = counters.findOne({ _id: "notificacoes_usuario" });
linha("counters.mural_posts_cobre_max", maxPostId === 0 || (contadorPosts && contadorPosts.seq >= maxPostId) ? 1 : 0);
linha(
  "counters.notificacoes_usuario_cobre_max",
  maxNotificacaoId === 0 || (contadorNotificacoes && contadorNotificacoes.seq >= maxNotificacaoId) ? 1 : 0
);

let divergentes = 0;
for (const origem of amostra(ler("mural_posts.ndjson"))) {
  const destino = posts.findOne({ _id: origem.id });
  const igual =
    destino &&
    destino.title === origem.title &&
    destino.content === origem.content &&
    destino.authorUserId === origem.authorUserId &&
    destino.authorName === origem.authorName &&
    destino.authorEmail === origem.authorEmail &&
    destino.active === origem.active &&
    iso(destino.createdAt) === iso(origem.createdAt) &&
    JSON.stringify(destino.audienceRoles) === JSON.stringify(origem.audienceRoles) &&
    JSON.stringify(destino.audienceSchoolroomIds) === JSON.stringify(origem.audienceSchoolroomIds);
  if (!igual) {
    divergentes++;
    print("amostra_divergente.mural_posts|" + origem.id);
  }
}
for (const origem of amostra(ler("notificacoes_usuario.ndjson"))) {
  const destino = notificacoes.findOne({ _id: origem.id });
  const igual =
    destino &&
    destino.userId === origem.userId &&
    destino.title === origem.title &&
    destino.message === origem.message &&
    destino.type === origem.type &&
    destino.referenceKey === origem.referenceKey &&
    iso(destino.readAt) === iso(origem.readAt) &&
    iso(destino.createdAt) === iso(origem.createdAt);
  if (!igual) {
    divergentes++;
    print("amostra_divergente.notificacoes_usuario|" + origem.id);
  }
}
linha("amostra.divergentes", divergentes);
