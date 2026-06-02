import math
from qiskit import QuantumCircuit
from qiskit.circuit import Instruction, Gate
from BaseCircuit.circuit_core import Circuit
from knapsack.knapsack import KnapsackInstance
from math import pi
from typing import Tuple
from qiskit.circuit.library import IntegerComparator, RYGate
from BaseCircuit.adder import constant_qft_add_gate, constant_qft_sub_gate
from BaseCircuit.comparator import apply_threshold_controlled_U

class QTG(Circuit):
    """
    Implements the Quantum Tree Generator (QTG) for generating superpositions of feasible solutions.
    """

    def __init__(self, input_circuit: Circuit, knapsack: KnapsackInstance, name: str = "QTG", depth_interval: Tuple[int, int] = (0, -1), current_best_solution: str= None, bias: float = 0.0, has_ancillas: bool = False):
        """
        Initialize the QTG instance.
        """
        self.input_circuit = input_circuit
        self.name = name
        self.knapsack = knapsack
        self.current_circuit = self.copy_circuit_structure()
        self.bias = bias
        self.has_ancillas = has_ancillas

        self.depth_interval = depth_interval
        if depth_interval[1] == -1:
            self.depth_interval = (depth_interval[0], len(knapsack.items))
        if current_best_solution is None:
            self.current_best_solution = '0' * len(knapsack.items)
        else:
            self.current_best_solution = current_best_solution
            
        self.initialize_capacity()
        print(f"QTG initialized with depth interval {self.depth_interval} and current best solution {self.current_best_solution}")

    def initialize_capacity(self, name_capacity_register: str = "capacity"):
        capacity_bin = format(self.knapsack.capacity, f'0{self.knapsack.capacity.bit_length()}b')
        # LSB first
        reg = self.current_circuit.get_register(name_capacity_register)
        for i, bit in enumerate(reversed(capacity_bin)):
            if bit == '1':
                self.current_circuit.qc.x(reg[i])

    def copy_circuit_structure(self) -> QuantumCircuit:
        qc = Circuit(name=self.name)
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

    def apply_U1_ancilla(
        self,
        m: int,
        decisions_reg_name: str = "items",
        capacity_reg_name: str = "capacity",
        flag_reg_name: str = "oracle",
        bias_angle: float = pi / 2
    ):
        """
        U1_m: compute feasibility flag 'remaining capacity >= w_m', apply controlled biased rotation on x_m, uncompute flag.
        Comparator + biased Hadamard (RY) + uncompute. Flag orientation may be inverted depending on library version.
        """
        decision_vars = self.current_circuit.get_register(decisions_reg_name)
        capacities = self.current_circuit.get_register(capacity_reg_name)
        flag = self.current_circuit.get_register(flag_reg_name)


        # Comparator: flips flag if cap >= wm
        comp = IntegerComparator(num_state_qubits=len(capacities), value=self.knapsack.weights[m], name=f'cap >= w_{m}')
        #determine how many ancillas the comparator needs.
        need_ancillas = comp.num_qubits - (len(capacities) + 1)
        if need_ancillas < 0:
            need_ancillas = 0
        anc_slice = []
        if need_ancillas > 0:
            anc_pool = None
            # Prefer any ancilla register that is not the flag register and has enough qubits
            for name, areg in self.current_circuit.registers.a.items():
                if name != flag_reg_name and len(areg) >= need_ancillas:
                    anc_pool = areg
                    break
            if anc_pool is None:
                raise ValueError(f"Insufficient ancillas for comparator: need {need_ancillas}, available "
                                f"{ {n: len(r) for n, r in self.current_circuit.registers.a.items()} } [1]")
            anc_slice = list(anc_pool[:need_ancillas])

        qargs_cmp = list(capacities)  + [flag[0]]+ anc_slice
        self.current_circuit.append_instruction(comp, qargs_cmp)

        # Controlled Hadamard on decision qubit x_m
        hprime_ctrl = self.biased_hadamard_gate(self.current_best_solution[m]).control(1)
        self.current_circuit.append_instruction(hprime_ctrl, [flag[0], decision_vars[m]])

        # Uncompute the flag to release ancillas back to |0> (reuse same qargs) [1]
        self.current_circuit.append_instruction(comp.inverse(), qargs_cmp)
        
    def apply_U1(
        self,
        m: int,
        decisions_reg_name: str = "items",
        capacity_reg_name: str = "capacity",
    ):
        """
        U1_m: compute feasibility flag 'remaining capacity >= w_m', apply controlled biased rotation on x_m, uncompute flag.
        Comparator + biased Hadamard (RY) + uncompute. Flag orientation may be inverted depending on library version.
        """
        decision_vars = self.current_circuit.get_register(decisions_reg_name)
        capacities = self.current_circuit.get_register(capacity_reg_name)

        U = self.biased_hadamard_gate(self.current_best_solution[m])
        controlled_H = apply_threshold_controlled_U(self.current_circuit.qc, capacities, decision_vars[m], U, self.knapsack.weights[m], inplace=False, label=f"CH(c >= w_{m})")
        # Controlled Hadamard on decision qubit x_m
        # INPLACE VERSION:
        #apply_threshold_controlled_U(self.current_circuit.qc, capacities, decision_vars[m], U, self.knapsack.weights[m], inplace=True)
        
        self.current_circuit.append_instruction(controlled_H, list(capacities)+ [decision_vars[m]])

    def apply_U2(
        self,
        m: int,
        decisions_reg_name: str = "items",
        capacity_reg_name: str = "capacity",
    ):
        """
        U2_m: subtract w_m from the capacity register controlled by x_m (QFT subtractor) [1].
        """
        decision_vars = self.current_circuit.get_register(decisions_reg_name)
        capacity = self.current_circuit.get_register(capacity_reg_name)
        wm = self.knapsack.weights[m]

        sub_gate_ctrl = constant_qft_sub_gate(len(capacity), wm, name=f"Sub(w_{m})").control(1)
        self.current_circuit.append_instruction(sub_gate_ctrl, [decision_vars[m]] + list(capacity))

    def apply_U3(
        self,
        m: int,
        decisions_reg_name: str = "items",
        profit_reg_name: str = "profit",
    ):
        """
        U3_m: add p_m to the profit register controlled by x_m (QFT adder) [1].
        """
        decision_vars = self.current_circuit.get_register(decisions_reg_name)
        profit = self.current_circuit.get_register(profit_reg_name)

        add_gate_ctrl = constant_qft_add_gate(len(profit), self.knapsack.values[m], name=f"Add(p_{m})").control(1)
        self.current_circuit.append_instruction(add_gate_ctrl, [decision_vars[m]] + list(profit))

    def build_circuit(self) -> QuantumCircuit:
        for i in range(self.depth_interval[0], self.depth_interval[1]):
            if self.has_ancillas:
                self.apply_U1_ancilla(i)
            else:
                self.apply_U1(i)
            self.apply_U2(i)
            self.apply_U3(i)

    def to_instruction(self) -> Instruction:
        return self.build_circuit().to_instruction()
    
    def biased_hadamard_gate(self, current_best_bit: int) -> Gate:
        """
        Biased Hadamard via RY(theta) on the decision qubit.
        """
        if current_best_bit == '0':
            return RYGate(2*math.acos(math.sqrt((1+self.bias)/(2+self.bias))))
        elif current_best_bit == '1':
            return RYGate(2*math.acos(math.sqrt((1)/(2+self.bias))))
    
        else:
           raise ValueError("Invalid current best bit value. Must be '0' or '1'.")
       




