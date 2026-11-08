const assert = require("node:assert/strict");
const test = require("node:test");
const { ErroApi } = require("../dist/api/erro-api");
const { EmailService } = require("../dist/email/email.service");
const { MuralService } = require("../dist/mural/mural.service");
const { validarPublicacao } = require("../dist/mural/mural.validacao");
const { ClienteCoreFalso, OutboxFalsa, PublicadorFalso, RepositorioMuralMemoria } = require("./support/memoria");

function cenario() {
  const repositorio = new RepositorioMuralMemoria();
  const core = new ClienteCoreFalso();
  const publicador = new PublicadorFalso();
  const outbox = new OutboxFalsa();
  const email = new EmailService(publicador, outbox);
  const servico = new MuralService(repositorio, core, email);
  core.resposta = {
    author: { userId: 1, name: "Ana Souza", email: "admin@sing.local", role: "ROLE_ADMIN" },
    recipients: [
      { userId: 7, email: "aluno7@sing.local" },
      { userId: 8, email: "aluno8@sing.local" },
    ],
  };
  return { repositorio, core, publicador, outbox, servico };
}

function usuario(role, userId, authorization = "Bearer token-do-usuario") {
  return { email: `u${userId}@sing.local`, role, userId, personId: null, authorization };
}

const ADMIN = usuario("ROLE_ADMIN", 1);
const SECRETARIA = usuario("ROLE_SECRETARY", 2);
const PROFESSOR = usuario("ROLE_PROFESSOR", 3);
const ALUNO7 = usuario("ROLE_STUDENT", 7);
const ALUNO8 = usuario("ROLE_STUDENT", 8);
const ALUNO9 = usuario("ROLE_STUDENT", 9);

const PUBLICACAO = {
  title: "Reunião",
  content: "Linha 1\nLinha 2",
  audienceRoles: ["ROLE_STUDENT"],
  audienceSchoolroomIds: [],
};

async function erroDe(promessa) {
  try {
    await promessa;
  } catch (erro) {
    assert.ok(erro instanceof ErroApi, `esperava ErroApi, veio ${erro}`);
    return erro;
  }
  assert.fail("deveria ter falhado");
}

test("criar devolve viewed=false, snapshot do autor e id sequencial", async () => {
  const { servico, repositorio, core } = cenario();
  const primeiro = await servico.criar(PUBLICACAO, ADMIN);
  const segundo = await servico.criar(PUBLICACAO, ADMIN);
  assert.equal(primeiro.id, 1);
  assert.equal(segundo.id, 2);
  assert.equal(primeiro.viewed, false);
  assert.equal(primeiro.authorName, "Ana Souza");
  assert.equal(primeiro.updatedAt, null);
  assert.match(primeiro.createdAt, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}$/);
  assert.deepEqual(primeiro.audienceRoles, ["ROLE_STUDENT"]);
  assert.equal(repositorio.posts[0].authorEmail, "admin@sing.local");
  assert.equal(repositorio.posts[0].authorUserId, 1);
  assert.deepEqual(core.chamadas[0], {
    authorization: "Bearer token-do-usuario",
    publico: PUBLICACAO,
  });
  assert.deepEqual(
    repositorio.views.map((view) => [view.postId, view.userId, view.viewedAt]),
    [
      [1, 7, null],
      [1, 8, null],
      [2, 7, null],
      [2, 8, null],
    ],
  );
});

test("erro do core na criacao nao grava nada nem manda e-mail", async () => {
  const { servico, repositorio, core, publicador } = cenario();
  core.erro = new ErroApi(403, "Turma 3 não pertence ao professor autenticado.");
  const erro = await erroDe(servico.criar(PUBLICACAO, PROFESSOR));
  assert.equal(erro.status, 403);
  assert.equal(erro.message, "Turma 3 não pertence ao professor autenticado.");
  assert.equal(repositorio.posts.length, 0);
  assert.equal(publicador.enviadas.length, 0);
});

