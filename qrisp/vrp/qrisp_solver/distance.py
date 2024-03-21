from math import ceil, log
from typing import Any, Tuple

import numpy as np
from qrisp import (
    QuantumArray,
    QuantumBool,
    QuantumDictionary,
    QuantumFloat
)


def qdict_calc_perm_travel_distance_forward(
    itinerary: QuantumArray,
    precision: int,
    city_amount: int,
    distance_matrix: np.ndarray[Any, np.dtype[np.float64]],
    demand: np.ndarray[Any, np.dtype[np.int64]],
    capacity: int,
) -> Tuple[QuantumFloat, QuantumArray, QuantumFloat]:
    
    # A QuantumFloat with n qubits and exponent -n
    # can represent values between 0 and 1
    res = QuantumFloat(precision * 2, -precision)

    # Fill QuantumDictionary with values
    qd = QuantumDictionary(return_type=res)
    for i in range(city_amount):
        for j in range(city_amount):
            qd[i, j] = res.truncate(distance_matrix[i, j])

    qd_to_zero = QuantumDictionary(return_type=res)

    for i in range(city_amount):
        qd_to_zero[i] = res.truncate(distance_matrix[0, i])

    res += qd_to_zero[itinerary[0]]

    # Add the distance of the final trip
    final_trip_distance = qd_to_zero[itinerary[-1]]
    res += final_trip_distance
    final_trip_distance.uncompute(recompute=True)

    bit_number_cap = ceil(log(capacity)) + 1

    demand_count_type = QuantumFloat(bit_number_cap)

    qa = QuantumDictionary(return_type=demand_count_type)
    for i in range(city_amount):
        qa[i] = demand[i]

    demand_indexer = QuantumFloat(1)
    demand_counter = QuantumArray(qtype=demand_count_type)
    demand_counter[:] = [0, 0]
    # print(demand_counter)

    demand_indexer[:] = 0
    with demand_counter[demand_indexer] as demand:
        demand += qa[itinerary[0]]

    # Evaluate result
    for i in range(city_amount - 2):
        demand_index = itinerary[(i + 1) % city_amount]
        # print(demand_index)
        city_demand = qa[demand_index]
        # print(city_demand)

        capped: QuantumBool

        with demand_counter[demand_indexer] as demand:
            demand += city_demand
            capped = demand <= capacity

        # print(capped)
        # print(demand_counter)

        with capped:
            trip_distance = qd[itinerary[i], demand_index]
            res += trip_distance
            trip_distance.uncompute(recompute=True)
        capped.flip()
        # print(capped)
        with capped:
            with demand_counter[demand_indexer] as demand:
                demand -= city_demand
            demand_indexer += 1
            with demand_counter[demand_indexer] as demand:
                demand += city_demand

            long_first_trip_distance = qd_to_zero[itinerary[i]]
            res += long_first_trip_distance
            long_first_trip_distance.uncompute(recompute=True)
            long_second_trip_distance = qd_to_zero[itinerary[(i + 1) % city_amount]]
            res += long_second_trip_distance
            long_second_trip_distance.uncompute(recompute=True)
        # print(demand_counter)
        with demand_counter[demand_indexer] as demand:
            with demand == city_demand:
                capped.flip()
        
        capped.delete() # already verfied once
        city_demand.uncompute()
    
    return res, demand_counter, demand_indexer



def qdict_calc_perm_travel_distance_backward(
    itinerary: QuantumArray,
    precision: int,
    city_amount: int,
    distance_matrix: np.ndarray[Any, np.dtype[np.float64]],
    demand: np.ndarray[Any, np.dtype[np.int64]],
    capacity: int,
    forward_result: Tuple[QuantumFloat, QuantumArray, QuantumFloat],
) -> None:
    
    res, demand_counter, demand_indexer = forward_result

    # Fill QuantumDictionary with values
    qd = QuantumDictionary(return_type=res)
    for i in range(city_amount):
        for j in range(city_amount):
            qd[i, j] = res.truncate(distance_matrix[i, j])

    qd_to_zero = QuantumDictionary(return_type=res)

    for i in range(city_amount):
        qd_to_zero[i] = res.truncate(distance_matrix[0, i])
        

    bit_number_cap = ceil(log(capacity)) + 1

    demand_count_type = QuantumFloat(bit_number_cap)

    qa = QuantumDictionary(return_type=demand_count_type)
    for i in range(city_amount):
        qa[i] = demand[i]

    # uncompute demand_counter
    for i in reversed(range(city_amount - 2)):
        demand_index = itinerary[(i + 1) % city_amount]
        city_demand = qa[demand_index]
        
        was_capped: QuantumBool
        
        with demand_counter[demand_indexer] as demand:
            demand -= city_demand
            was_capped = demand == 0
            
        with was_capped:
            demand_indexer -= 1
            
            long_first_trip_distance = qd_to_zero[itinerary[i]]
            res -= long_first_trip_distance
            long_first_trip_distance.uncompute(recompute=True)
            long_second_trip_distance = qd_to_zero[itinerary[(i + 1) % city_amount]]
            res -= long_second_trip_distance
            long_second_trip_distance.uncompute(recompute=True)
        was_capped.flip()
        with was_capped:
            trip_distance = qd[itinerary[i], demand_index]
            res -= trip_distance
            trip_distance.uncompute(recompute=True)
                
        with demand_counter[demand_indexer] as demand:
            
            added = demand + city_demand
            
            reverse_capped = added <= capacity
                
            added.uncompute(recompute=True)
            
        with reverse_capped:
            was_capped.flip()
            
        reverse_capped.uncompute()
        
        was_capped.delete(verify=True) # verified
        
        city_demand.uncompute()
    
    last_demand = qa[itinerary[0]]
    with demand_counter[demand_indexer] as demand:
        demand -= last_demand
        
    last_demand.uncompute()
        
    demand_indexer.delete() # verified
    demand_counter.delete() # verified
    
    # remove the distance of the first and final trip
    first_trip_distance = qd_to_zero[itinerary[0]]
    res -= first_trip_distance
    first_trip_distance.uncompute(recompute=True)
    
    final_trip_distance = qd_to_zero[itinerary[-1]]
    res -= final_trip_distance
    final_trip_distance.uncompute(recompute=True)
    
    res.delete() # verified
