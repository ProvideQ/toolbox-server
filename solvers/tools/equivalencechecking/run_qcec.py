#!/usr/bin/env python3
import multiprocessing
import os
import time
from typing import Any

from mqt import qcec

DEFAULT_QCEC_OPTIONS: dict[str, Any] = {
    # QCEC's parallel checker can remain stuck while joining its native worker
    # threads.
    "parallel": False,
}
DEFAULT_HARD_TIMEOUT_SECONDS = float(
    os.getenv("QCEC_HARD_TIMEOUT_SECONDS", "10")
)


def normalize_status(raw_equivalence: str) -> tuple[str, bool | None]:
    value = raw_equivalence.lower()

    if "not" in value and "equivalent" in value:
        return "not_equivalent", None

    if "up_to_global_phase" in value or "global_phase" in value:
        return "equivalent", True

    if "equivalent" in value:
        return "equivalent", False

    if "unknown" in value or "no_information" in value or "inconclusive" in value:
        return "unknown", None

    return "unknown", None


def stringify_equivalence(result: Any) -> str:
    equivalence = getattr(result, "equivalence", result)

    if callable(equivalence):
        equivalence = equivalence()

    if hasattr(equivalence, "name"):
        return str(equivalence.name)

    if hasattr(equivalence, "value"):
        return str(equivalence.value)

    return str(equivalence)


def run_qcec(
        qasm_a: str,
        qasm_b: str,
        qcec_options: dict[str, Any] | None = None,
) -> dict[str, Any]:
    started = time.perf_counter()
    effective_options = DEFAULT_QCEC_OPTIONS | (qcec_options or {})

    try:
        result = qcec.verify(qasm_a, qasm_b, **effective_options)

        raw_equivalence = stringify_equivalence(result)
        status, global_phase_ignored = normalize_status(raw_equivalence)

        runtime_ms = round((time.perf_counter() - started) * 1000, 3)

        return {
            "strategy": "mqt-qcec",
            "status": status,
            "globalPhaseIgnored": global_phase_ignored,
            "runtimeMs": runtime_ms,
            "rawEquivalence": raw_equivalence,
            "message": None,
            "error": None,
        }

    except Exception as exc:
        runtime_ms = round((time.perf_counter() - started) * 1000, 3)

        return {
            "strategy": "mqt-qcec",
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


def _isolated_qcec_worker(
        connection: Any,
        qasm_a: str,
        qasm_b: str,
        qcec_options: dict[str, Any] | None,
) -> None:
    try:
        connection.send(run_qcec(qasm_a, qasm_b, qcec_options))
    finally:
        connection.close()


def run_qcec_isolated(
        qasm_a: str,
        qasm_b: str,
        qcec_options: dict[str, Any] | None = None,
        hard_timeout_seconds: float = DEFAULT_HARD_TIMEOUT_SECONDS,
) -> dict[str, Any]:
    """Run QCEC in a disposable process with a reliable wall-clock timeout."""
    if hard_timeout_seconds <= 0:
        raise ValueError("hard_timeout_seconds must be greater than zero")

    started = time.perf_counter()
    context = multiprocessing.get_context("spawn")
    receiving_connection, sending_connection = context.Pipe(duplex=False)
    process = context.Process(
        target=_isolated_qcec_worker,
        args=(sending_connection, qasm_a, qasm_b, qcec_options),
        daemon=True,
    )

    try:
        process.start()
        sending_connection.close()

        if receiving_connection.poll(hard_timeout_seconds):
            try:
                result = receiving_connection.recv()
            except EOFError:
                result = None

            process.join(timeout=1)
            if result is not None:
                return result

            message = (
                "The QCEC worker exited without returning a result "
                f"(exit code {process.exitcode})."
            )
            error_type = "WorkerProcessError"
        else:
            message = (
                "MQT QCEC exceeded the hard timeout of "
                f"{hard_timeout_seconds:g} seconds."
            )
            error_type = "TimeoutError"
    except Exception as exc:
        message = str(exc)
        error_type = type(exc).__name__
    finally:
        receiving_connection.close()
        sending_connection.close()
        if process.is_alive():
            process.terminate()
            process.join(timeout=1)
        if process.is_alive():
            process.kill()
            process.join()

    runtime_ms = round((time.perf_counter() - started) * 1000, 3)
    return {
        "strategy": "mqt-qcec",
        "status": "error",
        "globalPhaseIgnored": None,
        "runtimeMs": runtime_ms,
        "rawEquivalence": None,
        "message": message,
        "error": {
            "type": error_type,
            "message": message,
        },
    }
