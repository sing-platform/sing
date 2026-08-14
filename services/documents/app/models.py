"""Modelo de dados do servico de documentos.

Este servico e o unico dono da tabela `user_documents`. Referencias a pessoas
e usuarios sao IDs simples (sem FK), porque essas entidades vivem no core —
o mesmo padrao que o monolito ja usava aqui, o que torna esta a extracao de
menor risco do sistema.
"""
from datetime import date, datetime, timezone

from sqlalchemy import BigInteger, Boolean, Date, DateTime, Index, String, Text
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

from .enums import DocumentCategory, DocumentScope, DocumentStatus


class Base(DeclarativeBase):
    pass


class UserDocument(Base):
    __tablename__ = "user_documents"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)

    owner_person_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    file_name: Mapped[str] = mapped_column(String(255), nullable=False)
    original_file_name: Mapped[str] = mapped_column(String(255), nullable=False)
    mime_type: Mapped[str] = mapped_column(String(100), nullable=False)
    file_size: Mapped[int] = mapped_column(BigInteger, nullable=False)
    category: Mapped[str] = mapped_column(String(30), nullable=False)
    scope: Mapped[str] = mapped_column(String(30), nullable=False)
    storage_key: Mapped[str] = mapped_column(String(512), nullable=False)
    uploaded_by_user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    uploaded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    description: Mapped[str | None] = mapped_column(String(500))
    active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    # ---- fluxo de aprovacao (atestados / justificativas) ----
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default=DocumentStatus.PENDING.value
    )
    reference_date_start: Mapped[date | None] = mapped_column(Date)
    reference_date_end: Mapped[date | None] = mapped_column(Date)
    reviewed_by_user_id: Mapped[int | None] = mapped_column(BigInteger)
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    rejection_reason: Mapped[str | None] = mapped_column(String(500))

    __table_args__ = (
        Index("idx_user_doc_owner", "owner_person_id"),
        Index("idx_user_doc_scope", "scope"),
        Index("idx_user_doc_uploader", "uploaded_by_user_id"),
        Index("idx_user_doc_status_category", "status", "category"),
    )

    # ---- regras de dominio (espelham UserDocument.java) ----

    def approve(self, reviewer_user_id: int, start: date, end: date) -> None:
        if start is None or end is None:
            raise ValueError("Periodo de referencia obrigatorio para aprovacao.")
        if end < start:
            raise ValueError("Data final nao pode ser anterior a data inicial.")
        self.status = DocumentStatus.APPROVED.value
        self.reference_date_start = start
        self.reference_date_end = end
        self.reviewed_by_user_id = reviewer_user_id
        self.reviewed_at = datetime.now(timezone.utc)
        self.rejection_reason = None

    def reject(self, reviewer_user_id: int, reason: str) -> None:
        if not reason or not reason.strip():
            raise ValueError("Motivo da rejeicao e obrigatorio.")
        self.status = DocumentStatus.REJECTED.value
        self.reviewed_by_user_id = reviewer_user_id
        self.reviewed_at = datetime.now(timezone.utc)
        self.rejection_reason = reason

    @property
    def is_atestado(self) -> bool:
        from .enums import ATESTADO_CATEGORIES
        return DocumentCategory(self.category) in ATESTADO_CATEGORIES

    @property
    def is_therapeutic(self) -> bool:
        return self.scope == DocumentScope.THERAPEUTIC_PRIVATE.value
