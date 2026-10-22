"""Smoke: o pacote importa sem Postgres/MinIO e expoe /actuator/health."""

from app.main import app


def test_health_route_exists():
    paths = [getattr(route, "path", None) for route in app.routes]
    assert "/actuator/health" in paths


def test_title():
    assert app.title == "SING Documents"
