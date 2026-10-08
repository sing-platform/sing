"""Enums do dominio de documentos.

Os valores sao identicos aos do monolito (br.sing.document.domain.enums):
sao serializados em JSON e gravados no banco como string, entao mudar um
valor aqui quebra o contrato com o core e com o frontend.
"""
from enum import Enum


class DocumentCategory(str, Enum):
    ATESTADO_MEDICO = "ATESTADO_MEDICO"
    JUSTIFICATIVA_FALTA = "JUSTIFICATIVA_FALTA"
    COMPROVANTE_RESIDENCIA = "COMPROVANTE_RESIDENCIA"
    RG = "RG"
    CPF = "CPF"
    COMPROVANTE_MATRICULA = "COMPROVANTE_MATRICULA"
    NOTA_FISCAL = "NOTA_FISCAL"
    ANOTACAO_TERAPEUTICA = "ANOTACAO_TERAPEUTICA"
    OUTROS = "OUTROS"


class DocumentScope(str, Enum):
    STUDENT_PERSONAL = "STUDENT_PERSONAL"
    INSTITUTIONAL = "INSTITUTIONAL"
    THERAPEUTIC_PRIVATE = "THERAPEUTIC_PRIVATE"


class DocumentStatus(str, Enum):
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"


#: Categorias que passam pelo fluxo de aprovacao da secretaria e, quando
#: aprovadas, justificam faltas no core.
ATESTADO_CATEGORIES = frozenset({
    DocumentCategory.ATESTADO_MEDICO,
    DocumentCategory.JUSTIFICATIVA_FALTA,
})

#: O que aluno/responsavel pode enviar por conta propria.
SELF_UPLOAD_CATEGORIES = ATESTADO_CATEGORIES

#: Exclusivo do psicologo.
THERAPEUTIC_CATEGORIES = frozenset({DocumentCategory.ANOTACAO_TERAPEUTICA})
