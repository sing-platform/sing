"""Regras de negocio do servico de documentos.

Porta o comportamento de DocumentService.java e AtestadoApprovalService.java,
com uma diferenca central: aprovar um atestado nao escreve mais em `attendance`
(tabela do core) — publica um evento. Ver events.py.
"""
import logging
from datetime import date, datetime, timezone

import magic
from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from . import events, storage
from .config import settings
from .enums import (
    ATESTADO_CATEGORIES,
    SELF_UPLOAD_CATEGORIES,
    THERAPEUTIC_CATEGORIES,
    DocumentCategory,
    DocumentScope,
    DocumentStatus,
)
from .models import UserDocument
from .security import CurrentUser

log = logging.getLogger(__name__)


# ── Upload ───────────────────────────────────────────────────────────────────

def detect_and_validate_mime(content: bytes, filename: str | None) -> str:
    """Detecta o MIME pelos magic bytes, nao pelo header do cliente.

    O `content-type` do multipart e informado por quem envia e pode ser
    forjado; ler os primeiros bytes do arquivo nao. Equivale ao Apache Tika
    usado no monolito (aqui, libmagic via python-magic).
    """
    if not content:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, "Arquivo vazio.")
    if len(content) > settings.max_file_size_bytes:
        raise HTTPException(
            status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            f"Arquivo excede {settings.max_file_size_mb} MB.",
        )
    detected = magic.from_buffer(content, mime=True)
    if detected not in settings.allowed_mimes:
        raise HTTPException(
            status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            f"Tipo de arquivo nao suportado: {detected}",
        )
    return detected


def create_document(
    db: Session,
    *,
    owner_person_id: int,
    uploaded_by_user_id: int,
    content: bytes,
    filename: str | None,
    category: DocumentCategory,
    scope: DocumentScope,
    description: str | None,
    reference_date_start: date | None = None,
    reference_date_end: date | None = None,
) -> UserDocument:
    mime = detect_and_validate_mime(content, filename)
    storage_key = storage.build_storage_key(owner_person_id, filename)

    storage.upload(content, storage_key, mime)

    doc = UserDocument(
        owner_person_id=owner_person_id,
        file_name=storage_key,
        original_file_name=filename or storage_key,
        mime_type=mime,
        file_size=len(content),
        category=category.value,
        scope=scope.value,
        storage_key=storage_key,
        uploaded_by_user_id=uploaded_by_user_id,
        uploaded_at=datetime.now(timezone.utc),
        description=description,
        active=True,
        status=DocumentStatus.PENDING.value,
        # datas declaradas por quem envia; a secretaria pode corrigir ao aprovar
        reference_date_start=reference_date_start,
        reference_date_end=reference_date_end,
    )
    try:
        db.add(doc)
        db.commit()
        db.refresh(doc)
        return doc
    except Exception:
        # acao compensatoria: sem isto o objeto ficaria orfao no storage
        db.rollback()
        storage.delete(storage_key)
        raise


def assert_category_allowed(category: DocumentCategory, allowed: frozenset) -> None:
    if category not in allowed:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            f"Categoria {category.value} nao permitida aqui. "
            f"Permitidas: {sorted(c.value for c in allowed)}",
        )


# ── Consulta ─────────────────────────────────────────────────────────────────

def get_active_or_404(db: Session, document_id: int) -> UserDocument:
    doc = db.get(UserDocument, document_id)
    if doc is None or not doc.active:
        raise HTTPException(status.HTTP_404_NOT_FOUND, f"Documento {document_id} nao encontrado.")
    return doc


def list_by_person(db: Session, owner_person_id: int) -> list[UserDocument]:
    """Lista documentos da pessoa, exceto anotacoes terapeuticas."""
    stmt = (
        select(UserDocument)
        .where(
            UserDocument.owner_person_id == owner_person_id,
            UserDocument.active.is_(True),
            UserDocument.scope != DocumentScope.THERAPEUTIC_PRIVATE.value,
        )
        .order_by(UserDocument.uploaded_at.desc())
    )
    return list(db.scalars(stmt))


def list_therapeutic(db: Session, patient_person_id: int, psychologist_user_id: int):
    stmt = (
        select(UserDocument)
        .where(
            UserDocument.owner_person_id == patient_person_id,
            UserDocument.uploaded_by_user_id == psychologist_user_id,
            UserDocument.scope == DocumentScope.THERAPEUTIC_PRIVATE.value,
            UserDocument.active.is_(True),
        )
        .order_by(UserDocument.uploaded_at.desc())
    )
    return list(db.scalars(stmt))


