import { CanActivate, ExecutionContext, Injectable, SetMetadata } from "@nestjs/common";
import { Reflector } from "@nestjs/core";
import type { Request } from "express";
import { naoAutenticado, semPermissao } from "../api/erro-api";
import { Credenciais, derivarChave, TokenInvalido, verificarToken } from "./jwt";

const CHAVE_PAPEIS = "sing:papeis";

export const Papeis = (...papeis: string[]) =>
  SetMetadata(
    CHAVE_PAPEIS,
    papeis.map((papel) => `ROLE_${papel}`),
  );

export interface UsuarioAutenticado extends Credenciais {
  userId: number;
  authorization: string;
}

export interface RequisicaoAutenticada extends Request {
  usuario?: UsuarioAutenticado;
}

@Injectable()
export class JwtAuthGuard implements CanActivate {
  private readonly chave: Buffer | null;

  constructor(private readonly reflector: Reflector) {
    this.chave = derivarChave(process.env.JWT_SECRET);
  }

  canActivate(contexto: ExecutionContext): boolean {
    const req = contexto.switchToHttp().getRequest<RequisicaoAutenticada>();
    const cabecalho = req.headers.authorization;
    if (typeof cabecalho !== "string" || !/^Bearer\s+\S+$/i.test(cabecalho)) {
      throw naoAutenticado();
    }
    const token = cabecalho.replace(/^Bearer\s+/i, "");
    let credenciais: Credenciais;
    try {
      credenciais = verificarToken(token, this.chave);
    } catch (erro) {
      if (erro instanceof TokenInvalido) {
        throw naoAutenticado();
      }
      throw erro;
    }
    if (credenciais.userId === null) {
      throw naoAutenticado();
    }
    const exigidos = this.reflector.getAllAndOverride<string[] | undefined>(CHAVE_PAPEIS, [
      contexto.getHandler(),
      contexto.getClass(),
    ]);
    if (exigidos && exigidos.length > 0 && (!credenciais.role || !exigidos.includes(credenciais.role))) {
      throw semPermissao();
    }
    req.usuario = { ...credenciais, userId: credenciais.userId, authorization: cabecalho };
    return true;
  }
}
