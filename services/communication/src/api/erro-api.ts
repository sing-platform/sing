import { ArgumentsHost, Catch, ExceptionFilter, HttpException, HttpStatus } from "@nestjs/common";
import type { Request, Response } from "express";

export interface CampoInvalido {
  field: string;
  rejectedValue: unknown;
  message: string;
}

export interface CorpoErro {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  path: string;
  details: null;
  fields?: CampoInvalido[];
}

const FRASES: Record<number, string> = {
  400: "Bad Request",
  401: "Unauthorized",
  403: "Forbidden",
  404: "Not Found",
  405: "Method Not Allowed",
  500: "Internal Server Error",
  503: "Service Unavailable",
};

export class ErroApi extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly campos?: CampoInvalido[],
  ) {
    super(message);
  }
}

export function naoAutenticado(): ErroApi {
  return new ErroApi(401, "Authentication required");
}

export function semPermissao(): ErroApi {
  return new ErroApi(403, "Você não tem permissão para realizar esta ação.");
}

export function naoEncontrado(mensagem: string): ErroApi {
  return new ErroApi(404, mensagem);
}

export function requisicaoInvalida(mensagem: string): ErroApi {
  return new ErroApi(400, mensagem);
}

export function falhaDeValidacao(campos: CampoInvalido[]): ErroApi {
  return new ErroApi(400, "One or more fields are invalid", campos);
}

export function montarCorpo(erro: ErroApi, caminho: string, agora: Date = new Date()): CorpoErro {
  const corpo: CorpoErro = {
    status: erro.status,
    error: erro.campos ? "Validation Failed" : (FRASES[erro.status] ?? "Error"),
    message: erro.message,
    timestamp: agora.toISOString(),
    path: caminho,
    details: null,
  };
  if (erro.campos) {
    corpo.fields = erro.campos;
  }
  return corpo;
}

export function traduzir(excecao: unknown): ErroApi {
  if (excecao instanceof ErroApi) {
    return excecao;
  }
  if (excecao instanceof HttpException) {
    const status = excecao.getStatus();
    if (status === HttpStatus.BAD_REQUEST) {
      return new ErroApi(400, "Malformed JSON request");
    }
    return new ErroApi(status, excecao.message);
  }
  console.error("erro inesperado", excecao);
  return new ErroApi(500, "An unexpected error occurred. Please try again later.");
}

@Catch()
export class FiltroErroApi implements ExceptionFilter {
  catch(excecao: unknown, host: ArgumentsHost): void {
    const contexto = host.switchToHttp();
    const req = contexto.getRequest<Request>();
    const res = contexto.getResponse<Response>();
    const erro = traduzir(excecao);
    res.status(erro.status).json(montarCorpo(erro, req.originalUrl.split("?")[0]));
  }
}
