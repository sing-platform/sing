"""Configuracao do servico, toda por variavel de ambiente."""
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # ---- banco proprio (database-per-service) ----
    database_url: str = "postgresql+psycopg://sing:sing@localhost:5433/sing_documents"

    # ---- storage S3/MinIO ----
    s3_endpoint: str = "http://localhost:9000"
    s3_access_key: str = "singadmin"
    s3_secret_key: str = ""
    s3_bucket: str = "sing-documents"
    s3_region: str = "us-east-1"

    # ---- autenticacao ----
    # HS256 compartilhado com o core nesta etapa. A migracao para RS256 + JWKS
    # (Keycloak, ja no compose) e o proximo passo: com HS256 todo servico que
    # valida o token tambem consegue emiti-lo.
    jwt_secret: str = ""
    jwt_algorithm: str = "HS256"

    # ---- mensageria ----
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "guest"
    rabbitmq_password: str = "guest"
    rabbitmq_exchange: str = "sing.events"

    # ---- integracao com o core ----
    core_base_url: str = "http://localhost:8080"
    core_timeout_seconds: float = 5.0

    # ---- regras de upload (identicas as do monolito) ----
    max_file_size_mb: int = 10
    allowed_mime_types: str = "application/pdf,image/jpeg,image/png,image/webp"

    @property
    def max_file_size_bytes(self) -> int:
        return self.max_file_size_mb * 1024 * 1024

    @property
    def allowed_mimes(self) -> set[str]:
        return {m.strip() for m in self.allowed_mime_types.split(",") if m.strip()}


settings = Settings()
