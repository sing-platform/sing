import base64
import unittest
from pathlib import Path

from app.comprovante import processar


EVENTO = {
    "eventId": "11111111-1111-4111-8111-111111111111",
    "eventType": "matricula.criada",
    "version": 1,
    "occurredAt": "2026-11-05T18:30:00Z",
    "payload": {
        "matriculaId": 9001,
        "data": "2026-11-05",
        "aluno": {"id": 501, "nome": "Aluno Exemplo Fictício"},
        "turma": {"id": 77, "nome": "Turma Exemplo Manhã"},
        "oficina": {"id": 12, "nome": "Oficina Exemplo de Teatro"},
    },
}


class ColecaoMemoria:
    def __init__(self):
        self.docs = []

    def find_one(self, filtro):
        for doc in self.docs:
            if doc.get("eventId") == filtro.get("eventId"):
                return doc
        return None

    def insert_one(self, doc):
        if self.find_one({"eventId": doc["eventId"]}):
            raise type("DuplicateKeyError", (Exception,), {})("duplicado")
        self.docs.append(doc)


class ComprovanteTest(unittest.TestCase):
    def test_grava_pdf_com_os_campos_do_contrato(self):
        colecao = ColecaoMemoria()
        self.assertEqual(processar(EVENTO, colecao), "gravado")
        doc = colecao.docs[0]
        self.assertEqual(doc["nomeArquivo"], "comprovante-matricula-9001.pdf")
        self.assertEqual(doc["mimeType"], "application/pdf")
        self.assertEqual(doc["matriculaId"], 9001)
        self.assertEqual(doc["aluno"]["nome"], "Aluno Exemplo Fictício")
        pdf = base64.b64decode(doc["arquivoBase64"])
        self.assertEqual(doc["tamanhoBytes"], len(pdf))
        self.assertTrue(pdf.startswith(b"%PDF-"))

    def test_reentrega_nao_cria_segundo_comprovante(self):
        colecao = ColecaoMemoria()
        self.assertEqual(processar(EVENTO, colecao), "gravado")
        self.assertEqual(processar(EVENTO, colecao), "duplicado")
        self.assertEqual(len(colecao.docs), 1)

    def test_payload_incompleto_fica_rejeitado_sem_segundo_registro(self):
        colecao = ColecaoMemoria()
        ruim = {
            "eventId": EVENTO["eventId"],
            "eventType": "matricula.criada",
            "version": 1,
            "occurredAt": "2026-11-05T18:30:00Z",
            "payload": {"matriculaId": 9001},
        }
        self.assertEqual(processar(ruim, colecao), "rejeitado")
        self.assertEqual(colecao.docs[0]["tipo"], "rejeicao")
        self.assertNotIn("arquivoBase64", colecao.docs[0])
        self.assertEqual(processar(ruim, colecao), "duplicado")
        self.assertEqual(len(colecao.docs), 1)

    def test_pdf_acima_do_teto_nao_grava(self):
        colecao = ColecaoMemoria()
        self.assertEqual(processar(EVENTO, colecao, max_bytes=10), "ignorado")
        self.assertEqual(colecao.docs, [])

    def test_recorte_nao_chama_o_core(self):
        raiz = Path(__file__).resolve().parents[1] / "app"
        for nome in ("comprovante.py", "consumidor_matricula.py"):
            texto = (raiz / nome).read_text(encoding="utf-8")
            self.assertNotIn("core_client", texto)
            self.assertNotIn("httpx", texto)
            self.assertNotIn("requests", texto)


if __name__ == "__main__":
    unittest.main()
