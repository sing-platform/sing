import { Injectable, OnModuleDestroy, OnModuleInit } from "@nestjs/common";

export interface MensagemEmail {
  to: string;
  subject: string;
  body: string;
  html: boolean;
}

export interface EmailPendente {
  id: string;
  mensagem: MensagemEmail;
}

export abstract class PublicadorEmail {
  abstract publicar(mensagem: MensagemEmail): Promise<void>;
}

export abstract class OutboxEmail {
  abstract guardar(mensagem: MensagemEmail): Promise<void>;
  abstract pendentes(limite: number): Promise<EmailPendente[]>;
  abstract remover(id: string): Promise<void>;
}

const INTERVALO_REENVIO_MS = 60_000;
const LOTE_REENVIO = 100;

@Injectable()
export class EmailService implements OnModuleInit, OnModuleDestroy {
  private temporizador: NodeJS.Timeout | null = null;

  constructor(
    private readonly publicador: PublicadorEmail,
    private readonly outbox: OutboxEmail,
  ) {}

  onModuleInit(): void {
    this.temporizador = setInterval(() => void this.reenviarPendentes(), INTERVALO_REENVIO_MS);
    this.temporizador.unref();
  }

  onModuleDestroy(): void {
    if (this.temporizador) {
      clearInterval(this.temporizador);
    }
  }

  async enviar(mensagem: MensagemEmail): Promise<void> {
    try {
      await this.publicador.publicar(mensagem);
    } catch (erro) {
      console.warn("broker indisponivel, e-mail vai para a outbox", erro);
      try {
        await this.outbox.guardar(mensagem);
      } catch (erroOutbox) {
        console.error("e-mail perdido: broker e outbox indisponiveis", erroOutbox);
      }
    }
  }

  async reenviarPendentes(): Promise<number> {
    let reenviados = 0;
    try {
      const pendentes = await this.outbox.pendentes(LOTE_REENVIO);
      for (const pendente of pendentes) {
        await this.publicador.publicar(pendente.mensagem);
        await this.outbox.remover(pendente.id);
        reenviados += 1;
      }
    } catch (erro) {
      console.warn("reenvio da outbox adiado", erro);
    }
    return reenviados;
  }
}
