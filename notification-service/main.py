import os
from contextlib import asynccontextmanager
from typing import List

import py_eureka_client.eureka_client as eureka_client
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

PORT = int(os.getenv("PORT", "8084"))
HOST = os.getenv("EUREKA_INSTANCE_HOST", "localhost")
EUREKA_SERVER = os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Enregistrement dans Eureka au démarrage
    await eureka_client.init_async(
        eureka_server=EUREKA_SERVER,
        app_name="notification",          # apparaît en NOTIFICATION dans Eureka
        instance_host=HOST,
        instance_ip=HOST,
        instance_port=PORT,
        health_check_url=f"http://{HOST}:{PORT}/health",
        status_page_url=f"http://{HOST}:{PORT}/health",
    )
    yield
    # Désenregistrement à l'arrêt
    await eureka_client.stop_async()


app = FastAPI(title="Notification service", lifespan=lifespan)


class Notification(BaseModel):
    destinataire: str
    message: str


notifications: List[dict] = []


@app.get("/health")
def health():
    return {"status": "UP"}


@app.get("/notifications")
def list_notifications():
    return notifications


@app.post("/notifications", status_code=201)
def send_notification(n: Notification):
    item = {"id": len(notifications) + 1, **n.model_dump()}
    notifications.append(item)
    return item


@app.get("/notifications/{nid}")
def get_notification(nid: int):
    for n in notifications:
        if n["id"] == nid:
            return n
    raise HTTPException(status_code=404, detail="not found")


@app.get("/discover/{app_name}")
async def discover(app_name: str):
    """Liste les instances d'un autre service enregistré dans Eureka."""
    client = eureka_client.get_client()
    app_info = client.applications.get_application(app_name.upper())
    return [
        {"host": i.hostName, "ip": i.ipAddr, "port": i.port.port, "status": i.status}
        for i in (app_info.instances if app_info else [])
    ]
