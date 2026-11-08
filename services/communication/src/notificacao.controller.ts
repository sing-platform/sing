import { Controller, Get, NotFoundException, Param } from "@nestjs/common";
import { ApiOperation, ApiParam } from "@nestjs/swagger";
import { NotificacaoService } from "./notificacao.service";

@Controller("api/v1/comunicacao/notificacoes")
export class NotificacaoController {
  constructor(private readonly notificacoes: NotificacaoService) {}

  @Get(":eventId")
  @ApiOperation({ summary: "Notificacao gravada para o eventId da matricula" })
  @ApiParam({ name: "eventId", example: "00000000-0000-4000-8000-000000000000" })
  async buscar(@Param("eventId") eventId: string): Promise<Record<string, unknown>> {
    const doc = await this.notificacoes.buscar(eventId);
    if (!doc) {
      throw new NotFoundException("Notificação não encontrada");
    }
    return doc;
  }
}
