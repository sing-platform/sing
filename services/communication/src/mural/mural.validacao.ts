import { CampoInvalido, ErroApi, falhaDeValidacao, requisicaoInvalida } from "../api/erro-api";
import { CorpoPublicacao } from "./mural.tipos";

const LIMITE_TITULO = 255;
const CORPO_MALFORMADO = "Malformed JSON request";

export function validarPublicacao(corpo: unknown): CorpoPublicacao {
  if (!corpo || typeof corpo !== "object" || Array.isArray(corpo)) {
    throw requisicaoInvalida(CORPO_MALFORMADO);
  }
  const dados = corpo as Record<string, unknown>;
  const audienceRoles = listaDeTextos(dados.audienceRoles);
  const audienceSchoolroomIds = listaDeInteiros(dados.audienceSchoolroomIds);
  const title = dados.title;
  const content = dados.content;
  const campos: CampoInvalido[] = [];
  if (typeof title !== "string" || title.trim() === "") {
    campos.push({ field: "title", rejectedValue: title ?? null, message: "Título é obrigatório" });
  } else if (title.length > LIMITE_TITULO) {
    campos.push({ field: "title", rejectedValue: title, message: "Título deve ter no máximo 255 caracteres" });
  }
  if (typeof content !== "string" || content.trim() === "") {
    campos.push({ field: "content", rejectedValue: content ?? null, message: "Conteúdo é obrigatório" });
  }
  if (campos.length > 0) {
    throw falhaDeValidacao(campos);
  }
  return {
    title: (title as string).trim(),
    content: content as string,
    audienceRoles,
    audienceSchoolroomIds,
  };
}

function listaDeTextos(valor: unknown): string[] {
  const itens = listaOuVazia(valor);
  if (!itens.every((item) => typeof item === "string")) {
    throw requisicaoInvalida(CORPO_MALFORMADO);
  }
  return [...new Set(itens as string[])];
}

function listaDeInteiros(valor: unknown): number[] {
  const itens = listaOuVazia(valor);
  if (!itens.every((item) => typeof item === "number" && Number.isInteger(item))) {
    throw requisicaoInvalida(CORPO_MALFORMADO);
  }
  return [...new Set(itens as number[])];
}

function listaOuVazia(valor: unknown): unknown[] {
  if (valor === undefined || valor === null) {
    return [];
  }
  if (!Array.isArray(valor)) {
    throw requisicaoInvalida(CORPO_MALFORMADO);
  }
  return valor;
}

export function interpretarId(texto: string, mensagem: string): number {
  if (!/^\d{1,15}$/.test(texto)) {
    throw new ErroApi(400, mensagem);
  }
  return Number(texto);
}

const DATA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/;

export function interpretarData(texto: unknown, nome: string): Date {
  const mensagem = `Parâmetro ${nome} é obrigatório no formato yyyy-MM-dd`;
  if (typeof texto !== "string") {
    throw requisicaoInvalida(mensagem);
  }
  const partes = DATA_ISO.exec(texto);
  if (!partes) {
    throw requisicaoInvalida(mensagem);
  }
  const data = new Date(Date.UTC(Number(partes[1]), Number(partes[2]) - 1, Number(partes[3])));
  if (data.toISOString().slice(0, 10) !== texto) {
    throw requisicaoInvalida(mensagem);
  }
  return data;
}
