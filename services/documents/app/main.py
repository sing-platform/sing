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
    yield


app = FastAPI(
    title="SING Documents",
    description="Documentos, atestados e fluxo de aprovacao.",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(documents.router)


@app.get("/actuator/health", tags=["ops"], summary="Health check")
def health():
    """Mesmo caminho do Actuator do core, para o compose e o Prometheus
    tratarem todos os servicos da stack de forma uniforme."""
    checks = {"db": "DOWN", "storage": "DOWN"}
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

    overall = "UP" if all(v == "UP" for v in checks.values()) else "DOWN"
    return {"status": overall, "components": checks}
