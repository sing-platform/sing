import { Injectable } from "@nestjs/common";
import { formatarData } from "../api/data";
import { ErroApi, naoEncontrado } from "../api/erro-api";
import { UsuarioAutenticado } from "../auth/auth.guard";
import { ClienteCore, DestinatarioResolvido } from "../core/core.client";
import { EmailService } from "../email/email.service";
import { montarEmailMural } from "./mural.email";
import {
  CorpoPublicacao,
  LinhaEngajamento,
  PostMural,
  RepositorioMural,
  RespostaEngajamento,
  RespostaMural,
} from "./mural.tipos";

const PAPEIS_GESTORES = ["ROLE_ADMIN", "ROLE_SECRETARY"];
const POST_NAO_ENCONTRADO = "Publicação não encontrada";

export function ehGestor(role: string | null): boolean {
  return role !== null && PAPEIS_GESTORES.includes(role);
}

function resposta(post: PostMural, viewed: boolean): RespostaMural {
  return {
    id: post.id,
    title: post.title,
    content: post.content,
    authorName: post.authorName,
    createdAt: formatarData(post.createdAt),
    updatedAt: post.updatedAt ? formatarData(post.updatedAt) : null,
    audienceRoles: post.audienceRoles,
    audienceSchoolroomIds: post.audienceSchoolroomIds,
    viewed,
  };
}

@Injectable()
export class MuralService {
  constructor(
    private readonly repositorio: RepositorioMural,
    private readonly core: ClienteCore,
    private readonly email: EmailService,
  ) {}

  async listar(usuario: UsuarioAutenticado): Promise<RespostaMural[]> {
    if (ehGestor(usuario.role)) {
      const posts = await this.repositorio.listarAtivos();
      return posts.map((post) => resposta(post, true));
    }
    const itens = await this.repositorio.listarComLeituraDoUsuario(usuario.userId);
    return itens.map((item) => resposta(item.post, item.viewedAt !== null));
  }

  async buscar(id: number, usuario: UsuarioAutenticado): Promise<RespostaMural> {
    const post = await this.exigirPostAtivo(id);
    if (ehGestor(usuario.role)) {
      return resposta(post, true);
    }
    const view = await this.repositorio.buscarView(id, usuario.userId);
    if (!view) {
      throw new ErroApi(403, "Acesso não autorizado a esta publicação");
    }
    return resposta(post, view.viewedAt !== null);
  }

  async criar(corpo: CorpoPublicacao, usuario: UsuarioAutenticado): Promise<RespostaMural> {
    const resolvidos = await this.core.resolverDestinatarios(usuario.authorization, corpo);
    const post = await this.repositorio.criarPost({
      title: corpo.title,
      content: corpo.content,
      authorUserId: usuario.userId,
      authorName: resolvidos.author.name,
      authorEmail: resolvidos.author.email,
      createdAt: new Date(),
      updatedAt: null,
      audienceRoles: corpo.audienceRoles,
      audienceSchoolroomIds: corpo.audienceSchoolroomIds,
      active: true,
    });
    await this.registrarDestinatariosENotificar(post, resolvidos.recipients);
    return resposta(post, false);
  }

  async atualizar(id: number, corpo: CorpoPublicacao, usuario: UsuarioAutenticado): Promise<RespostaMural> {
    const existente = await this.exigirPostAtivo(id);
    this.exigirAcessoDeEscrita(existente, usuario);
    const resolvidos = await this.core.resolverDestinatarios(usuario.authorization, corpo);
    const atualizado: PostMural = {
      ...existente,
      title: corpo.title,
      content: corpo.content,
      audienceRoles: corpo.audienceRoles,
      audienceSchoolroomIds: corpo.audienceSchoolroomIds,
      updatedAt: new Date(),
    };
    await this.repositorio.salvarPost(atualizado);
    await this.registrarDestinatariosENotificar(atualizado, resolvidos.recipients);
    return resposta(atualizado, false);
  }

  async remover(id: number, usuario: UsuarioAutenticado): Promise<void> {
    const post = await this.exigirPostAtivo(id);
    this.exigirAcessoDeEscrita(post, usuario);
    await this.repositorio.salvarPost({ ...post, active: false });
  }

  async marcarTodasLidas(usuario: UsuarioAutenticado): Promise<void> {
    if (ehGestor(usuario.role)) {
      return;
    }
    await this.repositorio.marcarTodasLidas(usuario.userId, new Date());
  }

  async contarNaoLidas(usuario: UsuarioAutenticado): Promise<number> {
    if (ehGestor(usuario.role)) {
      return 0;
    }
    return this.repositorio.contarNaoLidas(usuario.userId);
  }

  async engajamento(inicio: Date, fimExclusivo: Date): Promise<RespostaEngajamento[]> {
    const linhas: LinhaEngajamento[] = await this.repositorio.engajamento(inicio, fimExclusivo);
    return linhas.map((linha) => ({
      postId: linha.postId,
      title: linha.title,
      createdAt: formatarData(linha.createdAt),
      authorUsername: linha.authorUsername,
      viewCount: linha.viewCount,
      readCount: linha.readCount,
    }));
  }

  private async exigirPostAtivo(id: number): Promise<PostMural> {
    const post = await this.repositorio.buscarPost(id);
    if (!post || !post.active) {
      throw naoEncontrado(POST_NAO_ENCONTRADO);
    }
    return post;
  }

  private exigirAcessoDeEscrita(post: PostMural, usuario: UsuarioAutenticado): void {
    if (ehGestor(usuario.role)) {
      return;
    }
    if (post.authorUserId !== usuario.userId) {
      throw new ErroApi(403, "Apenas o autor pode modificar esta publicação");
    }
  }

  private async registrarDestinatariosENotificar(post: PostMural, destinatarios: DestinatarioResolvido[]): Promise<void> {
    const porUsuario = new Map<number, string>();
    for (const destinatario of destinatarios) {
      porUsuario.set(destinatario.userId, destinatario.email);
    }
    const novos = await this.repositorio.criarViewsNovas(post.id, [...porUsuario.keys()]);
    for (const userId of novos) {
      await this.email.enviar(montarEmailMural(post, porUsuario.get(userId) as string));
    }
  }
}
