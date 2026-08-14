"""Contratos de entrada e saida.

`DocumentResponse` reproduz campo a campo o record homonimo do monolito, para
que o frontend nao precise mudar quando a rota passa a ser servida por este
servico. Os nomes ficam em camelCase pelo mesmo motivo.
"""
from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from .enums import DocumentCategory, DocumentScope, DocumentStatus


class _CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True,
    )


class DocumentResponse(_CamelModel):
    id: int
    owner_person_id: int
    original_file_name: str
    mime_type: str
    file_size: int
    category: DocumentCategory
    scope: DocumentScope
    description: str | None = None
    uploaded_at: datetime
    # fluxo de aprovacao (relevante para ATESTADO_MEDICO / JUSTIFICATIVA_FALTA)
    status: DocumentStatus
    reference_date_start: date | None = None
    reference_date_end: date | None = None
    reviewed_by_user_id: int | None = None
    reviewed_at: datetime | None = None
    rejection_reason: str | None = None


class DocumentUpdateRequest(_CamelModel):
    description: str | None = Field(default=None, max_length=500)


class AtestadoApproveRequest(_CamelModel):
    reference_date_start: date
    reference_date_end: date


class AtestadoRejectRequest(_CamelModel):
    reason: str = Field(min_length=1, max_length=500)


class CoveringAtestadoResponse(_CamelModel):
    """Resposta da consulta que o core faz ao registrar uma falta."""
    document_id: int | None = None
