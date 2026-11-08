import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  Param,
  Post,
  Put,
  Query,
  UseFilters,
  UseGuards,
} from "@nestjs/common";
import { ApiBearerAuth, ApiBody, ApiOperation, ApiQuery, ApiResponse, ApiTags } from "@nestjs/swagger";
import { FiltroErroApi } from "../api/erro-api";
import { JwtAuthGuard, Papeis, UsuarioAutenticado } from "../auth/auth.guard";
import { Usuario } from "../auth/usuario.decorator";
import { MuralService } from "./mural.service";
import { RespostaEngajamento, RespostaMural } from "./mural.tipos";
import { interpretarData, interpretarId, validarPublicacao } from "./mural.validacao";

const MENSAGEM_ID = "Identificador de publicação inválido";
const UM_DIA_MS = 24 * 60 * 60 * 1000;

const CORPO_PUBLICACAO = {
  schema: {
    type: "object",
    required: ["title", "content"],
    properties: {
      title: { type: "string", maxLength: 255, example: "Reunião pedagógica — próxima segunda-feira" },
      content: { type: "string", example: "A reunião mensal será na segunda-feira às 17 h no auditório." },
      audienceRoles: { type: "array", items: { type: "string" }, example: ["ROLE_STUDENT", "ROLE_PARENT"] },
      audienceSchoolroomIds: { type: "array", items: { type: "integer" }, example: [1, 3] },
    },
  },
};

@ApiTags("Mural Institucional")
@ApiBearerAuth()
@Controller("api/v1/mural")
@UseGuards(JwtAuthGuard)
@UseFilters(FiltroErroApi)
export class MuralController {
  constructor(private readonly mural: MuralService) {}

  @Get()
  @ApiOperation({
    summary: "Listar publicações do mural",
    description:
      "ADMIN e SECRETARY veem todos os posts ativos. Os demais veem os posts dos quais são destinatários, com o status de leitura.",
  })
  @ApiResponse({ status: 200, description: "Lista de publicações" })
  listar(@Usuario() usuario: UsuarioAutenticado): Promise<RespostaMural[]> {
    return this.mural.listar(usuario);
  }

  @Get("unread-count")
  @ApiOperation({ summary: "Quantidade de publicações não lidas", description: "Sempre 0 para ADMIN e SECRETARY." })
  async naoLidas(@Usuario() usuario: UsuarioAutenticado): Promise<{ unreadCount: number }> {
    return { unreadCount: await this.mural.contarNaoLidas(usuario) };
  }

  @Get("engajamento")
  @Papeis("ADMIN", "SECRETARY", "MANAGER")
  @ApiOperation({
    summary: "Engajamento do mural no período",
    description: "Uma linha por post ativo criado entre start e end (inclusive), do mais recente ao mais antigo.",
  })
  @ApiQuery({ name: "start", example: "2026-11-01" })
  @ApiQuery({ name: "end", example: "2026-11-30" })
  @ApiResponse({ status: 400, description: "Datas ausentes ou fora do formato yyyy-MM-dd" })
  @ApiResponse({ status: 403, description: "Papel sem permissão" })
  engajamento(@Query("start") start: unknown, @Query("end") end: unknown): Promise<RespostaEngajamento[]> {
    const inicio = interpretarData(start, "start");
    const fim = interpretarData(end, "end");
    return this.mural.engajamento(inicio, new Date(fim.getTime() + UM_DIA_MS));
  }

  @Get(":id")
  @ApiOperation({ summary: "Buscar publicação por id" })
  @ApiResponse({ status: 403, description: "Usuário não é destinatário" })
  @ApiResponse({ status: 404, description: "Publicação não encontrada" })
  buscar(@Param("id") id: string, @Usuario() usuario: UsuarioAutenticado): Promise<RespostaMural> {
    return this.mural.buscar(interpretarId(id, MENSAGEM_ID), usuario);
  }

  @Post("mark-all-read")
  @HttpCode(204)
  @ApiOperation({ summary: "Marcar todas as publicações como lidas", description: "Sem efeito para ADMIN e SECRETARY." })
  @ApiResponse({ status: 204, description: "Publicações marcadas como lidas" })
  async marcarTodasLidas(@Usuario() usuario: UsuarioAutenticado): Promise<void> {
    await this.mural.marcarTodasLidas(usuario);
  }

  @Post()
  @HttpCode(201)
  @Papeis("PROFESSOR", "SECRETARY", "ADMIN")
  @ApiOperation({ summary: "Criar publicação" })
  @ApiBody(CORPO_PUBLICACAO)
  @ApiResponse({ status: 201, description: "Publicação criada" })
  @ApiResponse({ status: 400, description: "Validação do corpo ou do público-alvo" })
  @ApiResponse({ status: 403, description: "Papel ou público-alvo sem permissão" })
  @ApiResponse({ status: 503, description: "Serviço de usuários indisponível" })
  criar(@Body() corpo: unknown, @Usuario() usuario: UsuarioAutenticado): Promise<RespostaMural> {
    return this.mural.criar(validarPublicacao(corpo), usuario);
  }

  @Put(":id")
  @Papeis("PROFESSOR", "SECRETARY", "ADMIN")
  @ApiOperation({ summary: "Atualizar publicação", description: "Substitui título, conteúdo e público-alvo." })
  @ApiBody(CORPO_PUBLICACAO)
  @ApiResponse({ status: 403, description: "Apenas o autor ou um gestor pode modificar" })
  @ApiResponse({ status: 404, description: "Publicação não encontrada" })
  atualizar(
    @Param("id") id: string,
    @Body() corpo: unknown,
    @Usuario() usuario: UsuarioAutenticado,
  ): Promise<RespostaMural> {
    const publicacao = validarPublicacao(corpo);
    return this.mural.atualizar(interpretarId(id, MENSAGEM_ID), publicacao, usuario);
  }

  @Delete(":id")
  @HttpCode(204)
  @Papeis("PROFESSOR", "SECRETARY", "ADMIN")
  @ApiOperation({ summary: "Remover publicação", description: "Remoção lógica: o post fica inativo." })
  @ApiResponse({ status: 204, description: "Publicação removida" })
  @ApiResponse({ status: 403, description: "Apenas o autor ou um gestor pode modificar" })
  @ApiResponse({ status: 404, description: "Publicação não encontrada" })
  async remover(@Param("id") id: string, @Usuario() usuario: UsuarioAutenticado): Promise<void> {
    await this.mural.remover(interpretarId(id, MENSAGEM_ID), usuario);
  }
}
