import { SolicitacaoNotificacao } from "./notificacao-usuario.tipos";

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const INSTANTE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z$/;

export const TIPO_EVENTO_SOLICITACAO = "notificacao.solicitada";

export function lerSolicitacao(evento: unknown): SolicitacaoNotificacao | null {
  if (!evento || typeof evento !== "object") {
    return null;
  }
  const raiz = evento as Record<string, unknown>;
  if (typeof raiz.eventId !== "string" || !UUID.test(raiz.eventId)) {
    return null;
  }
  if (raiz.eventType !== TIPO_EVENTO_SOLICITACAO || raiz.version !== 1) {
    return null;
  }
  if (typeof raiz.occurredAt !== "string" || !INSTANTE.test(raiz.occurredAt)) {
    return null;
  }
  const payload = raiz.payload;
  if (!payload || typeof payload !== "object") {
    return null;
  }
  const dados = payload as Record<string, unknown>;
  const { type, title, message, referenceKey, actionUrl, recipientUserIds } = dados;
  if (!textoPreenchido(type) || !textoPreenchido(title) || !textoPreenchido(message) || !textoPreenchido(referenceKey)) {
    return null;
  }
  if (actionUrl !== undefined && actionUrl !== null && typeof actionUrl !== "string") {
    return null;
  }
  if (!Array.isArray(recipientUserIds) || !recipientUserIds.every(inteiro)) {
    return null;
  }
  return {
    eventId: raiz.eventId,
    type: type as string,
    title: title as string,
    message: message as string,
    actionUrl: (actionUrl as string | null | undefined) ?? null,
    referenceKey: referenceKey as string,
    recipientUserIds: [...new Set(recipientUserIds as number[])],
  };
}

function textoPreenchido(valor: unknown): boolean {
  return typeof valor === "string" && valor.trim() !== "";
}

function inteiro(valor: unknown): boolean {
  return typeof valor === "number" && Number.isInteger(valor);
}