test("gestor lista todos os ativos com viewed=true, do mais novo ao mais antigo", async () => {
  const { servico, repositorio } = cenario();
  await servico.criar({ ...PUBLICACAO, title: "Antigo" }, ADMIN);
  await servico.criar({ ...PUBLICACAO, title: "Novo" }, ADMIN);
  repositorio.posts[0].createdAt = new Date("2026-01-01T10:00:00.000Z");
  repositorio.posts[1].createdAt = new Date("2026-02-01T10:00:00.000Z");
  await servico.remover(2, ADMIN);
  await servico.criar({ ...PUBLICACAO, title: "Terceiro" }, ADMIN);
  const lista = await servico.listar(SECRETARIA);
  assert.deepEqual(lista.map((post) => post.title), ["Terceiro", "Antigo"]);
  assert.ok(lista.every((post) => post.viewed === true));
});

test("usuario comum lista so os posts em que e destinatario, com viewed", async () => {
  const { servico } = cenario();
  await servico.criar({ ...PUBLICACAO, title: "A" }, ADMIN);
  await servico.criar({ ...PUBLICACAO, title: "B" }, ADMIN);
  await servico.marcarTodasLidas(ALUNO7);
  await servico.criar({ ...PUBLICACAO, title: "C" }, ADMIN);
  const lista = await servico.listar(ALUNO7);
  assert.deepEqual(
    lista.map((post) => [post.title, post.viewed]).sort(),
    [["A", true], ["B", true], ["C", false]],
  );
  assert.deepEqual(await servico.listar(ALUNO9), []);
});

test("posts removidos somem da lista do destinatario", async () => {
  const { servico } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.remover(1, ADMIN);
  assert.deepEqual(await servico.listar(ALUNO7), []);
});

test("buscar: gestor ve qualquer ativo, destinatario ve o seu, outro leva 403", async () => {
  const { servico } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  assert.equal((await servico.buscar(1, ADMIN)).viewed, true);
  assert.equal((await servico.buscar(1, ALUNO7)).viewed, false);
  await servico.marcarTodasLidas(ALUNO7);
  assert.equal((await servico.buscar(1, ALUNO7)).viewed, true);
  const erro = await erroDe(servico.buscar(1, ALUNO9));
  assert.equal(erro.status, 403);
  assert.equal(erro.message, "Acesso não autorizado a esta publicação");
});

test("buscar: inexistente e inativo dao 404", async () => {
  const { servico } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.remover(1, ADMIN);
  for (const id of [1, 99]) {
    for (const quem of [ADMIN, ALUNO7]) {
      const erro = await erroDe(servico.buscar(id, quem));
      assert.equal(erro.status, 404);
      assert.equal(erro.message, "Publicação não encontrada");
    }
  }
});

test("unread-count conta views nao lidas e e 0 para gestor", async () => {
  const { servico } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.criar(PUBLICACAO, ADMIN);
  assert.equal(await servico.contarNaoLidas(ALUNO7), 2);
  assert.equal(await servico.contarNaoLidas(ALUNO9), 0);
  assert.equal(await servico.contarNaoLidas(ADMIN), 0);
  assert.equal(await servico.contarNaoLidas(SECRETARIA), 0);
});

test("unread-count nao filtra post inativo (comportamento preservado)", async () => {
  const { servico } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.remover(1, ADMIN);
  assert.equal(await servico.contarNaoLidas(ALUNO7), 2);
  assert.equal((await servico.listar(ALUNO7)).length, 1);
});

test("mark-all-read marca as views do usuario e e no-op para gestor", async () => {
  const { servico, repositorio } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.marcarTodasLidas(ADMIN);
  await servico.marcarTodasLidas(SECRETARIA);
  assert.equal(await servico.contarNaoLidas(ALUNO7), 1);
  await servico.marcarTodasLidas(ALUNO7);
  assert.equal(await servico.contarNaoLidas(ALUNO7), 0);
  assert.equal(await servico.contarNaoLidas(ALUNO8), 1);
  assert.ok(repositorio.views.find((view) => view.userId === 7).viewedAt instanceof Date);
});

