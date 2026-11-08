const assert = require("node:assert/strict");
const test = require("node:test");
const { EmailService } = require("../dist/email/email.service");
const { PublicadorEmailRabbit, EXCHANGE_EMAIL, ROUTING_KEY_EMAIL } = require("../dist/email/email.rabbit");
const { montarEmailMural } = require("../dist/mural/mural.email");
const { OutboxFalsa, PublicadorFalso } = require("./support/memoria");

const POST = {
  id: 1,
  title: "Reunião de pais",
  content: "Primeira linha\nSegunda <b>linha</b>",
  authorName: "Ana Souza",
  createdAt: new Date("2026-11-08T17:00:00.123Z"),
};

test("e-mail do mural tem assunto e HTML identicos ao do core", () => {
  const mensagem = montarEmailMural(POST, "aluno@sing.local");
  assert.deepEqual(mensagem, {
    to: "aluno@sing.local",
    subject: "Nova publicação no mural: Reunião de pais",
    body:
      "<html><body>\n" +
      "<h2>Nova publicação no Mural da escola</h2>\n" +
      "<h3>Reunião de pais</h3>\n" +
      "<p>Primeira linha<br/>Segunda <b>linha</b></p>\n" +
      "<hr/>\n" +
      "<p><small>Publicado por <strong>Ana Souza</strong> em 2026-11-08.<br/>\n" +
      "Acesse o sistema para visualizar.</small></p>\n" +
      "</body></html>\n",
    html: true,
  });
});

test("publica quando o broker responde e nao toca na outbox", async () => {
  const publicador = new PublicadorFalso();
  const outbox = new OutboxFalsa();
  const servico = new EmailService(publicador, outbox);
  const mensagem = montarEmailMural(POST, "a@sing.local");
  await servico.enviar(mensagem);
  assert.deepEqual(publicador.enviadas, [mensagem]);
  assert.equal(outbox.itens.length, 0);
});

test("se a publicacao falha a mensagem vai para a outbox", async () => {
  const publicador = new PublicadorFalso();
  publicador.falhar = true;
  const outbox = new OutboxFalsa();
  const servico = new EmailService(publicador, outbox);
  const mensagem = montarEmailMural(POST, "a@sing.local");
  await servico.enviar(mensagem);
  assert.deepEqual(outbox.itens.map((item) => item.mensagem), [mensagem]);
});

test("reenvio publica pendentes, remove da outbox e para no primeiro erro", async () => {
  const publicador = new PublicadorFalso();
  publicador.falhar = true;
  const outbox = new OutboxFalsa();
  const servico = new EmailService(publicador, outbox);
  for (const to of ["1@sing.local", "2@sing.local", "3@sing.local"]) {
    await servico.enviar(montarEmailMural(POST, to));
  }
  assert.equal(await servico.reenviarPendentes(), 0);
  assert.equal(outbox.itens.length, 3);

  publicador.falhar = false;
  assert.equal(await servico.reenviarPendentes(), 3);
  assert.equal(outbox.itens.length, 0);
  assert.deepEqual(publicador.enviadas.map((mensagem) => mensagem.to), ["1@sing.local", "2@sing.local", "3@sing.local"]);
});

test("falha da outbox nao derruba o envio", async () => {
  const publicador = new PublicadorFalso();
  publicador.falhar = true;
  const outbox = new OutboxFalsa();
  outbox.guardar = async () => {
    throw new Error("mongo fora");
  };
  const servico = new EmailService(publicador, outbox);
  await servico.enviar(montarEmailMural(POST, "a@sing.local"));
});

test("publicador do rabbit usa exchange, routing key, content_type e persistencia do contrato", async () => {
  const publicacoes = [];
  const canal = {
    publish(exchange, rk, conteudo, opcoes, retorno) {
      publicacoes.push({ exchange, rk, conteudo, opcoes });
      retorno(null);
      return true;
    },
  };
  const publicador = new PublicadorEmailRabbit();
  publicador.obterCanal = async () => canal;
  const mensagem = montarEmailMural(POST, "aluno@sing.local");
  await publicador.publicar(mensagem);
  assert.equal(publicacoes.length, 1);
  assert.equal(publicacoes[0].exchange, "sing.notifications");
  assert.equal(EXCHANGE_EMAIL, "sing.notifications");
  assert.equal(publicacoes[0].rk, "email.send");
  assert.equal(ROUTING_KEY_EMAIL, "email.send");
  assert.equal(publicacoes[0].opcoes.contentType, "application/json");
  assert.equal(publicacoes[0].opcoes.persistent, true);
  assert.deepEqual(JSON.parse(publicacoes[0].conteudo.toString("utf8")), {
    to: "aluno@sing.local",
    subject: "Nova publicação no mural: Reunião de pais",
    body: mensagem.body,
    html: true,
  });
});

test("erro de confirmacao do broker propaga para a outbox assumir", async () => {
  const canal = {
    publish(_e, _r, _c, _o, retorno) {
      retorno(new Error("nack"));
      return true;
    },
  };
  const publicador = new PublicadorEmailRabbit();
  publicador.obterCanal = async () => canal;
  publicador.descartarCanal = () => undefined;
  await assert.rejects(publicador.publicar(montarEmailMural(POST, "a@sing.local")), /nack/);
});
