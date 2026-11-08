const assert = require("node:assert/strict");
const test = require("node:test");
const { derivarChave, verificarToken, TokenInvalido } = require("../dist/auth/jwt");
const { assinar, codificar } = require("./support/token");

const SEGREDO_TEXTO = "segredo-de-teste-com-hifen-para-nao-ser-base64";
const CHAVE = derivarChave(SEGREDO_TEXTO);

function recusa(token, chave = CHAVE, agora) {
  assert.throws(() => verificarToken(token, chave, agora), TokenInvalido);
}

test("aceita HS256, HS384 e HS512 e extrai os claims", () => {
  for (const alg of ["HS256", "HS384", "HS512"]) {
    const token = assinar({ chave: CHAVE, alg, claims: { sub: "ana@sing.local", role: "ROLE_PROFESSOR", uid: 7, pid: 9 } });
    assert.deepEqual(verificarToken(token, CHAVE), {
      email: "ana@sing.local",
      role: "ROLE_PROFESSOR",
      userId: 7,
      personId: 9,
    });
  }
});

test("pid e role sao opcionais", () => {
  const token = assinar({ chave: CHAVE, claims: { role: undefined, pid: undefined } });
  const credenciais = verificarToken(token, CHAVE);
  assert.equal(credenciais.personId, null);
  assert.equal(credenciais.role, null);
});

test("recusa assinatura errada", () => {
  recusa(assinar({ chave: derivarChave("outro-segredo-completamente-diferente") }));
});

test("recusa payload adulterado", () => {
  const token = assinar({ chave: CHAVE, claims: { role: "ROLE_STUDENT" } });
  const [cabecalho, , assinatura] = token.split(".");
  const adulterado = codificar({ sub: "x@sing.local", role: "ROLE_ADMIN", uid: 1, exp: 9999999999 });
  recusa(`${cabecalho}.${adulterado}.${assinatura}`);
});

test("recusa alg none, com e sem assinatura", () => {
  const corpo = codificar({ sub: "x@sing.local", role: "ROLE_ADMIN", uid: 1, exp: 9999999999 });
  recusa(`${codificar({ alg: "none" })}.${corpo}.`);
  recusa(`${codificar({ alg: "none" })}.${corpo}.abc`);
  recusa(`${codificar({ alg: "NONE" })}.${corpo}.abc`);
});

test("recusa outros algoritmos", () => {
  recusa(assinar({ chave: CHAVE, cabecalho: { alg: "RS256" } }));
  recusa(assinar({ chave: CHAVE, cabecalho: { alg: "HS1" } }));
  recusa(assinar({ chave: CHAVE, cabecalho: {} }));
});

test("recusa token expirado e token sem exp", () => {
  const agora = Math.floor(Date.now() / 1000);
  recusa(assinar({ chave: CHAVE, claims: { exp: agora - 1 } }));
  recusa(assinar({ chave: CHAVE, claims: { exp: agora } }));
  recusa(assinar({ chave: CHAVE, claims: { exp: undefined } }));
});

test("recusa nbf no futuro", () => {
  const agora = Math.floor(Date.now() / 1000);
  recusa(assinar({ chave: CHAVE, claims: { nbf: agora + 600 } }));
});

test("recusa formato quebrado", () => {
  recusa("");
  recusa("a.b");
  recusa("a.b.c.d");
  recusa("..");
  recusa("nao-e-jwt.nem-json.assinatura");
});

test("segredo vazio ou ausente recusa qualquer token", () => {
  assert.equal(derivarChave(""), null);
  assert.equal(derivarChave(undefined), null);
  const tokenComChaveVazia = assinar({ chave: Buffer.alloc(0) });
  recusa(tokenComChaveVazia, derivarChave(""));
  recusa(tokenComChaveVazia, derivarChave(undefined));
  recusa(assinar({ chave: CHAVE }), null);
  recusa(assinar({ chave: CHAVE }), Buffer.alloc(0));
});

test("segredo Base64 valido vira bytes decodificados", () => {
  const bytes = Buffer.from("uma-chave-binaria-bem-comprida-de-32-bytes!!");
  const segredo = bytes.toString("base64");
  const chave = derivarChave(segredo);
  assert.deepEqual(chave, bytes);
  verificarToken(assinar({ chave: bytes }), chave);
  recusa(assinar({ chave: Buffer.from(segredo, "utf8") }), chave);
});

test("segredo Base64 sem preenchimento tambem e decodificado", () => {
  const bytes = Buffer.from("abcdefghij");
  const semPreenchimento = bytes.toString("base64").replace(/=+$/, "");
  assert.deepEqual(derivarChave(semPreenchimento), bytes);
});

test("segredo que nao e Base64 valido usa os bytes UTF-8", () => {
  for (const segredo of [SEGREDO_TEXTO, "com espaco", "abcde", "ab=cd", "abc=def=", "chave_com_underline_e_hifen-"]) {
    assert.deepEqual(derivarChave(segredo), Buffer.from(segredo, "utf8"), segredo);
  }
  verificarToken(assinar({ chave: Buffer.from(SEGREDO_TEXTO, "utf8") }), CHAVE);
});
