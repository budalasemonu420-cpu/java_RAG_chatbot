"""Deterministic Qwen-first, Llama-fallback selection for local Ollama tags."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class ModelSelection:
    name: str | None
    family: str | None


def select_local_model(model_names: list[str]) -> ModelSelection:
    names = sorted(set(name.strip() for name in model_names if name.strip()), key=str.lower)
    qwen = [name for name in names if "qwen" in name.lower()]
    if qwen:
        return ModelSelection(qwen[0], "Qwen")
    llama = [name for name in names if "llama" in name.lower()]
    if llama:
        return ModelSelection(llama[0], "Llama")
    return ModelSelection(None, None)