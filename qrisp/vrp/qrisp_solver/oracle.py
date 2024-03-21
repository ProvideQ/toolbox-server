from qrisp import *

from .distance import qdict_calc_perm_travel_distance_forward, qdict_calc_perm_travel_distance_backward
from .permutation import eval_perm



def eval_distance_threshold(
    perm_specifiers,
    precision,
    threshold,
    city_amount,
    distance_matrix,
    city_demand,
    max_cap,
):
    itinerary = eval_perm(perm_specifiers, city_amount=city_amount)
    
    distance, demand_array, demand_indexer = qdict_calc_perm_travel_distance_forward(itinerary, precision, city_amount, distance_matrix, city_demand, max_cap)
    
    is_below_treshold = distance <= threshold

    z(is_below_treshold)
    
    with distance <= threshold:
        is_below_treshold.flip()
    
    qdict_calc_perm_travel_distance_backward(itinerary, precision, city_amount, distance_matrix, city_demand, max_cap, forward_result=(distance, demand_array, demand_indexer))
    
    itinerary.uncompute()
    
    
