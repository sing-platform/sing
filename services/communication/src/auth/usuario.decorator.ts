import { createParamDecorator, ExecutionContext } from "@nestjs/common";
import { RequisicaoAutenticada, UsuarioAutenticado } from "./auth.guard";

export const Usuario = createParamDecorator((_dados: unknown, contexto: ExecutionContext): UsuarioAutenticado => {
  const req = contexto.switchToHttp().getRequest<RequisicaoAutenticada>();
  return req.usuario as UsuarioAutenticado;
});
