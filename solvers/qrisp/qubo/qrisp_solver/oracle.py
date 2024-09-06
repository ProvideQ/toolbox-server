import numpy as np
from qrisp import *

from .distance import (
    qdict_calc_perm_travel_distance_backward,
    qdict_calc_perm_travel_distance_forward,
)
from .permutation import eval_perm, eval_perm_backward


def eval_distance_threshold(
    perm_specifiers,
    precision,
    threshold,
    city_amount,
    distance_matrix,
    city_demand,
    max_cap,
):
    itinerary = QuantumArray(
        QuantumFloat(int(np.ceil(np.log2(city_amount)))), city_amount - 1
    )

    eval_perm(perm_specifiers, city_amount=city_amount, qa=itinerary)

    distance, demand_array, demand_indexer = qdict_calc_perm_travel_distance_forward(
        itinerary, precision, city_amount, distance_matrix, city_demand, max_cap
    )

    is_below_treshold = distance <= threshold

    z(is_below_treshold)

    is_below_treshold.uncompute()

    qdict_calc_perm_travel_distance_backward(
        itinerary,
        precision,
        city_amount,
        distance_matrix,
        city_demand,
        max_cap,
        forward_result=(distance, demand_array, demand_indexer),
    )

    eval_perm_backward(perm_specifiers, city_amount, itinerary)
    itinerary.delete()
