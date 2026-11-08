const { RepositorioMural } = require("../../dist/mural/mural.tipos");
const {
  RepositorioNotificacoesUsuario,
  EventosProcessados,
} = require("../../dist/notificacoes-usuario/notificacao-usuario.tipos");
const { ClienteCore } = require("../../dist/core/core.client");
const { OutboxEmail, PublicadorEmail } = require("../../dist/email/email.service");

class RepositorioMuralMemoria extends RepositorioMural {
  constructor() {
    super();
    this.posts = [];
    this.views = [];
    this.proximo = 1;
    this.relogio = 0;
  }

  async criarPost(dados) {
    const post = { id: this.proximo++, ...dados };
    this.posts.push(post);
    return { ...post };
  }

  async buscarPost(id) {
    const post = this.posts.find((item) => item.id === id);
    return post ? { ...post } : null;
  }

  async salvarPost(post) {
    const indice = this.posts.findIndex((item) => item.id === post.id);
    this.posts[indice] = { ...post };
  }

  async listarAtivos() {
    return this.posts
      .filter((post) => post.active)
      .sort((a, b) => b.createdAt - a.createdAt)
      .map((post) => ({ ...post }));
  }

  async listarComLeituraDoUsuario(userId) {
    return this.views
      .filter((view) => view.userId === userId)
      .map((view) => ({ post: this.posts.find((post) => post.id === view.postId), viewedAt: view.viewedAt }))
      .filter((item) => item.post.active)
      .sort((a, b) => b.post.createdAt - a.post.createdAt);
  }

  async buscarView(postId, userId) {
    const view = this.views.find((item) => item.postId === postId && item.userId === userId);
    return view ? { ...view } : null;
  }

  async criarViewsNovas(postId, userIds) {
    const criados = [];
    for (const userId of userIds) {
      if (!this.views.some((view) => view.postId === postId && view.userId === userId)) {
        this.views.push({ postId, userId, viewedAt: null });
        criados.push(userId);
      }
    }
    return criados;
  }

  async marcarTodasLidas(userId, agora) {
    for (const view of this.views) {
      if (view.userId === userId && view.viewedAt === null) {
        view.viewedAt = agora;
      }
    }
  }

  async contarNaoLidas(userId) {
    return this.views.filter((view) => view.userId === userId && view.viewedAt === null).length;
  }

  async engajamento(inicio, fimExclusivo) {
    return this.posts
      .filter((post) => post.active && post.createdAt >= inicio && post.createdAt < fimExclusivo)
      .sort((a, b) => b.createdAt - a.createdAt)
      .map((post) => {
        const views = this.views.filter((view) => view.postId === post.id);
        return {
          postId: post.id,
          title: post.title,
          createdAt: post.createdAt,
          authorUsername: post.authorEmail,
          viewCount: views.length,
          readCount: views.filter((view) => view.viewedAt !== null).length,
        };
      });
  }
}

class ClienteCoreFalso extends ClienteCore {
  constructor() {
    super();
    this.chamadas = [];
    this.resposta = null;
    this.erro = null;
  }

  async resolverDestinatarios(authorization, publico) {
    this.chamadas.push({ authorization, publico });
    if (this.erro) {
      throw this.erro;
    }
    return this.resposta;
  }
}

class PublicadorFalso extends PublicadorEmail {
  constructor() {
    super();
    this.enviadas = [];
    this.falhar = false;
    this.aoPublicar = null;
  }

  async publicar(mensagem) {
    if (this.aoPublicar) {
      this.aoPublicar(mensagem);
    }
    if (this.falhar) {
      throw new Error("broker fora");
    }
    this.enviadas.push(mensagem);
  }
}

class OutboxFalsa extends OutboxEmail {
  constructor() {
    super();
    this.itens = [];
    this.sequencia = 1;
  }

  async guardar(mensagem) {
    this.itens.push({ id: String(this.sequencia++), mensagem });
  }

  async pendentes(limite) {
    return this.itens.slice(0, limite);
  }

  async remover(id) {
    this.itens = this.itens.filter((item) => item.id !== id);
  }
}

class RepositorioNotificacoesMemoria extends RepositorioNotificacoesUsuario {
  constructor() {
    super();
    this.itens = [];
    this.proximo = 1;
  }

  async listar(userId) {
    return this.itens
      .filter((item) => item.userId === userId)
      .sort((a, b) => b.createdAt - a.createdAt || b.id - a.id)
      .map((item) => ({ ...item }));
  }

  async buscar(id) {
    const item = this.itens.find((candidato) => candidato.id === id);
    return item ? { ...item } : null;
  }

  async marcarLida(id, agora) {
    const item = this.itens.find((candidato) => candidato.id === id);
    if (item && item.readAt === null) {
      item.readAt = agora;
    }
  }

  async marcarTodasLidas(userId, agora) {
    for (const item of this.itens) {
      if (item.userId === userId && item.readAt === null) {
        item.readAt = agora;
      }
    }
  }

  async existeNaoLida(userId, referenceKey) {
    return this.itens.some((item) => item.userId === userId && item.referenceKey === referenceKey && item.readAt === null);
  }

  async inserir(dados) {
    this.itens.push({ id: this.proximo++, ...dados });
  }
}

class EventosProcessadosMemoria extends EventosProcessados {
  constructor() {
    super();
    this.ids = new Set();
  }

  async jaProcessado(eventId) {
    return this.ids.has(eventId);
  }

  async registrar(eventId) {
    this.ids.add(eventId);
  }
}

module.exports = {
  RepositorioMuralMemoria,
  ClienteCoreFalso,
  PublicadorFalso,
  OutboxFalsa,
  RepositorioNotificacoesMemoria,
  EventosProcessadosMemoria,
};
