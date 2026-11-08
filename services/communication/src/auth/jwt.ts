import { createHmac, timingSafeEqual } from "node:crypto";

export interface Credenciais {
  email: string;
  role: string | null;
  userId: number | null;
  personId: number | null;
}

export class TokenInvalido extends Error {}

const ALGORITMOS: Record<string, string> = {
  HS256: "sha256",
  HS384: "sha384",
  HS512: "sha512",
};

const BASE64_PADRAO = /^[A-Za-z0-9+/]*={0,2}$/;

export function derivarChave(segredo: string | undefined): Buffer | null {
  if (!segredo) {
    return null;
  }
  if (base64Estrito(segredo)) {
    const bytes = Buffer.from(segredo, "base64");
    return bytes.length > 0 ? bytes : null;
  }
  return Buffer.from(segredo, "utf8");
}

function base64Estrito(texto: string): boolean {
  if (!BASE64_PADRAO.test(texto)) {
    return false;
  }
  const semPreenchimento = texto.replace(/=+$/, "");
  const preenchimento = texto.length - semPreenchimento.length;
  if (preenchimento > 0) {
    return texto.length % 4 === 0;
  }
  return semPreenchimento.length % 4 !== 1;
}

export function verificarToken(
  token: string,
  chave: Buffer | null,
  agoraEmSegundos: number = Math.floor(Date.now() / 1000),
): Credenciais {
  if (!chave || chave.length === 0) {
    throw new TokenInvalido("sem chave");
  }
  const partes = token.split(".");
  if (partes.length !== 3 || partes.some((parte) => parte === "")) {
    throw new TokenInvalido("formato");
  }
  const [cabecalhoB64, corpoB64, assinaturaB64] = partes;
  const cabecalho = lerJson(cabecalhoB64);
  const algoritmo = typeof cabecalho.alg === "string" ? ALGORITMOS[cabecalho.alg] : undefined;
  if (!algoritmo) {
    throw new TokenInvalido("algoritmo");
  }
  const esperada = createHmac(algoritmo, chave).update(`${cabecalhoB64}.${corpoB64}`).digest();
  const recebida = Buffer.from(assinaturaB64, "base64url");
  if (recebida.length !== esperada.length || !timingSafeEqual(recebida, esperada)) {
    throw new TokenInvalido("assinatura");
  }
  const claims = lerJson(corpoB64);
  if (typeof claims.exp !== "number" || claims.exp <= agoraEmSegundos) {
    throw new TokenInvalido("expirado");
  }
  if (typeof claims.nbf === "number" && claims.nbf > agoraEmSegundos) {
    throw new TokenInvalido("ainda nao valido");
  }
  if (typeof claims.sub !== "string" || claims.sub === "") {
    throw new TokenInvalido("sem sub");
  }
  return {
    email: claims.sub,
    role: typeof claims.role === "string" ? claims.role : null,
    userId: inteiro(claims.uid),
    personId: inteiro(claims.pid),
  };
}

function lerJson(parteB64: string): Record<string, unknown> {
  let valor: unknown;
  try {
    valor = JSON.parse(Buffer.from(parteB64, "base64url").toString("utf8"));
  } catch {
    throw new TokenInvalido("json");
  }
  if (valor && typeof valor === "object" && !Array.isArray(valor)) {
    return valor as Record<string, unknown>;
  }
  throw new TokenInvalido("json");
}

function inteiro(valor: unknown): number | null {
  return typeof valor === "number" && Number.isInteger(valor) ? valor : null;
}
