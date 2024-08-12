# Knapsack solver using the knapsack-pip library

import sys
from knapsack01 import HSKnapsack

if len(sys.argv) != 3:
    raise TypeError('This script expects exactly 2 arguments. Input file (argument 1) and output file (argument 2).')

input_path = sys.argv[1]
output_path = sys.argv[2]

with open(input_path, 'r') as input_file:
    lines = input_file.readlines()

# first line gives number of items
number_items : int = int(lines[0])

profits: list[int] = []
weights: list[int] = []

# read items into profits, weights lists
for i in range(number_items):
    item = lines[i + 1].split(' ')
    profits.append(int(item[1]))
    weights.append(int(item[2]))

# last line contains maximum capacity of the knapsack
capacity: int = int(lines[-1])

# let the library solve the problem ^^
knapsack = HSKnapsack(capacity, profits, weights)
max_profit, max_solution = knapsack.maximize()

# max_solution only contains indicator whether item is present, collect items for solutions in a list
items_in_solution: list[list[int]] = []
for i in range(number_items):
    if max_solution[i] == 1:
        items_in_solution.append([profits[i], weights[i]])

# returning the solution as a single line with the maximum profit
# followed by the items present represented by profit/weight
with open(output_path, 'w') as output_file:
    output_file.write(str(max_profit) + '\n')
    for item in items_in_solution:
        output_file.write(f"{item[0]} {item[1]}\n")
