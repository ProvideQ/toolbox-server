from dataclasses import dataclass
from typing import Any, Optional, Sequence

@dataclass
class TranspileOptions:
    optimization_level: int = 2
    seed_transpiler: Optional[int] = None
    basis_gates: Optional[Sequence[str]] = None
    layout_method: Optional[str] = None
    routing_method: Optional[str] = None
    coupling_map: Any = None  # can be CouplingMap or list
    target: Any = None  # qiskit.transpiler.Target (optional)