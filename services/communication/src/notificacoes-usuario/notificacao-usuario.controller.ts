import { Controller, Get, HttpCode, Param, Patch, UseFilters, UseGuards } from "@nestjs/common";
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from "@nestjs/swagger";
import { FiltroErroApi } from "../api/erro-api";
import { JwtAuthGuard, Papeis, UsuarioAutenticado } from "../auth/auth.guard";
import { Usuario } from "../auth/usuario.decorator";
import { interpretarId } from "../mural/mural.validacao";
import { NotificacaoUsuarioService } from "./notificacao-usuario.service";
import { RespostaNotificacaoUsuario } from "./notificacao-usuario.tipos";

@ApiTags("Notificações")
@ApiBearerAuth()
@Controller("api/v1/notifications")
@UseGuards(JwtAuthGuard)
@UseFilters(FiltroErroApi)
@Papeis("SECRETARY", "ADMIN", "MANAGER")
export class NotificacaoUsuarioController {
  constructor(private readonly notificacoes: NotificacaoUsuarioService) {}

  @Get()
  @ApiOperation({ summary: "Listar notificações do usuário", description: "Da mais recente para a mais antiga." })
  listar(@Usuario() usuario: UsuarioAutenticado): Promise<RespostaNotificacaoUsuario[]> {
    return this.notificacoes.listar(usuario.userId);
  }

  @Patch("read-all")
  @HttpCode(204)
  @ApiOperation({ summary: "Marcar todas as notificações como lidas" })
  @ApiResponse({ status: 204, description: "Notificações marcadas como lidas" })
  async marcarTodasLidas(@Usuario() usuario: UsuarioAutenticado): Promise<void> {
    await this.notificacoes.marcarTodasLidas(usuario.userId);
  }

  @Patch(":id/read")
  @HttpCode(204)
  @ApiOperation({ summary: "Marcar uma notificação como lida" })
  @ApiResponse({ status: 204, description: "Notificação marcada como lida" })
  @ApiResponse({ status: 404, description: "Notificação não encontrada ou de outro usuário" })
  async marcarLida(@Param("id") id: string, @Usuario() usuario: UsuarioAutenticado): Promise<void> {
    await this.notificacoes.marcarLida(interpretarId(id, "Identificador de notificação inválido"), usuario.userId);
  }
}
