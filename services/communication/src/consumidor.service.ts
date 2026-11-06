import { Injectable, OnModuleDestroy, OnModuleInit } from "@nestjs/common";
import * as amqp from "amqplib";
import { NotificacaoService } from "./notificacao.service";

const FILA = "communication.matricula.criada";
const ROUTING_KEY = "matricula.criada";

@Injectable()
export class ConsumidorService implements OnModuleInit, OnModuleDestroy {
  private parado = false;
  private conexao: amqp.ChannelModel | null = null;

  constructor(private readonly notificacoes: NotificacaoService) {}

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
        console.warn("fila de matricula indisponivel, nova tentativa", erro);
        await esperar(5000);
      }
    }
  }

  private async consumir(): Promise<void> {
    const host = process.env.RABBITMQ_HOST ?? "localhost";
    const user = process.env.RABBITMQ_USER ?? "guest";
    const password = process.env.RABBITMQ_PASSWORD ?? "guest";
    const exchange = process.env.RABBITMQ_EXCHANGE ?? "sing.events";
    const conexao = await amqp.connect({
      hostname: host,
      port: Number(process.env.RABBITMQ_PORT ?? 5672),
      username: user,
      password,
    });
    this.conexao = conexao;
    const canal = await conexao.createChannel();
    await canal.assertExchange(exchange, "topic", { durable: true });
    await canal.assertQueue(FILA, { durable: true });
    await canal.bindQueue(FILA, exchange, ROUTING_KEY);
    await canal.prefetch(1);
    console.log("escutando", FILA);

    await new Promise<void>((resolve, reject) => {
      conexao.on("error", reject);
      conexao.on("close", () => reject(new Error("conexao com o rabbit fechou")));
      void canal.consume(FILA, async (msg) => {
        if (!msg) {
          return;
        }
        try {
          const evento = JSON.parse(msg.content.toString("utf8"));
          const resultado = await this.notificacoes.processar(evento);
          console.log("notificacao", evento.eventId, resultado);
          canal.ack(msg);
        } catch (erro) {
          console.warn("nao gravei a notificacao", erro);
          canal.nack(msg, false, true);
        }
      });
    });
  }
}

function esperar(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}
