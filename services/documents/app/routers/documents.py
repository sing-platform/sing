"""Endpoints de documentos.

O prefixo `/api/documents` e o mesmo do monolito, entao o frontend nao muda
quando a rota passa a ser servida por este servico.
"""
import io
from datetime import date

from fastapi import (
    APIRouter,
    Depends,
    File,
    Form,
    HTTPException,
    Query,
    Request,
    UploadFile,
    status,
)
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from .. import core_client, service, storage
from ..database import get_db
from ..enums import (
    SELF_UPLOAD_CATEGORIES,
    THERAPEUTIC_CATEGORIES,
    DocumentCategory,
    DocumentScope,
)
from ..schemas import (
    AtestadoApproveRequest,
    AtestadoRejectRequest,
    CoveringAtestadoResponse,
    DocumentResponse,
    DocumentUpdateRequest,
)
from ..security import CurrentUser, current_user, require_admin_or_secretary

router = APIRouter(prefix="/api/documents", tags=["Documents"])


def _bearer(request: Request) -> str:
    return request.headers.get("Authorization", "")


# ── Upload ───────────────────────────────────────────────────────────────────

@router.post("/me", response_model=DocumentResponse, status_code=status.HTTP_201_CREATED,
             summary="Enviar documento proprio (aluno)")
