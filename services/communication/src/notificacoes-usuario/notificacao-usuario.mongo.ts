import { Injectable } from "@nestjs/common";
import { MongoService } from "../mongo/mongo.service";
import { proximoId } from "../mongo/sequencia";
import {
  EventosProcessados,
  NotificacaoUsuario,
  NovaNotificacaoUsuario,
  RepositorioNotificacoesUsuario,
} from "./notificacao-usuario.tipos";

type Documento = Omit<NotificacaoUsuario, "id"> & { _id: number };

@Injectable()
export class RepositorioNotificacoesUsuarioMongo extends RepositorioNotificacoesUsuario {
  constructor(private readonly mongo: MongoService) {
    super();
  }

  async listar(userId: number): Promise<NotificacaoUsuario[]> {
    const docs = await this.colecao().find({ userId }).sort({ createdAt: -1, _id: -1 }).toArray();
    return docs.map(paraNotificacao);
  }

  async buscar(id: number): Promise<NotificacaoUsuario | null> {
    const doc = await this.colecao().findOne({ _id: id });
    return doc ? paraNotificacao(doc) : null;
  }

  async marcarLida(id: number, agora: Date): Promise<void> {
    await this.colecao().updateOne({ _id: id, readAt: null }, { $set: { readAt: agora } });
  }

  async marcarTodasLidas(userId: number, agora: Date): Promise<void> {
    await this.colecao().updateMany({ userId, readAt: null }, { $set: { readAt: agora } });
  }

  async existeNaoLida(userId: number, referenceKey: string): Promise<boolean> {
    return (await this.colecao().countDocuments({ userId, referenceKey, readAt: null }, { limit: 1 })) > 0;
  }

  async inserir(dados: NovaNotificacaoUsuario): Promise<void> {
    const id = await proximoId(this.mongo.db(), "notificacoes_usuario");
    await this.colecao().insertOne({ _id: id, ...dados });
  }

  private colecao() {
    return this.mongo.db().collection<Documento>("notificacoes_usuario");
  }
}

@Injectable()
export class EventosProcessadosMongo extends EventosProcessados {
  constructor(private readonly mongo: MongoService) {
    super();
  }

  async jaProcessado(eventId: string): Promise<boolean> {
    return (await this.colecao().countDocuments({ eventId }, { limit: 1 })) > 0;
  }

  async registrar(eventId: string): Promise<void> {
    try {
      await this.colecao().insertOne({ eventId, processedAt: new Date() });
    } catch (erro) {
      if ((erro as { code?: number }).code !== 11000) {
        throw erro;
      }
    }
  }

  private colecao() {
    return this.mongo.db().collection<{ eventId: string; processedAt: Date }>("eventos_processados");
  }
}

function paraNotificacao(doc: Documento): NotificacaoUsuario {
  const { _id, ...resto } = doc;
  return { id: _id, ...resto };
}
