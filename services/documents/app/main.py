"""sing-documents — servico de documentos (upload, atestados, aprovacao).

Extraido do modulo `document` do monolito. Foi o primeiro candidato porque ja
usava IDs em vez de relacionamento ORM com as outras tabelas, o storage ja era
externo e a unica ponte com o dominio academico estava isolada numa classe.
"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from sqlalchemy import text

from . import storage
from .consumidor_matricula import ConsumidorMatricula, mongo_comprovantes_ok
from .database import engine
from .routers import documents

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)-5s %(name)s: %(message)s")
log = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        storage.ensure_bucket()
    except Exception:
        # nao impede o servico de subir: leitura de metadados segue funcionando
        log.warning("storage indisponivel no boot", exc_info=True)
    consumidor = ConsumidorMatricula()
    consumidor.start()
    try:
        yield
    finally:
        consumidor.parar()


app = FastAPI(
    title="SING Documents",
    description=(
        "Documentos, atestados e fluxo de aprovacao. "
        "Esta UI e publica. As rotas /api/documents pedem o JWT do core."
    ),
    version="1.0.0",
    lifespan=lifespan,
    # Fora de /api/documents: esse prefixo tem /{document_id} e um GET em
    # /docs ou /openapi.json vira 405 em vez da documentacao.
    docs_url="/documents/docs",
    openapi_url="/documents/openapi.json",
    redoc_url=None,
    servers=[{"url": "/", "description": "Gateway (Traefik ou Ingress)"}],
)

app.include_router(documents.router)


@app.get("/actuator/health", tags=["ops"], summary="Health check")
def health():
    """Mesmo caminho do Actuator do core, para o compose e o Prometheus
    tratarem todos os servicos da stack de forma uniforme."""
    checks = {"db": "DOWN", "storage": "DOWN", "comprovantes": "DOWN"}
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        checks["db"] = "UP"
    except Exception:
        log.warning("health: banco inacessivel", exc_info=True)
    try:
        storage.ensure_bucket()
        checks["storage"] = "UP"
    except Exception:
        log.warning("health: storage inacessivel", exc_info=True)
    checks["comprovantes"] = "UP" if mongo_comprovantes_ok() else "DOWN"

    # O comprovante e outro banco. Atestados seguem no ar se o Mongo desse recorte cair.
    overall = "UP" if checks["db"] == "UP" and checks["storage"] == "UP" else "DOWN"
    return {"status": overall, "components": checks}
