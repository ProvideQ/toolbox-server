from typing import Any

from run_pyzx import run_pyzx
from run_qcec import run_qcec_isolated


def run(strategy: str, qasm_a: str, qasm_b: str,
        qcec_options: dict[str, Any] | None = None) -> dict[str, Any]:
    if strategy == "pyzx":
        return run_pyzx(
            qasm_a=qasm_a,
            qasm_b=qasm_b,
        )
    elif strategy == "mqt-qcec":
        return run_qcec_isolated(
            qasm_a=qasm_a,
            qasm_b=qasm_b,
            qcec_options=qcec_options,
        )
    else:
        raise ValueError(f"Unknown strategy: {strategy}")