def upload_own(
    file: UploadFile = File(...),
    category: DocumentCategory = Form(...),
    description: str | None = Form(None),
    reference_date_start: date | None = Form(None),
    reference_date_end: date | None = Form(None),
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    service.assert_category_allowed(category, SELF_UPLOAD_CATEGORIES)
    doc = service.create_document(
        db,
        owner_person_id=user.require_person_id(),
        uploaded_by_user_id=user.require_user_id(),
        content=file.file.read(),
        filename=file.filename,
        category=category,
        scope=DocumentScope.STUDENT_PERSONAL,
        description=description,
        reference_date_start=reference_date_start,
        reference_date_end=reference_date_end,
    )
    return doc


@router.post("/student/{student_id}", response_model=DocumentResponse,
             status_code=status.HTTP_201_CREATED,
             summary="Enviar documento do aluno (responsavel)")
def upload_for_student(
    student_id: int,
    request: Request,
    file: UploadFile = File(...),
    category: DocumentCategory = Form(...),
    description: str | None = Form(None),
    reference_date_start: date | None = Form(None),
    reference_date_end: date | None = Form(None),
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    service.assert_category_allowed(category, SELF_UPLOAD_CATEGORIES)
    if not core_client.is_guardian_of(user.require_person_id(), student_id, _bearer(request)):
        raise HTTPException(status.HTTP_403_FORBIDDEN,
                            "Acesso negado: voce nao e responsavel por este aluno.")
    return service.create_document(
        db,
        owner_person_id=student_id,
        uploaded_by_user_id=user.require_user_id(),
        content=file.file.read(),
        filename=file.filename,
        category=category,
        scope=DocumentScope.STUDENT_PERSONAL,
        description=description,
        reference_date_start=reference_date_start,
        reference_date_end=reference_date_end,
    )


@router.post("/person/{person_id}", response_model=DocumentResponse,
             status_code=status.HTTP_201_CREATED,
             summary="Enviar documento de qualquer pessoa (gestao)")
def upload_for_person(
    person_id: int,
    file: UploadFile = File(...),
    category: DocumentCategory = Form(...),
    description: str | None = Form(None),
    user: CurrentUser = Depends(require_admin_or_secretary),
    db: Session = Depends(get_db),
):
    return service.create_document(
        db,
        owner_person_id=person_id,
        uploaded_by_user_id=user.require_user_id(),
        content=file.file.read(),
        filename=file.filename,
        category=category,
        scope=DocumentScope.INSTITUTIONAL,
        description=description,
    )


@router.post("/patient/{patient_person_id}/therapeutic", response_model=DocumentResponse,
             status_code=status.HTTP_201_CREATED,
             summary="Enviar anotacao terapeutica (psicologo)")
def upload_therapeutic(
    patient_person_id: int,
    file: UploadFile = File(...),
    category: DocumentCategory = Form(...),
    description: str | None = Form(None),
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    if not user.is_psychologist:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Requer perfil de psicologo.")
    service.assert_category_allowed(category, THERAPEUTIC_CATEGORIES)
    return service.create_document(
        db,
        owner_person_id=patient_person_id,
        uploaded_by_user_id=user.require_user_id(),
        content=file.file.read(),
        filename=file.filename,
        category=category,
        scope=DocumentScope.THERAPEUTIC_PRIVATE,
        description=description,
    )


# ── Listagem ─────────────────────────────────────────────────────────────────

@router.get("/me", response_model=list[DocumentResponse], summary="Meus documentos")
def list_own(user: CurrentUser = Depends(current_user), db: Session = Depends(get_db)):
    return service.list_by_person(db, user.require_person_id())


@router.get("/student/{student_id}", response_model=list[DocumentResponse],
            summary="Documentos do aluno (responsavel)")
def list_for_student(
    student_id: int,
    request: Request,
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    if not core_client.is_guardian_of(user.require_person_id(), student_id, _bearer(request)):
        raise HTTPException(status.HTTP_403_FORBIDDEN,
                            "Acesso negado: voce nao e responsavel por este aluno.")
    return service.list_by_person(db, student_id)


@router.get("/person/{person_id}", response_model=list[DocumentResponse],
            summary="Documentos de uma pessoa (gestao)")
def list_for_person(
    person_id: int,
    _: CurrentUser = Depends(require_admin_or_secretary),
    db: Session = Depends(get_db),
):
    return service.list_by_person(db, person_id)


@router.get("/patient/{patient_person_id}/therapeutic", response_model=list[DocumentResponse],
            summary="Anotacoes terapeuticas do paciente (psicologo autor)")
def list_therapeutic(
    patient_person_id: int,
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    if not user.is_psychologist:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Requer perfil de psicologo.")
    return service.list_therapeutic(db, patient_person_id, user.require_user_id())


# ── Atestados (secretaria) ───────────────────────────────────────────────────

@router.get("/atestados/pending", response_model=list[DocumentResponse],
            summary="Atestados aguardando revisao")
def list_pending(
    _: CurrentUser = Depends(require_admin_or_secretary),
    db: Session = Depends(get_db),
):
    return service.list_atestados(db, only_pending=True)


@router.get("/atestados", response_model=list[DocumentResponse],
            summary="Todos os atestados")
def list_all_atestados(
    _: CurrentUser = Depends(require_admin_or_secretary),
    db: Session = Depends(get_db),
):
    return service.list_atestados(db, only_pending=False)


@router.patch("/{document_id}/approve", response_model=DocumentResponse,
              summary="Aprovar atestado e justificar faltas do periodo")
def approve(
    document_id: int,
    body: AtestadoApproveRequest,
    user: CurrentUser = Depends(require_admin_or_secretary),
    db: Session = Depends(get_db),
):
    return service.approve(
        db, document_id, user.require_user_id(),
        body.reference_date_start, body.reference_date_end,
    )


@router.patch("/{document_id}/reject", response_model=DocumentResponse,
              summary="Rejeitar atestado")
def reject(
    document_id: int,
    body: AtestadoRejectRequest,
    user: CurrentUser = Depends(require_admin_or_secretary),
    db: Session = Depends(get_db),
):
    return service.reject(db, document_id, user.require_user_id(), body.reason)


# ── Download / update / delete ───────────────────────────────────────────────

@router.get("/{document_id}/download", summary="Baixar arquivo")
def download(
    document_id: int,
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    doc = service.get_active_or_404(db, document_id)
    service.assert_can_access(doc, user)
    content = storage.download(doc.storage_key)
    return StreamingResponse(
        io.BytesIO(content),
        media_type=doc.mime_type,
        headers={"Content-Disposition": f'attachment; filename="{doc.original_file_name}"'},
    )


@router.put("/{document_id}", response_model=DocumentResponse, summary="Editar descricao")
def update_description(
    document_id: int,
    body: DocumentUpdateRequest,
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    doc = service.get_active_or_404(db, document_id)
    service.assert_can_access(doc, user)
    doc.description = body.description
    db.commit()
    db.refresh(doc)
    return doc


@router.delete("/{document_id}", status_code=status.HTTP_204_NO_CONTENT,
               summary="Excluir documento")
def delete(
    document_id: int,
    user: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    doc = service.get_active_or_404(db, document_id)
    service.assert_can_access(doc, user)
    service.soft_delete(db, doc)


# ── API interna (consumida pelo core) ────────────────────────────────────────

@router.get("/internal/covering", response_model=CoveringAtestadoResponse,
            summary="[interno] Atestado aprovado que cobre a data")
def covering_atestado(
    person_id: int = Query(..., description="person_id do aluno"),
    day: date = Query(..., description="data da aula"),
    _: CurrentUser = Depends(current_user),
    db: Session = Depends(get_db),
):
    """Consultado pelo core ao registrar uma falta.

    Se este servico estiver fora, o core grava ABSENT e o evento
    AtestadoApproved corrige depois — degradacao graciosa em vez de erro.
    """
    return CoveringAtestadoResponse(document_id=service.find_covering_approved(db, person_id, day))
