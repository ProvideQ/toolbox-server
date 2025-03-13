import cplex
import random
import time
import matplotlib.pyplot as plt
import numpy as np
import sys
from io import StringIO


arg_count = len(sys.argv) - 1
if arg_count != 3:
    raise TypeError(f'This script expects exactly 3 arguments but got {arg_count}: max number of variables, step size, and number of repetitons')

maxNbVars = int(sys.argv[1])
stepSize = int(sys.argv[2])
repetitions = int(sys.argv[3])

def generatePlot(table,):
    """ generates plot with table elements (x,y)
    table: elements are tuples first tuple is title of axes
    """
    xvalues = [ a for (a,_) in table]
    yvalues = [ a for (_,a) in table]
    xlabel = xvalues.pop(0)
    ylabel = yvalues.pop(0)    

    regression = np.poly1d(np.polyfit(xvalues, yvalues, 3))
    plt.plot(xvalues, regression(xvalues))

    plt.plot(xvalues, yvalues, 'o')
    plt.ylabel(ylabel=ylabel)
    plt.xlabel(xlabel=xlabel)

    plt.title(ylabel + " for " + xlabel)

    stringIo = StringIO()
    plt.savefig(stringIo, format='svg')
    plt.close()

    print(stringIo.getvalue())

def generateProblem(numberVars):
    """ returns random MIP Problem
    numberVars: the number if variables and constraints
    """
    pb = cplex.Cplex()
    pb.set_problem_type(pb.problem_type.MILP)

    pb.objective.set_sense(pb.objective.sense.maximize)

    obj = [1 for _ in range(0,numberVars)]
    ub = [random.randint(1,100) for _ in range(0,numberVars)]
    lb = [ -i for i in ub]
    pb.variables.add(obj=obj, lb=lb, ub=ub)

    senses=['L' for _ in range(0,numberVars)]
    rhs = [random.randint(1,10) for _ in range(0,numberVars)]

    lin_expr = [cplex.SparsePair(ind = [i for i in range(0,numberVars)], val = [random.choice([-1,1]) * round(random.randint(1, 10)) for _ in range(0,numberVars)]) for _ in range(0,numberVars)]
    pb.linear_constraints.add(senses=senses, rhs=rhs, lin_expr=lin_expr)

    #print(pb.write_as_string())
    return pb

# The following experiment generates MIP problems and solves them with CPLEX.
# It calculates the time taken by the solver for rising numbers of variables and shows results as plot.

results = [("number of variables","time (sec)")]

for i in [j*stepSize for j in range(0, maxNbVars // stepSize)]:
    totalTime = 0
    for _ in (0,repetitions):
        problem = generateProblem(i)
        problem.set_results_stream(None)
        problem.set_log_stream(None)

        start = time.perf_counter()
        problem.solve()
        end = time.perf_counter()
        totalTime += end - start

        results.append((i,totalTime))

generatePlot(results)
