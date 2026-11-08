const { createHmac } = require("node:crypto");

function codificar(objeto) {
  return Buffer.from(JSON.stringify(objeto)).toString("base64url");
}

function assinar({ chave, alg = "HS256", claims = {}, cabecalho = null, agora = Math.floor(Date.now() / 1000) }) {
  const completo = { sub: "usuario@sing.local", role: "ROLE_ADMIN", uid: 1, exp: agora + 3600, ...claims };
  const head = codificar(cabecalho ?? { alg });
  const corpo = codificar(completo);
  const hash = { HS256: "sha256", HS384: "sha384", HS512: "sha512" }[alg] ?? "sha256";
  const assinatura = createHmac(hash, chave).update(`${head}.${corpo}`).digest("base64url");
  return `${head}.${corpo}.${assinatura}`;
}

module.exports = { assinar, codificar };
