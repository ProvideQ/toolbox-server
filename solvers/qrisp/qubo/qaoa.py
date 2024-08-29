import argparse
import sys
from math import sqrt

import numpy as np
from gurobipy import read
from qrisp.core import demux
from qrisp.qaoa import (
    QAOAProblem,
    RX_mixer,
    create_QUBO_cl_cost_function,
    create_QUBO_cost_operator,
    def_backend,
)
from qrisp_qubo_util.permutation import create_perm_specifiers, eval_perm_old

from qrisp import QuantumArray, QuantumFloat, QuantumVariable, cyclic_shift, h, x

parser = argparse.ArgumentParser(
    prog="DWave QUBO solver",
    description="A CLI Program to initiate solving LP files with Qrisps QAOA Solver",
    epilog="Made by Lucas Berger for scientific purposes",
)

parser.add_argument("file")
parser.add_argument("--output-file")
parser.add_argument("--size-gate")

args = parser.parse_args()

m = read(args.file)

vars = [x.VarName for x in m.getVars() if x.VType == "B"]

qubo_size = len(vars)
size = int(sqrt(qubo_size))
qubo = np.zeros((qubo_size, qubo_size))

if args.size_gate and int(args.size_gate) < size:
    print(
        f"Stop execution to prevent denial of service due to a large QUBO. Size of the QUBO as VRP ({size})"
    )
    sys.exit(1)

obj = m.getObjective()

for term in range(obj.size()):
    num = obj.getCoeff(term)
    i = vars.index(obj.getVar1(term).VarName)
    j = vars.index(obj.getVar2(term).VarName)

    qubo[i, j] = num

problem = QAOAProblem(
    create_QUBO_cost_operator(qubo),
    RX_mixer,
    create_QUBO_cl_cost_function(qubo),
)


def init_func(qarg: QuantumArray):
    perm_specifiers = create_perm_specifiers(size)
    for qv in perm_specifiers:
        h(qv)
    perm = QuantumArray(QuantumFloat(int(np.ceil(np.log2(size)))), size)
    eval_perm_old(perm_specifiers, city_amount=size, qa=perm)

    for i in range(size * size):
        x_pos = int(i % size)
        if x_pos == 0:
            x(qarg[i])

    for i in range(size):
        cyclic_shift(qarg[i * size : (i + 1) * size], shift_amount=perm[i])

    for i in reversed(range(len(perm_specifiers))):
        demux(perm[i], perm_specifiers[i], perm[i:], permit_mismatching_size=True)

    for i in range(len(perm)):
        perm[i] -= i

    perm.delete()


problem.set_init_function(init_func)


qarg = QuantumArray(qtype=QuantumVariable(1), shape=(qubo_size))
res = problem.run(qarg, mes_kwargs={"backend": def_backend}, depth=1)
res = dict(list(res.items())[:1])

if args.output_file:
    with open(args.output_file, "w") as out:
        out.writelines([f"{bin}\n" for bin in list(list(res.keys())[0])])
else:
    print(list(list(res.keys())[0]))
