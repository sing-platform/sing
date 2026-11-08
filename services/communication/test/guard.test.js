require("reflect-metadata");
const assert = require("node:assert/strict");
const test = require("node:test");
const { Reflector } = require("@nestjs/core");
const { JwtAuthGuard } = require("../dist/auth/auth.guard");
const { montarCorpo, traduzir } = require("../dist/api/erro-api");
const { MuralController } = require("../dist/mural/mural.controller");
const { NotificacaoUsuarioController } = require("../dist/notificacoes-usuario/notificacao-usuario.controller");
const { NotificacaoController } = require("../dist/notificacao.controller");
const { assinar } = require("./support/token");

const SEGREDO = "segredo-do-guard-com-hifen-nao-base64";
const CHAVE = Buffer.from(SEGREDO, "utf8");

function guardCom(segredo) {
  const anterior = process.env.JWT_SECRET;
  if (segredo === undefined) {
    delete process.env.JWT_SECRET;
  } else {
    process.env.JWT_SECRET = segredo;
  }
  try {
    return new JwtAuthGuard(new Reflector());
  } finally {
    if (anterior === undefined) {
      delete process.env.JWT_SECRET;
    } else {
      process.env.JWT_SECRET = anterior;
    }
  }
}

function contexto(Controlador, metodo, authorization) {
  const req = { headers: authorization === undefined ? {} : { authorization } };
  return {
    req,
    contexto: {
      switchToHttp: () => ({ getRequest: () => req }),
      getHandler: () => Controlador.prototype[metodo],
      getClass: () => Controlador,
    },
  };
}

function tentar(guard, Controlador, metodo, authorization) {
  const { req, contexto: ctx } = contexto(Controlador, metodo, authorization);
  try {
    return { ok: guard.canActivate(ctx), req };
  } catch (erro) {
    return { erro: traduzir(erro), req };
  }
}

function bearer(role, extras = {}) {
  return `Bearer ${assinar({ chave: CHAVE, claims: { role, uid: 11, ...extras } })}`;
}

const guard = guardCom(SEGREDO);

test("sem token responde 401 no formato ApiError", () => {
  const { erro } = tentar(guard, MuralController, "listar", undefined);
  assert.equal(erro.status, 401);
  const corpo = montarCorpo(erro, "/api/v1/mural");
  assert.equal(corpo.status, 401);
  assert.equal(corpo.error, "Unauthorized");
  assert.equal(corpo.message, "Authentication required");
  assert.equal(corpo.path, "/api/v1/mural");
  assert.equal(corpo.details, null);
  assert.match(corpo.timestamp, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+Z$/);
});

test("cabecalho sem Bearer, token lixo e assinatura errada respondem 401", () => {
  assert.equal(tentar(guard, MuralController, "listar", "Basic abc").erro.status, 401);
  assert.equal(tentar(guard, MuralController, "listar", "Bearer lixo").erro.status, 401);
  const errado = `Bearer ${assinar({ chave: Buffer.from("outra-chave-qualquer"), claims: { uid: 1 } })}`;
  assert.equal(tentar(guard, MuralController, "listar", errado).erro.status, 401);
});

test("token expirado responde 401", () => {
  const expirado = `Bearer ${assinar({ chave: CHAVE, claims: { exp: 1000 } })}`;
  assert.equal(tentar(guard, MuralController, "listar", expirado).erro.status, 401);
});

test("token sem uid responde 401", () => {
  assert.equal(tentar(guard, MuralController, "listar", bearer("ROLE_ADMIN", { uid: undefined })).erro.status, 401);
});

test("segredo vazio ou ausente recusa tokens validos", () => {
  for (const segredo of ["", undefined]) {
    const semChave = guardCom(segredo);
    assert.equal(tentar(semChave, MuralController, "listar", bearer("ROLE_ADMIN")).erro.status, 401);
  }
});

