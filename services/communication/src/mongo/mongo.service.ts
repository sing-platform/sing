import { Injectable, OnModuleDestroy, OnModuleInit } from "@nestjs/common";
import { Db, MongoClient } from "mongodb";
import { ErroApi } from "../api/erro-api";

@Injectable()
export class MongoService implements OnModuleInit, OnModuleDestroy {
  private banco: Db | null = null;
  private cliente: MongoClient | null = null;
  private parado = false;

  onModuleInit(): void {
    void this.conectarComRetentativa();
  }

  async onModuleDestroy(): Promise<void> {
    this.parado = true;
    await this.cliente?.close().catch(() => undefined);
  }

  db(): Db {
    if (!this.banco) {
      throw new ErroApi(503, "Mongo da comunicação ainda não está pronto");
    }
    return this.banco;
  }

  private async conectarComRetentativa(): Promise<void> {
    while (!this.banco && !this.parado) {
      try {
        await this.conectar();
      } catch (erro) {
        console.warn("mongo do mural indisponivel, nova tentativa", erro);
        await new Promise((resolve) => setTimeout(resolve, 5000));
      }
    }
  }

  private async conectar(): Promise<void> {
    const uri =
      process.env.MONGO_URI ??
      "mongodb://communication:communication@localhost:27018/sing_communication?authSource=admin";
    const cliente = new MongoClient(uri, { serverSelectionTimeoutMS: 3000 });
    await cliente.connect();
    const banco = cliente.db();
    await Promise.all([
      banco.collection("mural_views").createIndex({ postId: 1, userId: 1 }, { unique: true }),
      banco.collection("mural_views").createIndex({ userId: 1, viewedAt: 1 }),
      banco.collection("mural_posts").createIndex({ active: 1, createdAt: -1 }),
      banco.collection("notificacoes_usuario").createIndex({ userId: 1, readAt: 1 }),
      banco.collection("eventos_processados").createIndex({ eventId: 1 }, { unique: true }),
      banco.collection("email_outbox").createIndex({ createdAt: 1 }),
    ]);
    this.cliente = cliente;
    this.banco = banco;
  }
}
