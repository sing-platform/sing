import { Injectable, OnModuleDestroy } from "@nestjs/common";
import * as amqp from "amqplib";
import { MensagemEmail, PublicadorEmail } from "./email.service";

export const EXCHANGE_EMAIL = "sing.notifications";
export const ROUTING_KEY_EMAIL = "email.send";

const TIMEOUT_CONEXAO_MS = 5000;

@Injectable()
export class PublicadorEmailRabbit extends PublicadorEmail implements OnModuleDestroy {
  private conexao: amqp.ChannelModel | null = null;
  private canal: amqp.ConfirmChannel | null = null;
  private abrindo: Promise<amqp.ConfirmChannel> | null = null;

  async onModuleDestroy(): Promise<void> {
    await this.conexao?.close().catch(() => undefined);
  }

  async publicar(mensagem: MensagemEmail): Promise<void> {
    const canal = await this.obterCanal();
    try {
      await new Promise<void>((resolve, reject) => {
        canal.publish(
          EXCHANGE_EMAIL,
          ROUTING_KEY_EMAIL,
          Buffer.from(JSON.stringify(mensagem), "utf8"),
          { contentType: "application/json", persistent: true },
          (erro) => (erro ? reject(erro) : resolve()),
        );
      });
    } catch (erro) {
      this.descartarCanal();
      throw erro;
    }
  }

  private obterCanal(): Promise<amqp.ConfirmChannel> {
    if (this.canal) {
      return Promise.resolve(this.canal);
    }
    if (!this.abrindo) {
      this.abrindo = this.abrir().finally(() => {
        this.abrindo = null;
      });
    }
    return this.abrindo;
  }

  private async abrir(): Promise<amqp.ConfirmChannel> {
    const conexao = await comTimeout(
      amqp.connect({
        hostname: process.env.RABBITMQ_HOST ?? "localhost",
        port: Number(process.env.RABBITMQ_PORT ?? 5672),
        username: process.env.RABBITMQ_USER ?? "guest",
        password: process.env.RABBITMQ_PASSWORD ?? "guest",
      }),
      TIMEOUT_CONEXAO_MS,
    );
    conexao.on("error", () => this.descartarCanal());
    conexao.on("close", () => this.descartarCanal());
    try {
      const canal = await conexao.createConfirmChannel();
      await canal.assertExchange(EXCHANGE_EMAIL, "topic", { durable: true });
      this.conexao = conexao;
      this.canal = canal;
      return canal;
    } catch (erro) {
      await conexao.close().catch(() => undefined);
      throw erro;
    }
  }

  private descartarCanal(): void {
    const conexao = this.conexao;
    this.canal = null;
    this.conexao = null;
    void conexao?.close().catch(() => undefined);
  }
}

function comTimeout<T>(promessa: Promise<T>, ms: number): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const relogio = setTimeout(() => reject(new Error("timeout ao conectar no rabbit")), ms);
    promessa.then(
      (valor) => {
        clearTimeout(relogio);
        resolve(valor);
      },
      (erro) => {
        clearTimeout(relogio);
        reject(erro);
      },
    );
  });
}
