import { Controller, Get, NotFoundException, Param } from "@nestjs/common";
import { NotificacaoService } from "./notificacao.service";

@Controller("api/v1/comunicacao/notificacoes")
export class NotificacaoController {
  constructor(private readonly notificacoes: NotificacaoService) {}

  @Get(":eventId")
  async buscar(@Param("eventId") eventId: string): Promise<Record<string, unknown>> {
    const doc = await this.notificacoes.buscar(eventId);
    if (!doc) {
      throw new NotFoundException("Notificação não encontrada");
    }
    return doc;
  }
}
