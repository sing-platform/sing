const assert = require("node:assert/strict");
const test = require("node:test");
const { montarCorpo, ErroApi, traduzir, falhaDeValidacao } = require("../dist/api/erro-api");
const { interpretarData, interpretarId } = require("../dist/mural/mural.validacao");
const { ClienteCoreHttp, lerResposta } = require("../dist/core/core.client");
const { formatarData } = require("../dist/api/data");

test("datas saem como LocalDateTime ISO sem fuso", () => {
  assert.equal(formatarData(new Date("2026-11-08T17:00:00.123Z")), "2026-11-08T17:00:00.123");
});

test("erro de validacao segue o ApiError do core, com fields", () => {
  const erro = falhaDeValidacao([{ field: "title", rejectedValue: "", message: "Título é obrigatório" }]);
  const corpo = montarCorpo(erro, "/api/v1/mural", new Date("2026-11-08T17:00:00.000Z"));
  assert.deepEqual(corpo, {
    status: 400,
    error: "Validation Failed",
    message: "One or more fields are invalid",
    timestamp: "2026-11-08T17:00:00.000Z",
    path: "/api/v1/mural",
    details: null,
    fields: [{ field: "title", rejectedValue: "", message: "Título é obrigatório" }],
  });
});

test("erro de negocio nao traz fields e usa a frase do status", () => {
  const corpo = montarCorpo(new ErroApi(404, "Publicação não encontrada"), "/api/v1/mural/9");
  assert.equal(corpo.error, "Not Found");
  assert.equal("fields" in corpo, false);
  assert.equal(montarCorpo(new ErroApi(503, "x"), "/").error, "Service Unavailable");
});

test("excecao desconhecida vira 500 generico sem vazar detalhe", () => {
  const original = console.error;
  console.error = () => undefined;
  try {
    const erro = traduzir(new Error("segredo interno"));
    assert.equal(erro.status, 500);
    assert.equal(erro.message, "An unexpected error occurred. Please try again later.");
  } finally {
    console.error = original;
  }
});

test("ids e datas de query sao validados", () => {
  assert.equal(interpretarId("42", "x"), 42);
  for (const ruim of ["abc", "-1", "1.5", "", "12345678901234567890"]) {
    assert.throws(() => interpretarId(ruim, "invalido"), (erro) => erro.status === 400 && erro.message === "invalido");
  }
  assert.equal(interpretarData("2026-11-08", "start").toISOString(), "2026-11-08T00:00:00.000Z");
  for (const ruim of [undefined, "", "08/11/2026", "2026-13-01", "2026-02-30", ["2026-11-08"]]) {
    assert.throws(() => interpretarData(ruim, "start"), (erro) => erro.status === 400);
  }
});

function respostaFalsa(status, corpo) {
  return {
    status,
    ok: status >= 200 && status < 300,
    json: async () => {
      if (corpo === undefined) {
        throw new Error("sem corpo");
      }
      return corpo;
    },
  };
}

async function comFetch(implementacao, acao) {
  const original = global.fetch;
  global.fetch = implementacao;
  try {
    return await acao();
  } finally {
    global.fetch = original;
  }
}

const PUBLICO = { audienceRoles: ["ROLE_STUDENT"], audienceSchoolroomIds: [3] };
const CORPO_CORE = {
  author: { userId: 1, name: "Ana", email: "a@sing.local", role: "ROLE_ADMIN" },
  recipients: [{ userId: 7, email: "aluno@sing.local" }],
};

test("cliente do core repassa Authorization, usa a URL do contrato e le a resposta", async () => {
  let chamada;
  process.env.CORE_BASE_URL = "http://core:8080/";
  const resultado = await comFetch(
    async (url, opcoes) => {
      chamada = { url, opcoes };
      return respostaFalsa(200, CORPO_CORE);
    },
    () => new ClienteCoreHttp().resolverDestinatarios("Bearer abc", PUBLICO),
  );
  delete process.env.CORE_BASE_URL;
  assert.equal(chamada.url, "http://core:8080/api/v1/internal/mural/destinatarios");
  assert.equal(chamada.opcoes.method, "POST");
  assert.equal(chamada.opcoes.headers.Authorization, "Bearer abc");
  assert.deepEqual(JSON.parse(chamada.opcoes.body), PUBLICO);
  assert.ok(chamada.opcoes.signal);
  assert.deepEqual(resultado, CORPO_CORE);
});

test("CORE_BASE_URL ausente usa localhost:8080", async () => {
  delete process.env.CORE_BASE_URL;
  let url;
  await comFetch(
    async (alvo) => {
      url = alvo;
      return respostaFalsa(200, CORPO_CORE);
    },
    () => new ClienteCoreHttp().resolverDestinatarios("Bearer abc", PUBLICO),
  );
  assert.equal(url, "http://localhost:8080/api/v1/internal/mural/destinatarios");
});

test("400 e 403 do core sao propagados com a mesma mensagem", async () => {
  for (const [status, mensagem] of [
    [400, "Professor deve informar ao menos uma turma em audienceSchoolroomIds."],
    [403, "Turma 3 não pertence ao professor autenticado."],
  ]) {
    await assert.rejects(
      comFetch(
        async () => respostaFalsa(status, { status, message: mensagem }),
        () => new ClienteCoreHttp().resolverDestinatarios("Bearer abc", PUBLICO),
      ),
      (erro) => erro.status === status && erro.message === mensagem,
    );
  }
});

test("core indisponivel, timeout ou 5xx viram 503 com mensagem clara", async () => {
  const falhas = [
    async () => {
      throw new Error("ECONNREFUSED");
    },
    async () => {
      const erro = new Error("timeout");
      erro.name = "TimeoutError";
      throw erro;
    },
    async () => respostaFalsa(500, { message: "boom" }),
    async () => respostaFalsa(502),
    async () => respostaFalsa(200, { inesperado: true }),
  ];
  for (const falha of falhas) {
    await assert.rejects(
      comFetch(falha, () => new ClienteCoreHttp().resolverDestinatarios("Bearer abc", PUBLICO)),
      (erro) => erro.status === 503 && /indisponível/.test(erro.message),
    );
  }
});

test("resposta do core fora do contrato e recusada", () => {
  assert.throws(() => lerResposta({ author: {}, recipients: [] }));
  assert.throws(() => lerResposta({ author: CORPO_CORE.author, recipients: [{ userId: "7" }] }));
});
