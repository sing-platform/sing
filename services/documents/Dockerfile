FROM python:3.12-slim

# libmagic1: base do python-magic, que detecta o MIME real pelos magic bytes.
# E o equivalente ao Apache Tika usado no monolito — sem ele o servico nao sobe.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libmagic1 curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Camada de dependencias separada do codigo: so reinstala se requirements mudar.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY alembic.ini .
COPY alembic ./alembic
COPY app ./app
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh

RUN useradd -r -u 1001 appuser && chown -R appuser /app
USER appuser

EXPOSE 8000
ENTRYPOINT ["./entrypoint.sh"]
