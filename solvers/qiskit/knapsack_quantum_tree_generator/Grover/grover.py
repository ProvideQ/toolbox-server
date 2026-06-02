from qiskit import QuantumCircuit
from BaseCircuit.circuit_core import Circuit
from knapsack.knapsack import KnapsackInstance
from typing import Tuple
from BaseCircuit.comparator import apply_threshold_controlled_U
from qiskit.circuit.library import XGate


class Grover(Circuit):
    def __init__(
        self,
        input_circuit: Circuit,
        knapsack: KnapsackInstance,
        depth_interval: Tuple[int, int] = (0, -1),
        name: str = "Grover",
        current_threshold: int = 0,
        optimal_iterations: int = None 
    ):
        """
        Initialize the Grover instance.
        """
        self.input_circuit = input_circuit
        self.name = name
        self.knapsack = knapsack
        self.current_circuit = self.copy_circuit_structure()
        self.current_threshold = current_threshold
        #the input circuit is always also the state prep circuit
        self.state_prep_instruction = self.input_circuit.get_circuit_as_instruction(name="QTG" if depth_interval==(0,-1) else "QTG_partial")
        if optimal_iterations is None:
            raise ValueError("optimal_iterations must be provided for Grover initialization.")
        self.optimal_iterations = optimal_iterations
        
        
        self.depth_interval = depth_interval
        if depth_interval[1] == -1:
            self.depth_interval = (depth_interval[0], len(knapsack.items))
            
        # this modifies the current threshold to exlude items than canont be feasible from some partial depth
        if self.depth_interval[1] < len(knapsack.items):
            self.current_threshold = self.current_threshold - sum(self.knapsack.values[self.depth_interval[1]:])
            print(f'Grover inner threshold modified to {self.current_threshold} for depth interval {self.depth_interval}')
        
        
                    
    def copy_circuit_structure(self, name:str = None) -> QuantumCircuit:
        qc = Circuit(name=name if name is not None else self.name)
        for qreg in self.input_circuit.registers.q:
            reg = self.input_circuit.get_register(qreg)
            qc.add_qubits(reg.name, reg.size)
        for areg in self.input_circuit.registers.a:
            reg = self.input_circuit.get_register(areg)
            qc.add_ancilla(reg.name, reg.size)
        for creg in self.input_circuit.registers.c:
            reg = self.input_circuit.get_register(creg)
            qc.add_clbits(reg.name, reg.size)
        return qc
            
    def oracle(self,
           profit_reg_name: str = "profit",
           oracle_reg_name: str = "oracle",
           as_instruction: bool = False,
           label: str | None = None):
        
        qc = self.current_circuit.qc
        profit_reg = self.current_circuit.get_register(profit_reg_name)
        oracle = self.current_circuit.get_register(oracle_reg_name)[0]

        if not as_instruction:
            qc.h(oracle)
            qc.z(oracle)

            # apply phase to ancilla which will become a global phase for the decision qubits
            controlled_Z = apply_threshold_controlled_U(
                circ=qc,
                qreg=profit_reg,
                target=oracle,
                U=XGate(),
                w=self.current_threshold,
                broadcast=False,
                inplace=False
            )
            self.current_circuit.append_instruction(controlled_Z, list(profit_reg)+ [oracle])
            # Unprepare ancilla back to |0>
            qc.z(oracle)
            qc.h(oracle)
            return qc
        else:
            # Build a reusable instruction that includes prepare-|->, comparator, and unprepare
            n = len(profit_reg)
            sub = QuantumCircuit(n + 1, name=label if label is not None else f"Oracle_P(x)>={self.current_threshold}")
            reg = [sub.qubits[i] for i in range(n)]
            anc = sub.qubits[n]

            sub.h(anc)
            sub.z(anc)
            apply_threshold_controlled_U(
                circ=sub,
                qreg=reg,
                target=anc,
                U=XGate(),
                w=self.current_threshold,
                broadcast=False,
                inplace=True
            )
            sub.z(anc)
            sub.h(anc)

            instr = sub.to_instruction()
            qc.append(instr, list(profit_reg) + [oracle])
            return instr

    def diffuser(self, as_instruction: bool = False, label: str | None = None):
        """
        Build/apply the Grover reflection R_psi = A (I - 2|0...0><0...0|) A^\dagger,
        where A is given by self.state_prep_circuit.

        If as_instruction=True, rappend whole instruction.
        If as_instruction=False, append to the same qubits that state_prep_circuit prepares.
        """
        qc = self.current_circuit.qc
        if not as_instruction:
            # the x gates transfer to controls on 1, the hadamard mcx hadarmard is a mcz so we control the last one phase flip on 11111111, and Z only acts on 1.
            qc.append(self.state_prep_instruction.inverse(), list(qc.qubits)[:self.state_prep_instruction.num_qubits])
            for q in list(qc.qubits)[:self.state_prep_instruction.num_qubits-1]:
                qc.x(q)
            qc.x(list(qc.qubits)[self.state_prep_instruction.num_qubits - 1])
            qc.h(list(qc.qubits)[self.state_prep_instruction.num_qubits - 1])
            qc.mcx(list(qc.qubits)[:self.state_prep_instruction.num_qubits - 1], qc.qubits[self.state_prep_instruction.num_qubits - 1])
            qc.h(list(qc.qubits)[self.state_prep_instruction.num_qubits - 1])
            qc.x(list(qc.qubits)[self.state_prep_instruction.num_qubits - 1])
            for q in list(qc.qubits)[:self.state_prep_instruction.num_qubits-1]:
                qc.x(q)
            qc.append(self.state_prep_instruction, list(qc.qubits)[:self.state_prep_instruction.num_qubits])
            return qc
        
        else:
            sub = QuantumCircuit(self.state_prep_instruction.num_qubits, name=label if label is not None else "R_psi")
            sub_qubits = list(sub.qubits)

            sub.append(self.state_prep_instruction, sub_qubits)
            for q in sub_qubits:
                sub.x(q)
            sub.h(sub_qubits[-1])
            sub.mcx(sub_qubits[:-1], sub_qubits[-1])  # no-ancilla MCX
            sub.h(sub_qubits[-1])
            for q in sub_qubits:
                sub.x(q)
            sub.append(self.state_prep_instruction.inverse(), sub_qubits)
            instr = sub.to_instruction()
            qc.append(instr, list(qc.qubits)[:self.state_prep_instruction.num_qubits])
            return instr
            

    def build_circuit(self):
        for _ in range(self.optimal_iterations):
            self.oracle(as_instruction=False)
            self.diffuser(as_instruction=False)

