//package edu.kit.provideq.toolbox.meta;
//
//import edu.kit.provideq.toolbox.Solution;
//import edu.kit.provideq.toolbox.SolveRequest;
//import edu.kit.provideq.toolbox.SubRoutinePool;
//import edu.kit.provideq.toolbox.meta.setting.MetaSolverSetting;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//
//  public Solution<SolutionT> solve(SolveRequest<ProblemT> request) {
//    Solution<SolutionT> solution = this.getSolutionManager().createSolution();
//    Problem<ProblemT> problem = new Problem<>(request.requestContent, this.getProblemType());
//
//    SolverT solver = this
//            .getSolver(request.requestedSolverId)
//            .orElseGet(() -> this.findSolver(problem, request.requestedMetaSolverSettings));
//
//    solution.setSolverName(solver.getName());
//
//    SubRoutinePool subRoutinePool =
//            request.requestedSubSolveRequests == null
//                    ? context.getBean(SubRoutinePool.class)
//                    : context.getBean(SubRoutinePool.class, request.requestedSubSolveRequests);
//
//    long start = System.currentTimeMillis();
//    solver.solve(problem, solution, subRoutinePool);
//    long finish = System.currentTimeMillis();
//
//    solution.setExecutionMilliseconds(finish - start);
//
//    return solution;
//  }
