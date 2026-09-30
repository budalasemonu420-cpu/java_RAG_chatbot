"""Small local HTTP client for Ollama's tags and chat endpoints."""

from __future__ import annotations

from dataclasses import dataclass

import requests

from core.model_selector import ModelSelection, select_local_model


DEFAULT_OLLAMA_URL = "http://127.0.0.1:11434"


@dataclass(frozen=True)
class OllamaStatus:
    connected: bool
    models: list[str]
    selection: ModelSelection
    error: str | None = None


def detect_ollama_models(base_url: str = DEFAULT_OLLAMA_URL) -> OllamaStatus:
    try:
        response = requests.get(f"{base_url.rstrip('/')}/api/tags", timeout=2)
        response.raise_for_status()
        payload = response.json()
        names = [item.get("name", "") for item in payload.get("models", [])]
        return OllamaStatus(True, names, select_local_model(names))
    except (requests.RequestException, ValueError, TypeError) as exc:
        return OllamaStatus(False, [], ModelSelection(None, None), str(exc))


def generate_answer(
    model_name: str,
    system_prompt: str,
    user_prompt: str,
    temperature: float = 0.2,
    base_url: str = DEFAULT_OLLAMA_URL,
    timeout: int = 180,
) -> str:
    response = requests.post(
        f"{base_url.rstrip('/')}/api/chat",
        json={
            "model": model_name,
            "stream": False,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "options": {"temperature": temperature},
        },
        timeout=timeout,
    )
    response.raise_for_status()
    payload = response.json()
    answer = payload.get("message", {}).get("content", "").strip()
    if not answer:
        raise RuntimeError("Ollama returned an empty response.")
    return answer