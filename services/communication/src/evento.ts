export type Resultado = "gravada" | "duplicada" | "ignorada";

export interface Memoria {
  buscar(eventId: string): Promise<Record<string, unknown> | null>;
  inserir(doc: Record<string, unknown>): Promise<void>;
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const DATA = /^\d{4}-\d{2}-\d{2}$/;
const INSTANTE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/;

export async function aplicarEvento(evento: unknown, memoria: Memoria): Promise<Resultado> {
  const doc = montar(evento);
  if (!doc) {
    return "ignorada";
  }
  const eventId = String(doc.eventId);
  const existente = await memoria.buscar(eventId);
  if (existente) {
    return "duplicada";
  }
  try {
    await memoria.inserir(doc);
    return "gravada";
  } catch (erro) {
    if (codigoDuplicado(erro)) {
      return "duplicada";
    }
    throw erro;
  }
}

export function montar(evento: unknown): Record<string, unknown> | null {
  if (!evento || typeof evento !== "object") {
    return null;
  }
  const raiz = evento as Record<string, unknown>;
  if (typeof raiz.eventId !== "string" || !UUID.test(raiz.eventId)) {
    return null;
  }
  if (raiz.eventType !== "matricula.criada" || raiz.version !== 1) {
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
  if (!numero(dados.matriculaId) || typeof dados.data !== "string" || !DATA.test(dados.data)) {
    return null;
  }
  const aluno = bloco(dados.aluno);
  const turma = bloco(dados.turma);
  const oficina = bloco(dados.oficina);
  if (!aluno || !turma || !oficina) {
    return null;
  }
  return {
    eventId: raiz.eventId,
    matriculaId: dados.matriculaId,
    data: dados.data,
    aluno,
    turma,
    oficina,
    occurredAt: raiz.occurredAt,
    mensagem: `Matrícula criada para ${aluno.nome} na turma ${turma.nome}.`,
  };
}

function bloco(valor: unknown): { id: number; nome: string } | null {
  if (!valor || typeof valor !== "object") {
    return null;
  }
  const item = valor as Record<string, unknown>;
  if (!numero(item.id) || typeof item.nome !== "string" || item.nome.trim() === "") {
    return null;
  }
  return { id: item.id as number, nome: item.nome };
}

function numero(valor: unknown): boolean {
  return typeof valor === "number" && Number.isInteger(valor);
}

function codigoDuplicado(erro: unknown): boolean {
  return typeof erro === "object" && erro !== null && (erro as { code?: number }).code === 11000;
}
