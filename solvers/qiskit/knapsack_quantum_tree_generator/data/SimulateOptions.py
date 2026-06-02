from dataclasses import dataclass
from typing import Any, Optional


@dataclass
class SimulationOptions:
    method: str = "statevector"  # "statevector" or "automatic" or "density_matrix" (if Aer)
    shots: Optional[int] = None
    seed_simulator: Optional[int] = None
    noise_model: Any = None  # from qiskit_aer.noise import NoiseModel
    measure_all_if_none: bool = True  # For shot-based runs, add measure_all if circuit has none