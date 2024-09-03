from typing import Any

import numpy as np


def normalize_vrp(
    distance_matrix: np.ndarray[Any, np.dtype[np.float64]],
    demand_array: np.ndarray[Any, np.dtype[np.float64]],
    capacity: int,
):
    city_amount = len(distance_matrix)

    est_cluster = estimate_worst_cluster_nr(capacity, demand_array)

    sorted_demand = np.sort(distance_matrix[0])[::-1]
    sorted_distance_matrix = np.sort(distance_matrix)[::-1]
    path_distance_for_going_back = np.sum(sorted_demand[:est_cluster]) * 2
    path_distance_for_normal_path = np.sum(
        sorted_distance_matrix[: int(city_amount - est_cluster / 2)]
    )

    total_path = path_distance_for_going_back + path_distance_for_normal_path

    print(total_path)

    return distance_matrix / total_path, total_path


def estimate_worst_cluster_nr(
    capacity: int, demand_array: np.ndarray[Any, np.dtype[np.float64]]
):
    return int(np.ceil(np.sum(demand_array) / capacity) * 2)


def calc_paths(
    distance_matrix: np.ndarray[Any, np.dtype[np.float64]],
    demand_array: np.ndarray[Any, np.dtype[np.float64]],
    capacity: int,
    perm: list[int],
):
    paths = []
    current_path = [0]
    current_demand = 0

    for i in perm:
        demand = demand_array[i]

        if capacity < current_demand + demand:
            paths.append(current_path)
            current_path = [0]
            current_demand = 0

        current_path.append(i)
        current_demand += demand

    paths.append(current_path)

    total_length = 0.0
    for path in paths:
        path_length = 0.0

        first = path[0]

        last = first
        for next in path[1:]:
            path_length += distance_matrix[(last, next)]
            last = next
        path_length += distance_matrix[(last, first)]
        total_length += path_length

    return total_length, paths
