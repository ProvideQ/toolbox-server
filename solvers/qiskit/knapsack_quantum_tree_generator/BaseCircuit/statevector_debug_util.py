from pathlib import Path
import numpy as np
from qiskit import QuantumCircuit, transpile
from qiskit_aer import AerSimulator
import json
from qiskit.quantum_info import Statevector


def save_step_by_step_sv_probs(
    qc: QuantumCircuit,
    out_dir: str = "log/full_probs_by_step",
    threshold: float = 1e-6,
):
    """
    Save the full probability distribution after every gate to separate JSON files.
    Each JSON maps bitstring -> probability for the entire system.
    """
    qc_dbg = QuantumCircuit(*qc.qregs, *qc.cregs)
    for i, (op, qargs, cargs) in enumerate(qc.data):
        qc_dbg.append(op, qargs, cargs)
        qc_dbg.save_statevector(label=f"sv_{i:04d}")

    backend = AerSimulator(method="statevector")
    tqc = transpile(qc_dbg, backend, optimization_level=0)
    result = backend.run(tqc).result()

    out_path = Path(out_dir)
    out_path.mkdir(parents=True, exist_ok=True)

    data = result.data(0)
    for key in sorted(k for k in data.keys() if k.startswith("sv_")):
        sv = data[key]
        if not isinstance(sv, Statevector):
            sv = Statevector(sv)
        probs = sv.probabilities_dict()  # all qubits
        # Filter small entries for readability
        probs = {k: float(v) for k, v in probs.items() if v > threshold}
        with open(out_path / f"{key}.json", "w") as f:
            json.dump(probs, f, indent=2)
    print(f"Wrote full probability snapshots to: {out_dir}")
    
def save_step_by_step_sv_amplitudes(
    qc: QuantumCircuit,
    out_dir: str = "log/full_probs_by_step",
    threshold: float = 1e-6,
):
    """
    Save the full amplitude distribution after every gate to separate JSON files.
    Each JSON maps bitstring -> {re, im}.
    """
    qc_dbg = QuantumCircuit(*qc.qregs, *qc.cregs)
    for i, (op, qargs, cargs) in enumerate(qc.data):
        qc_dbg.append(op, qargs, cargs)
        qc_dbg.save_statevector(label=f"sv_{i:04d}")
    backend = AerSimulator(method="statevector")
    tqc = transpile(qc_dbg, backend, optimization_level=0)
    result = backend.run(tqc).result()
    out_path = Path(out_dir)
    out_path.mkdir(parents=True, exist_ok=True)
    data = result.data(0)
    for key in sorted(k for k in data.keys() if k.startswith("sv_")):
        sv = data[key]
        if not isinstance(sv, Statevector):
            sv = Statevector(sv)
        amp_dict = sv.to_dict()  # bitstring -> complex amplitude
        # Filter small entries for readability (by probability magnitude)
        amps = {
            k: {"re": float(np.real(v)), "im": float(np.imag(v))}
            for k, v in amp_dict.items()
            if (np.abs(v) ** 2) > threshold
        }
        with open(out_path / f"{key}.json", "w") as f:
            json.dump(amps, f, indent=2)
    print(f"Wrote full amplitude snapshots to: {out_dir}")
