const assert = require("node:assert/strict");
const test = require("node:test");
const { ErroApi } = require("../dist/api/erro-api");
const { NotificacaoUsuarioService } = require("../dist/notificacoes-usuario/notificacao-usuario.service");
const { lerSolicitacao } = require("../dist/notificacoes-usuario/solicitacao");
const { EventosProcessadosMemoria, RepositorioNotificacoesMemoria } = require("./support/memoria");

function evento(sobrescrever = {}, payload = {}) {
  return {
    eventId: "6f1c2a4e-1b2c-4d5e-8f90-123456789abc",
    eventType: "notificacao.solicitada",
    version: 1,
    occurredAt: "2026-11-08T17:00:00Z",
    payload: {
      type: "WAITING_LIST_VACANCY",
      title: "Vaga disponível na lista de espera",
      message: "Há 1 vaga(s) na oficina \"Violão\" (turno manhã) e 2 candidato(s) aguardando.",
      actionUrl: "/dashboard/secretaria/espera",
      referenceKey: "WAITING_LIST:4:MORNING",
      recipientUserIds: [1, 2, 5],
      ...payload,
    },
    ...sobrescrever,
  };
}

function cenario() {
  const repositorio = new RepositorioNotificacoesMemoria();
  const eventos = new EventosProcessadosMemoria();
  return { repositorio, eventos, servico: new NotificacaoUsuarioService(repositorio, eventos) };
}

test("grava uma notificacao por destinatario com os campos do evento", async () => {
  const { servico, repositorio, eventos } = cenario();
  assert.equal(await servico.processarSolicitacao(evento()), "processada");
  assert.deepEqual(repositorio.itens.map((item) => item.userId), [1, 2, 5]);
  const primeira = repositorio.itens[0];
  assert.equal(primeira.type, "WAITING_LIST_VACANCY");
  assert.equal(primeira.title, "Vaga disponível na lista de espera");
  assert.equal(primeira.actionUrl, "/dashboard/secretaria/espera");
  assert.equal(primeira.referenceKey, "WAITING_LIST:4:MORNING");
  assert.equal(primeira.readAt, null);
  assert.ok(primeira.createdAt instanceof Date);
  assert.ok(eventos.ids.has("6f1c2a4e-1b2c-4d5e-8f90-123456789abc"));
});

test("reentrega do mesmo eventId nao duplica, mesmo depois de lidas", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento());
  await servico.marcarTodasLidas(1);
  assert.equal(await servico.processarSolicitacao(evento()), "duplicada");
  assert.equal(repositorio.itens.length, 3);
});

test("nao cria quando ja existe nao lida com o mesmo referenceKey para o usuario", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento());
  const outro = evento({ eventId: "11111111-1111-4111-8111-111111111111" }, { recipientUserIds: [1, 9] });
  assert.equal(await servico.processarSolicitacao(outro), "processada");
  assert.deepEqual(repositorio.itens.map((item) => item.userId), [1, 2, 5, 9]);
});

test("depois de lida, um novo evento com o mesmo referenceKey cria outra", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento());
  await servico.marcarTodasLidas(1);
  const outro = evento({ eventId: "22222222-2222-4222-8222-222222222222" }, { recipientUserIds: [1] });
  await servico.processarSolicitacao(outro);
  assert.equal(repositorio.itens.filter((item) => item.userId === 1).length, 2);
});

test("referenceKey diferente cria mesmo com outra nao lida", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento());
  const outro = evento(
    { eventId: "33333333-3333-4333-8333-333333333333" },
    { referenceKey: "WAITING_LIST:4:AFTERNOON", recipientUserIds: [1] },
  );
  await servico.processarSolicitacao(outro);
  assert.equal(repositorio.itens.filter((item) => item.userId === 1).length, 2);
});

test("destinatario repetido no mesmo evento gera uma so notificacao", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento({}, { recipientUserIds: [3, 3, 3] }));
  assert.equal(repositorio.itens.length, 1);
});

