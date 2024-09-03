import numpy as np
from qrisp import QuantumArray, QuantumFloat
from qrisp.core import demux
from qrisp.environments import invert


# Create a function that generates a state of superposition of all permutations
def swap_to_front(qa, index):
    with invert():
        # The keyword ctrl_method = "gray_pt" allows the controlled swaps to be synthesized
        # using Margolus gates. These gates perform the same operation as a regular Toffoli
        # but add a different phase for each input. This phase will not matter though,
        # since it will be reverted once the ancilla values of the oracle are uncomputed.
        demux(qa[0], index, qa, permit_mismatching_size=True)


def eval_perm(perm_specifiers, city_amount, qa=None):
    N = len(perm_specifiers)

    # To filter out the cyclic permutations, we impose that the first city is always city 0
    # We will have to consider this assumption later when calculating the route distance
    # by manually adding the trip distance of the first trip (from city 0) and the
    # last trip (to city 0)
    if qa is None:
        qa = QuantumArray(
            QuantumFloat(int(np.ceil(np.log2(city_amount)))), city_amount - 1
        )

    for i in range(city_amount - 1):
        qa[i] += i + 1

    for i in range(N):
        swap_to_front(qa[i:], perm_specifiers[i])

    return qa


def eval_perm_backward(perm_specifiers, city_amount, qa=None):
    N = len(perm_specifiers)

    # To filter out the cyclic permutations, we impose that the first city is always city 0
    # We will have to consider this assumption later when calculating the route distance
    # by manually adding the trip distance of the first trip (from city 0) and the
    # last trip (to city 0)

    for i in reversed(range(N)):
        demux(qa[i], perm_specifiers[i], qa[i:], permit_mismatching_size=True)

    for i in range(city_amount - 1):
        qa[i] -= i + 1

    return qa


def eval_perm_old(perm_specifiers, city_amount, qa=None):
    N = len(perm_specifiers)

    # To filter out the cyclic permutations, we impose that the first city is always city 0
    # We will have to consider this assumption later when calculating the route distance
    # by manually adding the trip distance of the first trip (from city 0) and the
    # last trip (to city 0)
    if qa is None:
        qa = QuantumArray(QuantumFloat(int(np.ceil(np.log2(city_amount)))), city_amount)

    add = np.arange(0, city_amount)

    for i in range(city_amount):
        qa[i] += int(add[i])

    for i in range(N):
        swap_to_front(qa[i:], perm_specifiers[i])

    return qa


# Create function that returns QuantumFloats specifying the permutations (these will be in uniform superposition)
def create_perm_specifiers(city_amount, init_seq=None) -> list[QuantumFloat]:
    perm_specifiers = []

    for i in range(city_amount - 1):
        qf_size = int(np.ceil(np.log2(city_amount - i)))

        if i == 0:
            continue

        temp_qf = QuantumFloat(qf_size)

        if not init_seq is None:
            temp_qf[:] = init_seq[i - 1]

        perm_specifiers.append(temp_qf)

    return perm_specifiers
