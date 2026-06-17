from typing import Dict, List, Sequence, Any, Union, Optional
from qiskit import QuantumCircuit, QuantumRegister, AncillaRegister
from qiskit.circuit import Instruction, ClassicalRegister
from knapsack.knapsack import KnapsackInstance



class RegisterBank:
    """
    Helper to keep named registers organized.
    """

    def __init__(self):
        self.q: Dict[str, QuantumRegister] = {}
        self.c: Dict[str, ClassicalRegister] = {}
        self.a: Dict[str, AncillaRegister] = {}

    def add_qubits(self, name: str, size: int) -> QuantumRegister:
        if name in self.q:
            raise ValueError(f"Quantum register '{name}' already exists")
        reg = QuantumRegister(size, name=name)
        self.q[name] = reg
        return reg

    def add_ancilla(self, name: str, size: int) -> AncillaRegister:
        if name in self.a:
            raise ValueError(f"Ancilla register '{name}' already exists")
        reg = AncillaRegister(size, name=name)
        self.a[name] = reg
        return reg

    def add_clbits(self, name: str, size: int) -> ClassicalRegister:
        if name in self.c:
            raise ValueError(f"Classical register '{name}' already exists")
        reg = ClassicalRegister(size, name=name)
        self.c[name] = reg
        return reg

    def get(self, name: List[str]) -> Union[QuantumRegister, AncillaRegister, ClassicalRegister]:
        list_of_registers = []
        for n in name:
            if n in self.q:
                list_of_registers.append(self.q[n])
            if n in self.a:
                list_of_registers.append(self.a[n])
            if n in self.c:
                list_of_registers.append(self.c[n])
        if len(list_of_registers) == 1:
            return list_of_registers[0]
        elif len(list_of_registers) > 1:
            return list_of_registers
        else:
            raise KeyError(f"Register '{name}' not found")

    def has_measurements(self) -> bool:
        return len(self.c) > 0


class Circuit:
    """
    Thin wrapper around QuantumCircuit with named register management.
    """

    def __init__(self, name: str = "circuit"):
        self.name = name
        self.registers = RegisterBank()
        self.qc = QuantumCircuit(name=name)
        self.metadata: Dict[str, Any] = {}

    def add_qubits(self, name: str, size: int) -> QuantumRegister:
        reg = self.registers.add_qubits(name, size)
        self.qc.add_register(reg)
        return reg

    def add_ancilla(self, name: str, size: int) -> AncillaRegister:
        reg = self.registers.add_ancilla(name, size)
        self.qc.add_register(reg)
        return reg

    def add_clbits(self, name: str, size: int) -> ClassicalRegister:
        reg = self.registers.add_clbits(name, size)
        self.qc.add_register(reg)
        return reg

    def get_register(self, name: Union[str, List[str]]) -> Union[QuantumRegister, AncillaRegister, ClassicalRegister]:
        """Get a register by name."""
        if isinstance(name, str):
            return self.registers.get([name])
        return self.registers.get(name)

    def get_qubits_in_registers(self, names: List[str] = [], all=False) -> List[Any]:
        """Get all qubits in the specified registers as a flat list."""
        if all and len(names) != 0:
            raise ValueError("If 'all' is True, 'names' must be an empty list.")
        if not all and len(names) == 0:
            raise ValueError("If 'all' is False, 'names' must contain at least one register name.")
        
        if all:
            names = list(self.registers.q.keys()) + list(self.registers.a.keys()) + list(self.registers.c.keys())
        qubits = []
        for name in names:
            reg = self.get_register(name)
            for q in reg:
                qubits.append(q)
        return qubits


    def append_instruction(
        self,
        inst: Instruction,
        qargs: Sequence[Any],
        cargs: Optional[Sequence[Any]] = None,
    ):
        self.qc.append(inst, qargs, [] if cargs is None else cargs)

    def append_subcircuit_as_instruction(
        self,
        sub: QuantumCircuit,
        qubits: Sequence[Any],
        clbits: Optional[Sequence[Any]] = None,
        name: Optional[str] = None,
    ):
        """Append a subcircuit as a single instruction to this circuit."""
        inst = sub.to_instruction()
        if name:
            inst.name = name
        self.append_instruction(inst, qubits, [] if clbits is None else clbits)
        


    def append_subcircuit_inline_simple(self, sub: QuantumCircuit):
        """
        Adding instruction 1 by 1 without mapping, assuming:
        - both circuits have the same number of qubits,
        - no classical bits are present.
        """
        if len(sub.qc.clbits) != 0 or len(self.qc.clbits) != 0:
            raise ValueError("This simplified method requires circuits without classical bits.")
        if len(sub.qc.qubits) != len(self.qc.qubits):
            raise ValueError("Both circuits must have the same number of qubits.")

        # Map sub qubits -> target qubits by index
        qmap = {sub_q: self.qc.qubits[i] for i, sub_q in enumerate(sub.qc.qubits)}

        # Append each instruction as-is
        for ci in sub.qc.data:
            op = ci.operation
            mapped_qargs = [qmap[q] for q in ci.qubits]
            self.qc.append(op, mapped_qargs, [])

    def to_instruction(self, name: Optional[str] = None) -> Instruction:
        inst = self.qc.to_instruction()
        if name:
            inst.name = name
        return inst

    def prepare_knapsack_circuit(self, knapsack_instance: 'KnapsackInstance', has_ancillas = False) -> None:
        """Prepare the quantum circuit for the knapsack problem."""
        self.add_qubits("items", knapsack_instance.num_items)
        self.add_qubits("capacity", knapsack_instance.capacity.bit_length())
        max_profit = sum(it.value for it in knapsack_instance.items)
        self.add_qubits("profit", max_profit.bit_length())
        self.add_ancilla("oracle", 1)
        if has_ancillas:
            self.add_ancilla("compare_flag", 1)
            self.add_ancilla("comparator", max(knapsack_instance.num_items, knapsack_instance.capacity.bit_length(), max_profit.bit_length()))
        # set capacity register to knapsack capacity


    def measure_items(self, creg_name: str = "items_c"):
        self.add_clbits(creg_name, len(self.get_register("items")))
        # Freeze ordering before measuring
        self.qc.barrier(*self.qc.qubits)
        self.measure_register("items", creg_name)

    def measure_register(self, qreg_name: str, creg_name: str):
        qreg = self.get_register(qreg_name)
        creg = self.get_register(creg_name)
        self.qc.measure(qreg, creg)


    def draw(self, output: str = "text") -> Any:
        return self.qc.draw(output=output)
    
    def get_circuit_as_instruction(self, name: Optional[str] = None) -> Instruction:
        inst = self.qc.to_instruction()
        if name:
            inst.name = name
        return inst






