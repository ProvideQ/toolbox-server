#!/usr/bin/env python3
import time
from pathlib import Path
from typing import Any

import pyzx as zx


def run_pyzx(
        qasm_a: str,
        qasm_b: str,
) -> dict[str, Any]:
    started = time.perf_counter()

    try:
        circuit_a = zx.Circuit.from_qasm(qasm_a)
        circuit_b = zx.Circuit.from_qasm(qasm_b)

        equivalent = circuit_a.verify_equality(circuit_b)

        runtime_ms = round((time.perf_counter() - started) * 1000, 3)

        return {
            "strategy": "pyzx",
            # False vorsichtshalber nicht als bewiesene Nichtäquivalenz behandeln.
            "status": "equivalent" if equivalent else "unknown",
            "globalPhaseIgnored": True if equivalent else None,
            "runtimeMs": runtime_ms,
            "rawEquivalence": str(equivalent),
            "message": (
                None
                if equivalent
                else "PyZX konnte die Äquivalenz nicht beweisen."
            ),
            "error": None,
        }

    except Exception as exc:
        runtime_ms = round((time.perf_counter() - started) * 1000, 3)

        return {
            "strategy": "pyzx",
            "status": "error",
            "globalPhaseIgnored": None,
            "runtimeMs": runtime_ms,
            "rawEquivalence": None,
            "message": str(exc),
            "error": {
                "type": type(exc).__name__,
                "message": str(exc),
            },
        }
