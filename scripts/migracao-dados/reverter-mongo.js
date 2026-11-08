const fs = require("fs");
const path = require("path");

const dir = process.env.DIR_DADOS;
const dryRun = process.env.DRY_RUN === "1";

if (!dir || !process.env.MONGO_URI) {
  print("defina DIR_DADOS e MONGO_URI");
  process.exit(2);
}

const manifestoPath = path.join(dir, "manifesto-mongo.json");
if (!fs.existsSync(manifestoPath)) {
  print("manifesto-mongo.json nao encontrado; nada a reverter");
  process.exit(2);
}
const manifesto = JSON.parse(fs.readFileSync(manifestoPath, "utf8"));

let db;
try {
  db = connect(process.env.MONGO_URI);
} catch (erro) {
  print("falha ao conectar no MongoDB");
  process.exit(3);
}

const faixaPosts = manifesto.mural_posts;
const faixaNotificacoes = manifesto.notificacoes_usuario;

const filtroPosts = faixaPosts.min === null ? null : { _id: { $gte: faixaPosts.min, $lte: faixaPosts.max } };
const filtroViews = faixaPosts.min === null ? null : { postId: { $gte: faixaPosts.min, $lte: faixaPosts.max } };
const filtroNotificacoes =
  faixaNotificacoes.min === null ? null : { _id: { $gte: faixaNotificacoes.min, $lte: faixaNotificacoes.max } };

function remover(nome, filtro) {
  if (filtro === null) {
    print(nome + ": faixa vazia");
    return;
  }
  const colecao = db.getCollection(nome);
  const alvo = colecao.countDocuments(filtro);
  if (dryRun) {
    print(nome + ": removeria " + alvo);
    return;
  }
  const resultado = colecao.deleteMany(filtro);
  print(nome + ": removidos " + resultado.deletedCount);
}

print("modo: " + (dryRun ? "dry-run (nada sera apagado)" : "rollback"));
remover("mural_views", filtroViews);
remover("mural_posts", filtroPosts);
remover("notificacoes_usuario", filtroNotificacoes);

const contadores = db.getCollection("counters");
for (const nome of ["mural_posts", "notificacoes_usuario"]) {
  const aplicado = manifesto.seqAplicado[nome];
  const anterior = manifesto.seqAnterior[nome];
  if (aplicado === null || aplicado === undefined) {
    continue;
  }
  const atual = contadores.findOne({ _id: nome });
  if (!atual || atual.seq !== aplicado) {
    print("counters." + nome + ": mantido (mudou depois da carga)");
    continue;
  }
  if (dryRun) {
    print("counters." + nome + ": voltaria para " + anterior);
    continue;
  }
  if (anterior === null || anterior === undefined) {
    contadores.deleteOne({ _id: nome });
    print("counters." + nome + ": removido (nao existia antes da carga)");
  } else {
    contadores.updateOne({ _id: nome }, { $set: { seq: anterior } });
    print("counters." + nome + ": voltou para " + anterior);
  }
}

if (!dryRun) {
  fs.renameSync(manifestoPath, manifestoPath + ".revertido-" + new Date().toISOString().replace(/[:.]/g, "-"));
}