test("professor so edita e remove o proprio post; gestor edita qualquer um", async () => {
  const { servico, core } = cenario();
  core.resposta = {
    author: { userId: 3, name: "Prof Carlos", email: "prof@sing.local", role: "ROLE_PROFESSOR" },
    recipients: [{ userId: 7, email: "aluno7@sing.local" }],
  };
  await servico.criar({ title: "Do professor", content: "x", audienceRoles: [], audienceSchoolroomIds: [4] }, PROFESSOR);
  const outroProfessor = usuario("ROLE_PROFESSOR", 30);
  for (const acao of [
    () => servico.atualizar(1, PUBLICACAO, outroProfessor),
    () => servico.remover(1, outroProfessor),
  ]) {
    const erro = await erroDe(acao());
    assert.equal(erro.status, 403);
    assert.equal(erro.message, "Apenas o autor pode modificar esta publicação");
  }
  assert.equal((await servico.atualizar(1, PUBLICACAO, PROFESSOR)).title, "Reunião");
  assert.equal((await servico.atualizar(1, { ...PUBLICACAO, title: "Pela secretaria" }, SECRETARIA)).title, "Pela secretaria");
  await servico.remover(1, ADMIN);
});

test("atualizar checa 404 e 403 antes de consultar o core", async () => {
  const { servico, core } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  const chamadasAntes = core.chamadas.length;
  assert.equal((await erroDe(servico.atualizar(99, PUBLICACAO, ADMIN))).status, 404);
  assert.equal((await erroDe(servico.atualizar(1, PUBLICACAO, usuario("ROLE_PROFESSOR", 30)))).status, 403);
  assert.equal(core.chamadas.length, chamadasAntes);
});

test("atualizar substitui campos, devolve viewed=false e preserva o autor original", async () => {
  const { servico, repositorio, core } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  core.resposta = { ...core.resposta, author: { userId: 2, name: "Outra Pessoa", email: "outra@sing.local", role: "ROLE_SECRETARY" } };
  const resposta = await servico.atualizar(
    1,
    { title: "Novo título", content: "Novo", audienceRoles: ["ROLE_PARENT"], audienceSchoolroomIds: [5] },
    SECRETARIA,
  );
  assert.equal(resposta.viewed, false);
  assert.equal(resposta.title, "Novo título");
  assert.deepEqual(resposta.audienceRoles, ["ROLE_PARENT"]);
  assert.deepEqual(resposta.audienceSchoolroomIds, [5]);
  assert.equal(resposta.authorName, "Ana Souza");
  assert.match(resposta.updatedAt, /^\d{4}-\d{2}-\d{2}T/);
  assert.equal(repositorio.posts[0].authorEmail, "admin@sing.local");
  assert.equal(repositorio.posts[0].authorUserId, 1);
});

test("atualizar cria view e e-mail so para destinatarios novos e nao remove views", async () => {
  const { servico, repositorio, publicador, core } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.marcarTodasLidas(ALUNO7);
  publicador.enviadas.length = 0;
  const lidoEm = repositorio.views.find((view) => view.userId === 7).viewedAt;
  core.resposta = {
    author: { userId: 1, name: "Ana Souza", email: "admin@sing.local", role: "ROLE_ADMIN" },
    recipients: [{ userId: 7, email: "aluno7@sing.local" }, { userId: 9, email: "aluno9@sing.local" }],
  };
  await servico.atualizar(1, PUBLICACAO, ADMIN);
  assert.deepEqual(publicador.enviadas.map((mensagem) => mensagem.to), ["aluno9@sing.local"]);
  assert.deepEqual(repositorio.views.map((view) => view.userId).sort(), [7, 8, 9]);
  assert.equal(repositorio.views.find((view) => view.userId === 7).viewedAt, lidoEm);
  assert.equal(repositorio.views.find((view) => view.userId === 8).viewedAt, null);
});

test("remover e soft delete: post fica inativo e some das consultas", async () => {
  const { servico, repositorio } = cenario();
  await servico.criar(PUBLICACAO, ADMIN);
  await servico.remover(1, ADMIN);
  assert.equal(repositorio.posts.length, 1);
  assert.equal(repositorio.posts[0].active, false);
  assert.equal((await erroDe(servico.remover(1, ADMIN))).status, 404);
  assert.equal((await erroDe(servico.atualizar(1, PUBLICACAO, ADMIN))).status, 404);
});

