"""Health payload for the Python module."""


def status():
    return {"status": "ok", "service": "python-service"}
