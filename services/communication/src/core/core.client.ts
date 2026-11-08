import { Injectable } from "@nestjs/common";
import { ErroApi } from "../api/erro-api";

export interface AutorResolvido {
  userId: number;
  name: string;
  email: string;
  role: string;
}

export interface DestinatarioResolvido {
  userId: number;
  email: string;
}

export interface DestinatariosResolvidos {
  author: AutorResolvido;
  recipients: DestinatarioResolvido[];
}

export interface PublicoAlvo {
  audienceRoles: string[];
  audienceSchoolroomIds: number[];
}

export abstract class ClienteCore {
  abstract resolverDestinatarios(authorization: string, publico: PublicoAlvo): Promise<DestinatariosResolvidos>;
}

const TIMEOUT_MS = 5000;
const MENSAGEM_INDISPONIVEL = "Serviço de usuários indisponível no momento. Tente novamente em instantes.";

@Injectable()
export class ClienteCoreHttp extends ClienteCore {
  async resolverDestinatarios(authorization: string, publico: PublicoAlvo): Promise<DestinatariosResolvidos> {
    const base = (process.env.CORE_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");
    let resposta: Response;
    try {
      resposta = await fetch(`${base}/api/v1/internal/mural/destinatarios`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Accept: "application/json", Authorization: authorization },
        body: JSON.stringify(publico),
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
    } catch {
      throw new ErroApi(503, MENSAGEM_INDISPONIVEL);
    }
    if (resposta.status === 400 || resposta.status === 403) {
      throw new ErroApi(resposta.status, await mensagemDe(resposta));
    }
    if (!resposta.ok) {
      throw new ErroApi(503, MENSAGEM_INDISPONIVEL);
    }
    try {
      return lerResposta(await resposta.json());
    } catch {
      throw new ErroApi(503, MENSAGEM_INDISPONIVEL);
    }
  }
}

async function mensagemDe(resposta: Response): Promise<string> {
  try {
    const corpo: unknown = await resposta.json();
    if (corpo && typeof corpo === "object" && typeof (corpo as { message?: unknown }).message === "string") {
      return (corpo as { message: string }).message;
    }
  } catch {
    return resposta.status === 403 ? "Acesso negado" : "Requisição inválida";
  }
  return resposta.status === 403 ? "Acesso negado" : "Requisição inválida";
}

export function lerResposta(corpo: unknown): DestinatariosResolvidos {
  const raiz = corpo as { author?: Record<string, unknown>; recipients?: unknown };
  const autor = raiz.author;
  if (!autor || typeof autor.userId !== "number" || typeof autor.email !== "string" || !Array.isArray(raiz.recipients)) {
    throw new Error("resposta do core fora do contrato");
  }
  return {
    author: {
      userId: autor.userId,
      name: typeof autor.name === "string" ? autor.name : "",
      email: autor.email,
      role: typeof autor.role === "string" ? autor.role : "",
    },
    recipients: raiz.recipients.map((item) => {
      const destinatario = item as Record<string, unknown>;
      if (typeof destinatario.userId !== "number" || typeof destinatario.email !== "string") {
        throw new Error("destinatario fora do contrato");
      }
      return { userId: destinatario.userId, email: destinatario.email };
    }),
  };
}
