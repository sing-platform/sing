import { Injectable } from "@nestjs/common";
import { formatarData } from "../api/data";
import { naoEncontrado } from "../api/erro-api";
import {
  EventosProcessados,
  NotificacaoUsuario,
  RepositorioNotificacoesUsuario,
  ResultadoSolicitacao,
  RespostaNotificacaoUsuario,
} from "./notificacao-usuario.tipos";
import { lerSolicitacao } from "./solicitacao";

const NAO_ENCONTRADA = "Notificação não encontrada";

function resposta(notificacao: NotificacaoUsuario): RespostaNotificacaoUsuario {
  return {
    id: notificacao.id,
    title: notificacao.title,
    message: notificacao.message,
    type: notificacao.type,
    actionUrl: notificacao.actionUrl,
    readAt: notificacao.readAt ? formatarData(notificacao.readAt) : null,
    createdAt: formatarData(notificacao.createdAt),
  };
}

@Injectable()
export class NotificacaoUsuarioService {
  constructor(
    private readonly repositorio: RepositorioNotificacoesUsuario,
    private readonly eventos: EventosProcessados,
  ) {}

  async listar(userId: number): Promise<RespostaNotificacaoUsuario[]> {
    const itens = await this.repositorio.listar(userId);
    return itens.map(resposta);
  }

  async marcarLida(id: number, userId: number): Promise<void> {
    const notificacao = await this.repositorio.buscar(id);
    if (!notificacao || notificacao.userId !== userId) {
      throw naoEncontrado(NAO_ENCONTRADA);
    }
    if (notificacao.readAt === null) {
      await this.repositorio.marcarLida(id, new Date());
    }
  }

  async marcarTodasLidas(userId: number): Promise<void> {
    await this.repositorio.marcarTodasLidas(userId, new Date());
  }

  async processarSolicitacao(evento: unknown): Promise<ResultadoSolicitacao> {
    const solicitacao = lerSolicitacao(evento);
    if (!solicitacao) {
      return "ignorada";
    }
    if (await this.eventos.jaProcessado(solicitacao.eventId)) {
      return "duplicada";
    }
    for (const userId of solicitacao.recipientUserIds) {
      if (await this.repositorio.existeNaoLida(userId, solicitacao.referenceKey)) {
        continue;
      }
      await this.repositorio.inserir({
        userId,
        title: solicitacao.title,
        message: solicitacao.message,
        type: solicitacao.type,
        actionUrl: solicitacao.actionUrl,
        referenceKey: solicitacao.referenceKey,
        readAt: null,
        createdAt: new Date(),
      });
    }
    await this.eventos.registrar(solicitacao.eventId);
    return "processada";
  }
}