test("payload invalido e ignorado sem gravar", async () => {
  const { servico, repositorio, eventos } = cenario();
  const invalidos = [
    null,
    "texto",
    {},
    evento({ eventId: "nao-e-uuid" }),
    evento({ eventType: "outra.coisa" }),
    evento({ version: 2 }),
    evento({ occurredAt: "ontem" }),
    evento({ payload: null }),
    evento({}, { type: "" }),
    evento({}, { title: undefined }),
    evento({}, { message: 5 }),
    evento({}, { referenceKey: " " }),
    evento({}, { actionUrl: 7 }),
    evento({}, { recipientUserIds: "1,2" }),
    evento({}, { recipientUserIds: [1, "2"] }),
    evento({}, { recipientUserIds: undefined }),
  ];
  for (const invalido of invalidos) {
    assert.equal(await servico.processarSolicitacao(invalido), "ignorada");
  }
  assert.equal(repositorio.itens.length, 0);
  assert.equal(eventos.ids.size, 0);
});

test("aceita actionUrl nulo e occurredAt com fracao de segundo", () => {
  const lido = lerSolicitacao(evento({ occurredAt: "2026-11-08T17:00:00.123456Z" }, { actionUrl: null }));
  assert.equal(lido.actionUrl, null);
});

test("erro do banco propaga e o evento nao e marcado como processado", async () => {
  const { servico, repositorio, eventos } = cenario();
  repositorio.inserir = async () => {
    throw new Error("mongo fora");
  };
  await assert.rejects(servico.processarSolicitacao(evento()), /mongo fora/);
  assert.equal(eventos.ids.size, 0);
});

test("reprocessar depois de falha parcial nao duplica o que ja foi gravado", async () => {
  const { servico, repositorio } = cenario();
  const inserir = repositorio.inserir.bind(repositorio);
  let chamadas = 0;
  repositorio.inserir = async (dados) => {
    chamadas += 1;
    if (chamadas === 2) {
      throw new Error("mongo fora");
    }
    return inserir(dados);
  };
  await assert.rejects(servico.processarSolicitacao(evento()));
  await servico.processarSolicitacao(evento());
  assert.deepEqual(repositorio.itens.map((item) => item.userId).sort(), [1, 2, 5]);
});

test("listar devolve so as do usuario, da mais nova para a mais antiga, com datas ISO sem fuso", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento({}, { recipientUserIds: [1] }));
  await servico.processarSolicitacao(
    evento({ eventId: "44444444-4444-4444-8444-444444444444" }, { referenceKey: "OUTRA", recipientUserIds: [1, 2] }),
  );
  repositorio.itens[0].createdAt = new Date("2026-11-08T10:00:00.000Z");
  repositorio.itens[1].createdAt = new Date("2026-11-08T11:00:00.000Z");
  await servico.marcarLida(repositorio.itens[0].id, 1);
  const lista = await servico.listar(1);
  assert.deepEqual(lista.map((item) => item.createdAt), ["2026-11-08T11:00:00.000", "2026-11-08T10:00:00.000"]);
  assert.equal(lista[0].readAt, null);
  assert.match(lista[1].readAt, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}$/);
  assert.deepEqual(Object.keys(lista[0]), ["id", "title", "message", "type", "actionUrl", "readAt", "createdAt"]);
  assert.equal((await servico.listar(2)).length, 1);
});

test("marcarLida e idempotente e nao muda readAt na segunda vez", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento({}, { recipientUserIds: [1] }));
  const id = repositorio.itens[0].id;
  await servico.marcarLida(id, 1);
  const primeiraLeitura = repositorio.itens[0].readAt;
  assert.ok(primeiraLeitura instanceof Date);
  await new Promise((resolve) => setTimeout(resolve, 5));
  await servico.marcarLida(id, 1);
  assert.equal(repositorio.itens[0].readAt, primeiraLeitura);
});

test("marcarLida responde 404 para id inexistente ou de outro usuario", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento({}, { recipientUserIds: [1] }));
  for (const [id, userId] of [[999, 1], [repositorio.itens[0].id, 2]]) {
    await assert.rejects(servico.marcarLida(id, userId), (erro) => {
      assert.ok(erro instanceof ErroApi);
      assert.equal(erro.status, 404);
      assert.equal(erro.message, "Notificação não encontrada");
      return true;
    });
  }
  assert.equal(repositorio.itens[0].readAt, null);
});

test("marcarTodasLidas afeta so o usuario", async () => {
  const { servico, repositorio } = cenario();
  await servico.processarSolicitacao(evento());
  await servico.marcarTodasLidas(2);
  assert.deepEqual(
    repositorio.itens.map((item) => [item.userId, item.readAt !== null]),
    [[1, false], [2, true], [5, false]],
  );
});
