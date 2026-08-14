"""Autenticacao por JWT emitido pelo core.

O token traz `sub` (e-mail), `role`, `uid` (id do usuario) e, quando existe
pessoa associada, `pid` (id da pessoa). Com essas claims o servico decide
sozinho quem e o portador, sem consultar o core a cada requisicao.

LIMITACAO CONHECIDA: a assinatura ainda e HS256 (segredo compartilhado com o
core), o que significa que este servico tambem consegue *emitir* tokens
validos. O Keycloak ja esta na stack para migrar isso para RS256 + JWKS, onde
os servicos recebem apenas a chave publica. Ver docs/adr/0002.
"""
import base64
import binascii
from dataclasses import dataclass

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt

from .config import settings

_bearer = HTTPBearer(auto_error=True)


def _signing_key() -> bytes:
    """Deriva a chave do mesmo jeito que o core (JwtService.getSignInKey).

    O core tenta decodificar o segredo como Base64 e so cai para os bytes
    literais se isso falhar. Um segredo em hex — como o gerado por
    `openssl rand -hex 48` — e Base64 valido, entao o core assina com os bytes
    DECODIFICADOS. Usar a string literal aqui produziria outra chave e toda
    assinatura seria rejeitada.
    """
    secret = settings.jwt_secret
    try:
        return base64.b64decode(secret, validate=True)
    except (binascii.Error, ValueError):
        return secret.encode("utf-8")

ADMIN_ROLES = {"ROLE_ADMIN", "ROLE_SECRETARY", "ROLE_MANAGER"}
PSYCHOLOGIST_ROLES = {"ROLE_PSYCHOLOGIST"}


@dataclass(frozen=True)
class CurrentUser:
    email: str
    user_id: int | None
    person_id: int | None
    role: str | None

    @property
    def is_admin_or_secretary(self) -> bool:
        return self.role in ADMIN_ROLES

    @property
    def is_psychologist(self) -> bool:
        return self.role in PSYCHOLOGIST_ROLES

    def require_user_id(self) -> int:
        if self.user_id is None:
            raise HTTPException(
                status.HTTP_401_UNAUTHORIZED,
                "Token sem a claim 'uid'. Refaca o login no core.",
            )
        return self.user_id

    def require_person_id(self) -> int:
        if self.person_id is None:
            raise HTTPException(
                status.HTTP_403_FORBIDDEN,
                "Usuario sem pessoa associada; operacao indisponivel.",
            )
        return self.person_id


def current_user(
    creds: HTTPAuthorizationCredentials = Depends(_bearer),
) -> CurrentUser:
    try:
        payload = jwt.decode(
            creds.credentials,
            _signing_key(),
            algorithms=[settings.jwt_algorithm],
            # o core nao preenche `aud`; sem isto o jose rejeitaria o token
            options={"verify_aud": False},
        )
    except JWTError as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Token invalido") from exc

    return CurrentUser(
        email=payload.get("sub", ""),
        user_id=_as_int(payload.get("uid")),
        person_id=_as_int(payload.get("pid")),
        role=payload.get("role"),
    )


def require_admin_or_secretary(user: CurrentUser = Depends(current_user)) -> CurrentUser:
    if not user.is_admin_or_secretary:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Requer perfil administrativo.")
    return user


def _as_int(value) -> int | None:
    if value is None:
        return None
    try:
        return int(value)
    except (TypeError, ValueError):
        return None
