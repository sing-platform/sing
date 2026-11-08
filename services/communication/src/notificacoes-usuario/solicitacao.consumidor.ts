import { Injectable, OnModuleDestroy, OnModuleInit } from "@nestjs/common";
import * as amqp from "amqplib";
import { NotificacaoUsuarioService } from "./notificacao-usuario.service";
import { TIPO_EVENTO_SOLICITACAO } from "./solicitacao";

export const FILA_SOLICITACAO = "communication.notificacao.solicitada";

const ESPERA_ANTES_DE_REENFILEIRAR_MS = 2000;

@Injectable()
export class SolicitacaoConsumidor implements OnModuleInit, OnModuleDestroy {
  private parado = false;
  private conexao: amqp.ChannelModel | null = null;

  constructor(private readonly notificacoes: NotificacaoUsuarioService) {}

  onModuleInit(): void {
    void this.loop();
  }

  async onModuleDestroy(): Promise<void> {
    this.parado = true;
    await this.conexao?.close().catch(() => undefined);
  }

  private async loop(): Promise<void> {
    while (!this.parado) {
      try {
        await this.consumir();
      } catch (erro) {
        if (this.parado) {
          return;
        }
        console.warn("fila de notificacao solicitada indisponivel, nova tentativa", erro);
        await esperar(5000);
      }
    }
  }

  private async consumir(): Promise<void> {
    const exchange = process.env.RABBITMQ_EXCHANGE ?? "sing.events";
    const conexao = await amqp.connect({
      hostname: process.env.RABBITMQ_HOST ?? "localhost",
      port: Number(process.env.RABBITMQ_PORT ?? 5672),
      username: process.env.RABBITMQ_USER ?? "guest",
      password: process.env.RABBITMQ_PASSWORD ?? "guest",
    });
    this.conexao = conexao;
    const canal = await conexao.createChannel();
    await canal.assertExchange(exchange, "topic", { durable: true });
    await canal.assertQueue(FILA_SOLICITACAO, { durable: true });
    await canal.bindQueue(FILA_SOLICITACAO, exchange, TIPO_EVENTO_SOLICITACAO);
    await canal.prefetch(1);
    console.log("escutando", FILA_SOLICITACAO);

    await new Promise<void>((resolve, reject) => {
      conexao.on("error", reject);
      conexao.on("close", () => reject(new Error("conexao com o rabbit fechou")));
      void canal.consume(FILA_SOLICITACAO, async (msg) => {
        if (!msg) {
          return;
        }
        let evento: unknown;
        try {
          evento = JSON.parse(msg.content.toString("utf8"));
        } catch {
          console.warn("mensagem de notificacao solicitada nao e JSON, descartada");
          canal.ack(msg);
          return;
        }
        try {
          const resultado = await this.notificacoes.processarSolicitacao(evento);
          if (resultado === "ignorada") {
            console.warn("notificacao solicitada com payload invalido, descartada");
          }
          canal.ack(msg);
        } catch (erro) {
          console.warn("nao gravei a notificacao solicitada", erro);
          await esperar(ESPERA_ANTES_DE_REENFILEIRAR_MS);
          canal.nack(msg, false, true);
        }
      });
    });
  }
}

function esperar(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