def list_atestados(db: Session, *, only_pending: bool) -> list[UserDocument]:
    categories = [c.value for c in ATESTADO_CATEGORIES]
    stmt = select(UserDocument).where(
        UserDocument.category.in_(categories),
        UserDocument.active.is_(True),
    )
    if only_pending:
        stmt = stmt.where(UserDocument.status == DocumentStatus.PENDING.value)
        stmt = stmt.order_by(UserDocument.uploaded_at.asc())
    else:
        stmt = stmt.order_by(UserDocument.uploaded_at.desc())
    return list(db.scalars(stmt))


def find_covering_approved(db: Session, owner_person_id: int, day: date) -> int | None:
    """Atestado aprovado que cobre a data — consultado pelo core ao lancar falta."""
    stmt = (
        select(UserDocument.id)
        .where(
            UserDocument.owner_person_id == owner_person_id,
            UserDocument.active.is_(True),
            UserDocument.status == DocumentStatus.APPROVED.value,
            UserDocument.category.in_([c.value for c in ATESTADO_CATEGORIES]),
            UserDocument.reference_date_start <= day,
            UserDocument.reference_date_end >= day,
        )
        .order_by(UserDocument.id)
        .limit(1)
    )
    return db.scalars(stmt).first()


# ── Controle de acesso ───────────────────────────────────────────────────────

def assert_can_access(doc: UserDocument, user: CurrentUser) -> None:
    """Espelha DocumentService.assertAccess.

    Compara o dono por person_id e o autor por user_id — sao identificadores
    distintos, e trata-los como um so era a origem de uma falha de acesso no
    monolito.
    """
    if doc.is_therapeutic:
        if doc.uploaded_by_user_id != user.user_id:
            raise HTTPException(
                status.HTTP_403_FORBIDDEN,
                "Acesso negado: anotacao terapeutica pertence a outro psicologo.",
            )
        return

    if user.is_admin_or_secretary:
        return

    if doc.owner_person_id != user.person_id:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN,
            "Acesso negado: documento pertence a outro usuario.",
        )


# ── Fluxo de aprovacao ───────────────────────────────────────────────────────

def assert_is_atestado(doc: UserDocument) -> None:
    if not doc.is_atestado:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            f"Documento {doc.id} nao e atestado nem justificativa de falta.",
        )


def approve(db: Session, document_id: int, reviewer_user_id: int,
            start: date, end: date) -> UserDocument:
    """Aprova e pede ao core que justifique as faltas do periodo.

    Idempotente: reaprovar com outro periodo revoga o anterior antes, para o
    core nao manter justificativa de datas que sairam do intervalo.
    """
    doc = get_active_or_404(db, document_id)
    assert_is_atestado(doc)

    was_approved = doc.status == DocumentStatus.APPROVED.value

    try:
        doc.approve(reviewer_user_id, start, end)
    except ValueError as exc:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, str(exc)) from exc

    db.commit()
    db.refresh(doc)

    if was_approved:
        events.publish(events.EVENT_ATESTADO_REVOKED, doc.id, {"documentId": doc.id})

    events.publish(events.EVENT_ATESTADO_APPROVED, doc.id, {
        "documentId": doc.id,
        "ownerPersonId": doc.owner_person_id,
        "referenceDateStart": doc.reference_date_start,
        "referenceDateEnd": doc.reference_date_end,
    })
    return doc


def reject(db: Session, document_id: int, reviewer_user_id: int, reason: str) -> UserDocument:
    doc = get_active_or_404(db, document_id)
    assert_is_atestado(doc)

    was_approved = doc.status == DocumentStatus.APPROVED.value

    try:
        doc.reject(reviewer_user_id, reason)
    except ValueError as exc:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, str(exc)) from exc

    db.commit()
    db.refresh(doc)

    if was_approved:
        events.publish(events.EVENT_ATESTADO_REVOKED, doc.id, {"documentId": doc.id})
    return doc


def soft_delete(db: Session, doc: UserDocument) -> None:
    """Exclusao logica: revoga justificativas, remove o objeto e inativa."""
    if doc.is_atestado and doc.status == DocumentStatus.APPROVED.value:
        events.publish(events.EVENT_ATESTADO_REVOKED, doc.id, {"documentId": doc.id})

    storage.delete(doc.storage_key)
    doc.active = False
    db.commit()
