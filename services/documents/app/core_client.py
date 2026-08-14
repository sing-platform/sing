"""Chamadas sincronas ao core.

Usado apenas para o que este servico nao tem como saber sozinho: o vinculo
responsavel-aluno, que vive no dominio `people`. E leitura, tolera indisponibilidade
e nao entra em transacao — o inverso (escrita cross-service) seria inaceitavel.
"""
import logging

import httpx

from .config import settings

log = logging.getLogger(__name__)


def is_guardian_of(guardian_person_id: int, student_person_id: int, bearer: str) -> bool:
    """Confirma que o solicitante e responsavel pelo aluno.

    Em caso de indisponibilidade do core, nega o acesso: numa verificacao de
    permissao o padrao seguro e negar, nao liberar.
    """
    url = (f"{settings.core_base_url}/api/v1/internal/guardianship"
           f"/{student_person_id}/{guardian_person_id}")
    try:
        resp = httpx.get(
            url,
            headers={"Authorization": bearer},
            timeout=settings.core_timeout_seconds,
        )
        if resp.status_code != 200:
            log.warning("core respondeu %s em %s", resp.status_code, url)
            return False
        return bool((resp.json() or {}).get("isGuardian", False))
    except Exception:
        log.warning("core indisponivel ao checar vinculo de responsavel", exc_info=True)
        return False