test("rotas de leitura aceitam qualquer papel autenticado e preenchem o usuario", () => {
  for (const metodo of ["listar", "buscar", "naoLidas", "marcarTodasLidas"]) {
    const { ok, req } = tentar(guard, MuralController, metodo, bearer("ROLE_STUDENT", { pid: 5 }));
    assert.equal(ok, true, metodo);
    assert.equal(req.usuario.userId, 11);
    assert.equal(req.usuario.personId, 5);
    assert.equal(req.usuario.role, "ROLE_STUDENT");
    assert.match(req.usuario.authorization, /^Bearer /);
  }
});

test("escrita do mural exige PROFESSOR, SECRETARY ou ADMIN", () => {
  for (const metodo of ["criar", "atualizar", "remover"]) {
    for (const role of ["ROLE_PROFESSOR", "ROLE_SECRETARY", "ROLE_ADMIN"]) {
      assert.equal(tentar(guard, MuralController, metodo, bearer(role)).ok, true, `${metodo} ${role}`);
    }
    for (const role of ["ROLE_STUDENT", "ROLE_PARENT", "ROLE_MANAGER"]) {
      assert.equal(tentar(guard, MuralController, metodo, bearer(role)).erro.status, 403, `${metodo} ${role}`);
    }
  }
});

test("papel ausente no token nao passa por rota restrita", () => {
  const semPapel = `Bearer ${assinar({ chave: CHAVE, claims: { role: undefined, uid: 3 } })}`;
  assert.equal(tentar(guard, MuralController, "criar", semPapel).erro.status, 403);
});

test("403 por papel usa a mensagem do core", () => {
  const { erro } = tentar(guard, MuralController, "criar", bearer("ROLE_STUDENT"));
  const corpo = montarCorpo(erro, "/api/v1/mural");
  assert.equal(corpo.status, 403);
  assert.equal(corpo.error, "Forbidden");
  assert.equal(corpo.message, "Você não tem permissão para realizar esta ação.");
});

test("engajamento exige ADMIN, SECRETARY ou MANAGER", () => {
  for (const role of ["ROLE_ADMIN", "ROLE_SECRETARY", "ROLE_MANAGER"]) {
    assert.equal(tentar(guard, MuralController, "engajamento", bearer(role)).ok, true, role);
  }
  for (const role of ["ROLE_PROFESSOR", "ROLE_STUDENT"]) {
    assert.equal(tentar(guard, MuralController, "engajamento", bearer(role)).erro.status, 403, role);
  }
});

test("notificacoes in-app exigem SECRETARY, ADMIN ou MANAGER", () => {
  for (const metodo of ["listar", "marcarLida", "marcarTodasLidas"]) {
    for (const role of ["ROLE_ADMIN", "ROLE_SECRETARY", "ROLE_MANAGER"]) {
      assert.equal(tentar(guard, NotificacaoUsuarioController, metodo, bearer(role)).ok, true, `${metodo} ${role}`);
    }
    for (const role of ["ROLE_PROFESSOR", "ROLE_STUDENT", "ROLE_PARENT"]) {
      assert.equal(tentar(guard, NotificacaoUsuarioController, metodo, bearer(role)).erro.status, 403, `${metodo} ${role}`);
    }
  }
});

test("a notificacao da matricula continua publica", () => {
  const guards = Reflect.getMetadata("__guards__", NotificacaoController) ?? [];
  assert.equal(guards.length, 0);
});

test("rotas estaticas vem antes de :id no controller do mural", () => {
  const caminhos = Object.getOwnPropertyNames(MuralController.prototype)
    .filter((nome) => nome !== "constructor")
    .map((nome) => ({
      nome,
      caminho: Reflect.getMetadata("path", MuralController.prototype[nome]),
      metodo: Reflect.getMetadata("method", MuralController.prototype[nome]),
    }));
  const ordemGet = caminhos.filter((rota) => rota.metodo === 0).map((rota) => rota.caminho);
  assert.ok(ordemGet.indexOf("unread-count") < ordemGet.indexOf(":id"));
  assert.ok(ordemGet.indexOf("engajamento") < ordemGet.indexOf(":id"));
});