test("engajamento: so ativos no periodo, ordem decrescente, contagens e autor", async () => {
  const { servico, repositorio } = cenario();
  for (const titulo of ["Fora antes", "Dentro 1", "Dentro 2", "Inativo", "Fora depois"]) {
    await servico.criar({ ...PUBLICACAO, title: titulo }, ADMIN);
  }
  const datas = [
    "2026-10-31T23:59:59.999Z",
    "2026-11-01T00:00:00.000Z",
    "2026-11-30T23:59:59.999Z",
    "2026-11-15T12:00:00.000Z",
    "2026-12-01T00:00:00.000Z",
  ];
  repositorio.posts.forEach((post, indice) => {
    post.createdAt = new Date(datas[indice]);
  });
  await servico.remover(4, ADMIN);
  await servico.marcarTodasLidas(ALUNO7);
  const linhas = await servico.engajamento(new Date("2026-11-01T00:00:00.000Z"), new Date("2026-12-01T00:00:00.000Z"));
  assert.deepEqual(linhas, [
    { postId: 3, title: "Dentro 2", createdAt: "2026-11-30T23:59:59.999", authorUsername: "admin@sing.local", viewCount: 2, readCount: 1 },
    { postId: 2, title: "Dentro 1", createdAt: "2026-11-01T00:00:00.000", authorUsername: "admin@sing.local", viewCount: 2, readCount: 1 },
  ]);
});

test("e-mail e publicado depois de gravar post e views", async () => {
  const { servico, repositorio, publicador } = cenario();
  const vistos = [];
  publicador.aoPublicar = () => vistos.push([repositorio.posts.length, repositorio.views.length]);
  await servico.criar(PUBLICACAO, ADMIN);
  assert.deepEqual(vistos, [[1, 2], [1, 2]]);
});

test("validacao: titulo e conteudo obrigatorios no formato de erro do core", () => {
  for (const corpo of [{}, { title: "  ", content: "x" }, { title: "t", content: "" }, { title: 5, content: null }]) {
    assert.throws(() => validarPublicacao(corpo), (erro) => erro.status === 400 && erro.campos.length > 0);
  }
  try {
    validarPublicacao({ title: "", content: " " });
  } catch (erro) {
    assert.equal(erro.message, "One or more fields are invalid");
    assert.deepEqual(erro.campos, [
      { field: "title", rejectedValue: "", message: "Título é obrigatório" },
      { field: "content", rejectedValue: " ", message: "Conteúdo é obrigatório" },
    ]);
  }
});

test("validacao: titulo com mais de 255 caracteres", () => {
  assert.equal(validarPublicacao({ title: "a".repeat(255), content: "x" }).title.length, 255);
  try {
    validarPublicacao({ title: "a".repeat(256), content: "x" });
    assert.fail();
  } catch (erro) {
    assert.deepEqual(erro.campos, [
      { field: "title", rejectedValue: "a".repeat(256), message: "Título deve ter no máximo 255 caracteres" },
    ]);
  }
});

test("validacao: normaliza titulo, deduplica publico e rejeita tipos errados", () => {
  const corpo = validarPublicacao({
    title: "  Aviso  ",
    content: " conteúdo ",
    audienceRoles: ["ROLE_STUDENT", "ROLE_STUDENT"],
    audienceSchoolroomIds: [1, 1, 2],
  });
  assert.deepEqual(corpo, {
    title: "Aviso",
    content: " conteúdo ",
    audienceRoles: ["ROLE_STUDENT"],
    audienceSchoolroomIds: [1, 2],
  });
  assert.deepEqual(validarPublicacao({ title: "t", content: "c" }).audienceRoles, []);
  for (const ruim of [
    { title: "t", content: "c", audienceRoles: "ROLE_STUDENT" },
    { title: "t", content: "c", audienceRoles: [1] },
    { title: "t", content: "c", audienceSchoolroomIds: ["1"] },
    { title: "t", content: "c", audienceSchoolroomIds: [1.5] },
    [],
    null,
  ]) {
    assert.throws(() => validarPublicacao(ruim), (erro) => erro.status === 400 && erro.message === "Malformed JSON request");
  }
});
