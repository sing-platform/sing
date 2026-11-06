const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const { aplicarEvento } = require("../dist/evento");

const EVENTO = {
  eventId: "11111111-1111-4111-8111-111111111111",
  eventType: "matricula.criada",
  version: 1,
  occurredAt: "2026-11-05T18:30:00Z",
  payload: {
    matriculaId: 9001,
    data: "2026-11-05",
    aluno: { id: 501, nome: "Aluno Exemplo Fictício" },
    turma: { id: 77, nome: "Turma Exemplo Manhã" },
    oficina: { id: 12, nome: "Oficina Exemplo de Teatro" },
  },
};

function memoria() {
  const docs = [];
  return {
    docs,
    async buscar(eventId) {
      return docs.find((doc) => doc.eventId === eventId) ?? null;
    },
    async inserir(doc) {
      if (docs.some((item) => item.eventId === doc.eventId)) {
        const erro = new Error("duplicado");
        erro.code = 11000;
        throw erro;
      }
      docs.push(doc);
    },
  };
}

test("grava uma notificacao e a reentrega nao cria outra", async () => {
  const banco = memoria();
  assert.equal(await aplicarEvento(EVENTO, banco), "gravada");
  assert.equal(await aplicarEvento(EVENTO, banco), "duplicada");
  assert.equal(banco.docs.length, 1);
  assert.equal(banco.docs[0].matriculaId, 9001);
  assert.match(banco.docs[0].mensagem, /Aluno Exemplo Fictício/);
  assert.equal(banco.docs[0].email, undefined);
});

test("payload incompleto nao grava", async () => {
  const banco = memoria();
  const ruim = { ...EVENTO, payload: { matriculaId: 9001 } };
  assert.equal(await aplicarEvento(ruim, banco), "ignorada");
  assert.equal(banco.docs.length, 0);
});

test("outro eventId gera outra notificacao", async () => {
  const banco = memoria();
  const segundo = {
    ...EVENTO,
    eventId: "22222222-2222-4222-8222-222222222222",
    payload: { ...EVENTO.payload, matriculaId: 9002 },
  };
  assert.equal(await aplicarEvento(EVENTO, banco), "gravada");
  assert.equal(await aplicarEvento(segundo, banco), "gravada");
  assert.equal(banco.docs.length, 2);
});

test("este recorte nao manda e-mail", () => {
  const src = path.join(__dirname, "..", "src");
  for (const nome of ["evento.ts", "consumidor.service.ts", "notificacao.service.ts"]) {
    const texto = readFileSync(path.join(src, nome), "utf8");
    assert.equal(texto.includes("nodemailer"), false);
    assert.equal(texto.includes("smtp"), false);
    assert.equal(texto.includes("whatsapp"), false);
  }
});
