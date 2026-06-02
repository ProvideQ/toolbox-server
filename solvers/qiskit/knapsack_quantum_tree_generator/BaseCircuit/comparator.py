from qiskit import QuantumCircuit
from qiskit.circuit import Instruction
from typing import Sequence, Optional, Union
from qiskit.circuit import QuantumRegister, Qubit


def apply_threshold_controlled_U(circ: QuantumCircuit,
                                 qreg: Sequence,
                                 target,
                                 U: Instruction,
                                 w: int,
                                 inplace: bool = True,
                                 broadcast: bool = False,
                                 label: Optional[str] = None) -> Union[QuantumCircuit, Instruction]:
    """
    There are 3 cases: qreg = w, qreg < w, qreg > w.
    Choose the first position where qreg and w differ when scanning from MSB to LSB --> position j.
    - (1) If qreg[j] = 0 and w[j] = 1 --> qreg < w --> do nothing.
    - (2) If qreg[j] = 1 and w[j] = 0 --> qreg > w --> apply U on target.
    - (3) if no such j is found --> qreg = w --> apply U on target.
    
    We only care about cases (2) and (3). So for all j where w[j] = 0, we control on all qreg[k>j] = w[k] and qreg[j] = 1 to apply U on target for case (2).
    Finally, we control on all qreg[k] = w[k] to apply U on target for case (3).
    Because if qreg[k>j] = w[k] not true there must be some index t, with qreg[t]=0 und w[t]=1, which is case (1) and we don't care about it.
    
    Since we at most check all bits, we have O(log(C)) gates.

    If inplace=True:
        - Modifies 'circ' in place and returns 'circ'.
    If inplace=False:
        - Returns an Instruction (built from a fresh (n+1)-qubit subcircuit) that performs
          the same operation, with name/label 'label' or 'CU(c>={w})'.
          You must append it with the qubits: list(qreg) + [target].
          
    Broadcast applies U to all targets in target register
    """
    n = len(qreg)
    if U.num_qubits != 1:
        raise ValueError("U must be a single-qubit gate/instruction.")

    if broadcast:
        if not isinstance(target, QuantumRegister):
            raise ValueError("When broadcast=True, 'target' must be a non-empty sequence of qubits.")
        tgt_list = list(target)
    else:
        # Single target
        if isinstance(target, Sequence):
            raise ValueError("When broadcast=False, 'target' must be a single qubit (or a sequence of length 1).")
        else:
            tgt_list = [target]

    m = len(tgt_list)

    def _body(on_circ: QuantumCircuit, reg, targets):
        # Edge cases on the comparator condition
        if n == 0:
            if w <= 0:
                for t in targets:
                    on_circ.append(U, [t])
            return
        if w <= 0:
            for t in targets:
                on_circ.append(U, [t])
            return
        if w >= (1 << n):
            return

        # Bits of w in little-endian: bits[i] is bit i (i=0 is LSB, i=n-1 is MSB)
        bits = [(w >> i) & 1 for i in range(n)]

        # Clauses for first-difference positions where w_j = 0 (x >= w because x_j=1 and all higher bits equal)
        for j in range(n - 1, -1, -1):  # MSB down to LSB
            if bits[j] == 0:
                zeros_higher = [t for t in range(j + 1, n) if bits[t] == 0]
                for t in zeros_higher:
                    on_circ.x(reg[t])  # control-on-0 -> control-on-1 for higher positions
                control_qubits = [reg[t] for t in range(j + 1, n)] + [reg[j]]
                CU = U.control(len(control_qubits))
                for tgt in targets:
                    on_circ.append(CU, control_qubits + [tgt])
                for t in zeros_higher:
                    on_circ.x(reg[t])  # undo

        # Equality clause x == w (all bits match w)
        zeros_all = [t for t in range(n) if bits[t] == 0]
        for t in zeros_all:
            on_circ.x(reg[t])
        CU_eq = U.control(n)
        for tgt in targets:
            on_circ.append(CU_eq, list(reg) + [tgt])
        for t in zeros_all:
            on_circ.x(reg[t])

    if inplace:
        _body(circ, qreg, tgt_list)
        return circ
    else:
        # Build a minimal subcircuit and return as instruction
        if broadcast:
            sc = QuantumCircuit(n + m, name=label or f"CU_broadcast(c>={w})")
            reg = [sc.qubits[i] for i in range(n)]
            targets = [sc.qubits[n + i] for i in range(m)]
        else:
            sc = QuantumCircuit(n + 1, name=label or f"CU(c>={w})")
            reg = [sc.qubits[i] for i in range(n)]
            targets = [sc.qubits[n]]  # single target
        _body(sc, reg, targets)
        return sc.to_instruction()

