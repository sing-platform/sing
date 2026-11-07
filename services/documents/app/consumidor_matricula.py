"""Consome `documents.matricula.criada` e grava o comprovante no Mongo deste recorte.

Não usa o PostgreSQL de atestados, o MinIO nem o Mongo do communication.
Não faz HTTP para o core: o payload do evento já traz ids e nomes.
"""
import json
import logging
import threading

import pika
from pymongo import MongoClient
from pymongo.errors import PyMongoError

from .comprovante import processar
from .config import settings

log = logging.getLogger(__name__)

FILA = "documents.matricula.criada"
ROUTING_KEY = "matricula.criada"


class ConsumidorMatricula(threading.Thread):
    def __init__(self):
        super().__init__(daemon=True, name="matricula-comprovante")
        self._parar = threading.Event()

    def parar(self):
        self._parar.set()

    def run(self):
        while not self._parar.is_set():
            try:
                self._consumir()
            except Exception:
                log.warning("consumidor de matricula caiu; nova tentativa em 5s", exc_info=True)
                self._parar.wait(5)

    def _consumir(self):
        cliente = MongoClient(settings.mongo_comprovantes_uri, serverSelectionTimeoutMS=3000)
        colecao = cliente.get_default_database()["comprovantes"]
        colecao.create_index("eventId", unique=True)

        parametros = pika.ConnectionParameters(
            host=settings.rabbitmq_host,
            port=settings.rabbitmq_port,
            credentials=pika.PlainCredentials(settings.rabbitmq_user, settings.rabbitmq_password),
            connection_attempts=1,
            socket_timeout=3,
        )
        conexao = pika.BlockingConnection(parametros)
        canal = conexao.channel()
        canal.exchange_declare(exchange=settings.rabbitmq_exchange, exchange_type="topic", durable=True)
        canal.queue_declare(queue=FILA, durable=True)
        canal.queue_bind(queue=FILA, exchange=settings.rabbitmq_exchange, routing_key=ROUTING_KEY)
        canal.basic_qos(prefetch_count=1)

        def callback(ch, method, _properties, body):
            try:
                evento = json.loads(body)
            except Exception:
                log.warning("mensagem de matricula nao e json; descartada")
                ch.basic_ack(method.delivery_tag)
                return
            try:
                resultado = processar(evento, colecao, settings.documents_comprovante_max_bytes)
                log.info("matricula %s -> %s", evento.get("eventId"), resultado)
                ch.basic_ack(method.delivery_tag)
            except PyMongoError:
                log.warning("mongo do comprovante falhou; mensagem volta para a fila", exc_info=True)
                ch.basic_nack(method.delivery_tag, requeue=True)

        canal.basic_consume(queue=FILA, on_message_callback=callback)
        log.info("escutando %s", FILA)
        while not self._parar.is_set() and conexao.is_open:
            conexao.process_data_events(time_limit=1)
        try:
            conexao.close()
        finally:
            cliente.close()


def mongo_comprovantes_ok() -> bool:
    try:
        cliente = MongoClient(settings.mongo_comprovantes_uri, serverSelectionTimeoutMS=500)
        cliente.admin.command("ping")
        cliente.close()
        return True
    except Exception:
        return False
