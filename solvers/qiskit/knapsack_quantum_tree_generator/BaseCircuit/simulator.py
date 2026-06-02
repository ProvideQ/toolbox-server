

from typing import Any, Dict, Optional
from data.SimulateOptions import SimulationOptions
from BaseCircuit.circuit_core import Circuit
from qiskit import transpile
from qiskit.quantum_info import Statevector
from qiskit.visualization import plot_histogram

try:
    # Qiskit Aer is optional; for shot-based or noisy simulation
    from qiskit_aer import AerSimulator
    AER_AVAILABLE = True
except Exception:
    AER_AVAILABLE = False


class Simulator:
    def __init__(self, circuit: Circuit):
        self.circuit = circuit
        self.result = None
        self.simulated = False
        
    def simulate(self, sim: Optional[SimulationOptions] = None) -> Dict[str, Any]:
        """Start simulation of the circuit according to SimulationOptions."""
        sim = sim or SimulationOptions()

        # Analytic statevector path (no noise, no shots)
        if sim.method == "statevector" and sim.noise_model is None and (sim.shots is None or sim.shots <= 0):
            sv = Statevector.from_instruction(self.qc)
            return {"statevector": sv}

        # Aer required for shot-based / noisy runs
        if not AER_AVAILABLE:
            raise RuntimeError("Aer is not available. Install qiskit-aer for shot-based/noisy simulation.")

        # Default shots when not in analytic statevector mode
        if sim.shots is None:
            sim.shots = 1024

        qc_to_run = self.circuit.qc.copy()

        # Auto-insert measurements only if requested
        if sim.method != "statevector" and sim.shots and sim.shots > 0 and sim.measure_all_if_none:
            def _has_meas_or_cond(qc):
                for ci in qc.data:
                    op = ci.operation  # CircuitInstruction.operation
                    if op.name == "measure":
                        return True
                    if getattr(ci, "condition", None) is not None:
                        return True
                return False

            if not _has_meas_or_cond(qc_to_run):
                qc_to_run.measure_all()

        method = sim.method if sim.method in ("automatic", "statevector", "density_matrix") else "automatic"
        backend = AerSimulator(method=method)
        if sim.noise_model is not None:
            backend.set_options(noise_model=sim.noise_model)
        if sim.seed_simulator is not None:
            backend.set_options(seed_simulator=sim.seed_simulator)

        tqc = transpile(qc_to_run, backend, optimization_level=0)
        job = backend.run(tqc, shots=sim.shots)
        result = job.result()

        payload: Dict[str, Any] = {"result": result}

        # Counts, invert bitstrings to upset qiskit MSB notation.
        try:
            counts = result.get_counts(0)
            counts = {bits[::-1]: cnt for bits, cnt in counts.items()}
            payload["counts"] = counts
        except Exception:
            pass

        # Statevector
        try:
            payload["statevector"] = result.get_statevector(0)
        except Exception:
            pass

        self.result = payload
        self.simulated = True
        return True
    
    def get_probabilities_from_counts(self):
        if self.simulated is False or self.result is None or "counts" not in self.result:
            raise RuntimeError("No counts available. Please run simulate() with shot-based options first.")
        total = sum(self.result["counts"].values())
        probs = {bits: c / total for bits, c in self.result["counts"].items()}
        # Sort by probability (descending) and print
        return {bits: p for bits, p in sorted(probs.items(), key=lambda kv: kv[1], reverse=True)}
    
    def plot_counts(self) -> Any:
        """Plot counts using Qiskit visualization tools."""
        return plot_histogram(data=self.result["counts"], title="Measurement Counts")

    def plot_infered_prob_from_counts(self) -> Any:
        """Plot inferred probabilities from counts using Qiskit visualization tools."""
        return plot_histogram(data=self.get_probabilities_from_counts(), title="Inferred Probabilities from Counts", sort='value_desc')

