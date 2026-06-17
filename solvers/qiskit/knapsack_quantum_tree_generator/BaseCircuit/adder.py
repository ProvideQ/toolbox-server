from qiskit import QuantumCircuit
from qiskit.circuit import Gate
from qiskit.circuit.library import QFT
from math import pi
from typing import Optional

def constant_qft_add_gate(n_bits: int, const: int, name: Optional[str] = None) -> Gate:
    """
    QFT-based constant adder: |v> -> |(v + const) mod 2^n_bits>.
    Uses QFT, per-qubit phase rotations, and inverse QFT.
    """
    
    qc = QuantumCircuit(n_bits, name=name or f"AddConst({const})")
    qft_gate = QFT(n_bits, do_swaps=False).to_gate(label="QFT")
    iqft_gate = qft_gate.inverse()

    qc.append(qft_gate, range(n_bits))
    for k in range(n_bits):
        angle = 2 * pi * const / (2 ** (k + 1))
        qc.p(angle, k)
    qc.append(iqft_gate, range(n_bits))
    return qc.to_gate(label=name or f"AddConst({const})")



def constant_qft_sub_gate(n_bits: int, const: int, name: Optional[str] = None) -> Gate:
    """
    QFT-based constant subtractor: |v> -> |(v - const) mod 2^n_bits>.
    Implemented by flipping the signs of the adder’s phase rotations [1].
    """
    return constant_qft_add_gate(n_bits, const=-const, name=name or f"SubConst({const})")

