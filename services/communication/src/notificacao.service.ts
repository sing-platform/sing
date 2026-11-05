import { Injectable, OnModuleInit, ServiceUnavailableException } from "@nestjs/common";
import { Collection, MongoClient } from "mongodb";
import { aplicarEvento, Resultado } from "./evento";

@Injectable()
export class NotificacaoService implements OnModuleInit {
  private colecao: Collection | null = null;

  async onModuleInit(): Promise<void> {
    void this.tentarConectar();
  }

  async processar(evento: unknown): Promise<Resultado> {
    const colecao = this.exigirColecao();
    return aplicarEvento(evento, {
      buscar: async (eventId) => colecao.findOne({ eventId }),
      inserir: async (doc) => {
        await colecao.insertOne(doc);
      },
    });
  }

  async buscar(eventId: string): Promise<Record<string, unknown> | null> {
    const colecao = this.exigirColecao();
    const doc = await colecao.findOne({ eventId });
    if (!doc) {
      return null;
    }
    const { _id, ...resto } = doc;
    return resto;
  }

  private exigirColecao(): Collection {
    if (!this.colecao) {
      throw new ServiceUnavailableException("Mongo da comunicação ainda não está pronto");
    }
    return this.colecao;
  }

  private async tentarConectar(): Promise<void> {
    while (!this.colecao) {
      try {
        await this.conectar();
      } catch (erro) {
        console.warn("mongo da comunicacao indisponivel, nova tentativa", erro);
        await esperar(5000);
      }
    }
  }

  private async conectar(): Promise<void> {
    const uri =
      process.env.MONGO_URI ??
      "mongodb://communication:communication@localhost:27018/sing_communication?authSource=admin";
    const cliente = new MongoClient(uri, { serverSelectionTimeoutMS: 3000 });
    await cliente.connect();
    const colecao = cliente.db().collection("notificacoes");
    await colecao.createIndex({ eventId: 1 }, { unique: true });
    this.colecao = colecao;
  }
}

function esperar(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
