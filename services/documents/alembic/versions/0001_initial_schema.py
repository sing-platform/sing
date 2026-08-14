"""schema inicial do servico de documentos

Espelha a tabela `user_documents` do monolito (V1/V2/V6), agora no banco
proprio deste servico. Sem FK para person/users: essas entidades vivem no
core e sao referenciadas por id — foi justamente isso que tornou este modulo
o candidato de menor risco para a primeira extracao.

Revision ID: 0001
Revises:
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "user_documents",
        sa.Column("id", sa.BigInteger(), sa.Identity(always=False), primary_key=True),
        sa.Column("owner_person_id", sa.BigInteger(), nullable=False),
        sa.Column("file_name", sa.String(255), nullable=False),
        sa.Column("original_file_name", sa.String(255), nullable=False),
        sa.Column("mime_type", sa.String(100), nullable=False),
        sa.Column("file_size", sa.BigInteger(), nullable=False),
        sa.Column("category", sa.String(30), nullable=False),
        sa.Column("scope", sa.String(30), nullable=False),
        sa.Column("storage_key", sa.String(512), nullable=False),
        sa.Column("uploaded_by_user_id", sa.BigInteger(), nullable=False),
        sa.Column("uploaded_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("description", sa.String(500), nullable=True),
        sa.Column("active", sa.Boolean(), nullable=False, server_default=sa.true()),
        # fluxo de aprovacao
        sa.Column("status", sa.String(20), nullable=False, server_default="PENDING"),
        sa.Column("reference_date_start", sa.Date(), nullable=True),
        sa.Column("reference_date_end", sa.Date(), nullable=True),
        sa.Column("reviewed_by_user_id", sa.BigInteger(), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("rejection_reason", sa.String(500), nullable=True),
    )
    op.create_index("idx_user_doc_owner", "user_documents", ["owner_person_id"])
    op.create_index("idx_user_doc_scope", "user_documents", ["scope"])
    op.create_index("idx_user_doc_uploader", "user_documents", ["uploaded_by_user_id"])
    op.create_index("idx_user_doc_status_category", "user_documents", ["status", "category"])
    # consulta do core ao lancar falta: por aluno + intervalo de cobertura
    op.create_index(
        "idx_user_doc_covering",
        "user_documents",
        ["owner_person_id", "status", "reference_date_start", "reference_date_end"],
    )


def downgrade() -> None:
    op.drop_table("user_documents")
