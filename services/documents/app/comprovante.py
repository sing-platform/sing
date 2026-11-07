"""Comprovante da matrícula criada.

O PDF sai só do payload do evento. Este módulo não chama o core.
"""
import base64
import re

EVENTO = "matricula.criada"
MIME = "application/pdf"
LIMITE_BSON = 16 * 1024 * 1024

_UUID = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
)
_DATA = re.compile(r"^\d{4}-\d{2}-\d{2}$")
_INSTANTE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


def processar(evento, colecao, max_bytes=1_048_576) -> str:
    """Grava um comprovante ou uma rejeição. A segunda cópia do mesmo eventId não grava de novo."""
    if not isinstance(evento, dict):
        return "ignorado"
    event_id = evento.get("eventId")
    if not isinstance(event_id, str) or not _UUID.match(event_id):
        return "ignorado"
    if colecao.find_one({"eventId": event_id}):
        return "duplicado"

    motivo = _motivo(evento)
    if motivo:
        documento = {"eventId": event_id, "tipo": "rejeicao", "motivo": motivo}
        return _inserir(colecao, documento, "rejeitado")

    pdf = pdf_comprovante(evento)
    arquivo = base64.b64encode(pdf).decode("ascii")
    if len(pdf) > max_bytes or len(arquivo) + 4096 > LIMITE_BSON:
        return "ignorado"

    payload = evento["payload"]
    matricula_id = payload["matriculaId"]
    documento = {
        "eventId": event_id,
        "tipo": "comprovante",
        "matriculaId": matricula_id,
        "data": payload["data"],
        "aluno": payload["aluno"],
        "turma": payload["turma"],
        "oficina": payload["oficina"],
        "occurredAt": evento["occurredAt"],
        "mimeType": MIME,
        "tamanhoBytes": len(pdf),
        "arquivoBase64": arquivo,
        "nomeArquivo": f"comprovante-matricula-{matricula_id}.pdf",
    }
    return _inserir(colecao, documento, "gravado")


def pdf_comprovante(evento: dict) -> bytes:
    payload = evento["payload"]
    linhas = [
        f"Comprovante de matricula {payload['matriculaId']}",
        f"Aluno: {payload['aluno']['nome']}",
        f"Turma: {payload['turma']['nome']}",
        f"Oficina: {payload['oficina']['nome']}",
        f"Data: {payload['data']}",
    ]
    return _pdf(linhas)


def _inserir(colecao, documento: dict, quando_ok: str) -> str:
    try:
        colecao.insert_one(documento)
    except Exception as exc:
        if exc.__class__.__name__ == "DuplicateKeyError":
            return "duplicado"
        raise
    return quando_ok


def _motivo(evento: dict) -> str | None:
    if evento.get("eventType") != EVENTO:
        return "eventType invalido"
    if evento.get("version") != 1:
        return "version invalida"
    if not isinstance(evento.get("occurredAt"), str) or not _INSTANTE.match(evento["occurredAt"]):
        return "occurredAt invalido"
    payload = evento.get("payload")
    if not isinstance(payload, dict):
        return "payload ausente"
    if not _numero(payload.get("matriculaId")):
        return "matriculaId invalido"
    if not isinstance(payload.get("data"), str) or not _DATA.match(payload["data"]):
        return "data invalida"
    for nome in ("aluno", "turma", "oficina"):
        erro = _bloco(payload.get(nome))
        if erro:
            return f"{nome} {erro}"
    return None


def _bloco(valor) -> str | None:
    if not isinstance(valor, dict):
        return "ausente"
    if not _numero(valor.get("id")):
        return "sem id"
    nome = valor.get("nome")
    if not isinstance(nome, str) or not nome.strip():
        return "sem nome"
    return None


def _numero(valor) -> bool:
    return isinstance(valor, int) and not isinstance(valor, bool)


def _pdf(linhas: list[str]) -> bytes:
    def escapar(texto: str) -> str:
        return texto.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

    comandos = ["BT", "/F1 12 Tf", "50 760 Td"]
    for indice, linha in enumerate(linhas):
        if indice:
            comandos.append("0 -16 Td")
        comandos.append(f"({escapar(linha)}) Tj")
    comandos.append("ET")
    stream = "\n".join(comandos).encode("latin-1", errors="replace")

    objetos = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        (
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
            b"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>"
        ),
        b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    ]

    saida = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for numero, corpo in enumerate(objetos, start=1):
        offsets.append(len(saida))
        saida.extend(f"{numero} 0 obj\n".encode("ascii"))
        saida.extend(corpo)
        saida.extend(b"\nendobj\n")
    xref = len(saida)
    saida.extend(f"xref\n0 {len(offsets)}\n".encode("ascii"))
    saida.extend(b"0000000000 65535 f \n")
    for offset in offsets[1:]:
        saida.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
    saida.extend(
        f"trailer<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF".encode("ascii")
    )
    return bytes(saida)
