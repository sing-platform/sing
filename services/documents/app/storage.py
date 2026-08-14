"""Storage de arquivos em S3 (MinIO local, S3 real em nuvem).

Substitui o AzureBlobStorageService do monolito. A chave de armazenamento
mantem o mesmo formato: documents/{ownerPersonId}/{uuid}{ext}
"""
import logging
import uuid
from pathlib import PurePosixPath

import boto3
from botocore.client import Config
from botocore.exceptions import ClientError

from .config import settings

log = logging.getLogger(__name__)


def _client():
    return boto3.client(
        "s3",
        endpoint_url=settings.s3_endpoint,
        aws_access_key_id=settings.s3_access_key,
        aws_secret_access_key=settings.s3_secret_key,
        region_name=settings.s3_region,
        config=Config(signature_version="s3v4"),
    )


def ensure_bucket() -> None:
    """Cria o bucket se ainda nao existir (idempotente)."""
    s3 = _client()
    try:
        s3.head_bucket(Bucket=settings.s3_bucket)
    except ClientError:
        s3.create_bucket(Bucket=settings.s3_bucket)
        log.info("bucket %s criado", settings.s3_bucket)


def build_storage_key(owner_person_id: int, original_filename: str | None) -> str:
    ext = PurePosixPath(original_filename or "").suffix
    return f"documents/{owner_person_id}/{uuid.uuid4()}{ext}"


def upload(content: bytes, storage_key: str, mime_type: str) -> None:
    _client().put_object(
        Bucket=settings.s3_bucket,
        Key=storage_key,
        Body=content,
        ContentType=mime_type,
    )


def download(storage_key: str) -> bytes:
    obj = _client().get_object(Bucket=settings.s3_bucket, Key=storage_key)
    return obj["Body"].read()


def delete(storage_key: str) -> None:
    """Remove o objeto. Nao propaga erro: a exclusao logica do documento nao
    deve falhar porque o storage esta indisponivel."""
    try:
        _client().delete_object(Bucket=settings.s3_bucket, Key=storage_key)
    except ClientError:
        log.warning("falha ao remover %s do storage", storage_key, exc_info=True)
