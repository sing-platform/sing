"""Publicacao de eventos de dominio no RabbitMQ.

No monolito, aprovar um atestado escrevia direto na tabela `attendance`
(AtestadoApprovalService era "a unica ponte explicita entre os modulos").
Aqui essa escrita vira evento: quem e dono de `attendance` e o core.

Justificar falta tolera consistencia eventual — a secretaria aprova e o
efeito pode chegar segundos depois. O que NAO tolera (matricula, vaga,
chamada, declaracao) continua sincrono e dentro do core.

Envelope versionado desde o inicio: mudar um campo sem versao quebraria os
consumidores em silencio.
"""
import json
import logging
import uuid
from datetime import date, datetime, timezone
from typing import Any

import pika

from .config import settings

log = logging.getLogger(__name__)

EVENT_ATESTADO_APPROVED = "AtestadoApproved"
EVENT_ATESTADO_REVOKED = "AtestadoRevoked"

ROUTING_KEYS = {
    EVENT_ATESTADO_APPROVED: "atestado.approved",
    EVENT_ATESTADO_REVOKED: "atestado.revoked",
}


def _envelope(event_type: str, aggregate_id: int, payload: dict[str, Any]) -> bytes:
    body = {
        "eventId": str(uuid.uuid4()),
        "eventType": event_type,
        "version": 1,
        "occurredAt": datetime.now(timezone.utc).isoformat(),
        "aggregateId": str(aggregate_id),
        "payload": payload,
    }
    return json.dumps(body, default=_json_default).encode("utf-8")


def _json_default(value):
    if isinstance(value, (date, datetime)):
        return value.isoformat()
    raise TypeError(f"nao serializavel: {type(value)}")


def publish(event_type: str, aggregate_id: int, payload: dict[str, Any]) -> bool:
    """Publica o evento. Devolve False se o broker estiver indisponivel.

    A falha nao derruba a operacao de negocio: o documento ja foi aprovado e
    persistido. O core reconcilia consultando /internal/atestados/covering
    quando registra a falta.
    """
    routing_key = ROUTING_KEYS[event_type]
    try:
        params = pika.ConnectionParameters(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            credentials=pika.PlainCredentials(
                settings.rabbitmq_user, settings.rabbitmq_password
            ),
            connection_attempts=1,
            socket_timeout=3,
        )
        with pika.BlockingConnection(params) as conn:
            ch = conn.channel()
            ch.exchange_declare(
                exchange=settings.rabbitmq_exchange, exchange_type="topic", durable=True
            )
            ch.basic_publish(
                exchange=settings.rabbitmq_exchange,
                routing_key=routing_key,
                body=_envelope(event_type, aggregate_id, payload),
                properties=pika.BasicProperties(
                    content_type="application/json", delivery_mode=2  # persistente
                ),
            )
        log.info("evento %s publicado (doc=%s)", event_type, aggregate_id)
        return True
    except Exception:
        log.warning(
            "broker indisponivel; evento %s (doc=%s) nao publicado",
            event_type, aggregate_id, exc_info=True,
        )
        return False
