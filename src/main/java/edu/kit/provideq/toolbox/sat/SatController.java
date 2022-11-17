package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.meta.ProblemType;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.meta.MetaSolver;
import edu.kit.provideq.toolbox.meta.MetaSolverSAT;
import edu.kit.provideq.toolbox.meta.Problem;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.validation.Valid;

import edu.kit.provideq.toolbox.sat.convert.SATSolver;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SatController {
  private final AtomicLong nextId = new AtomicLong();
  private final List<Solution<String>> solutions = new LinkedList<>();

  private final MetaSolver<SATSolver> metaSolver = new MetaSolverSAT();

  @PostMapping("/solve/sat")
  public SolutionHandle solveSat(@RequestBody @Valid SolveSatRequest request) {
    var id = nextId.incrementAndGet();
    var status = SolutionStatus.COMPUTING;
    Solution<String> solution = new Solution<>(id);
    solutions.add(solution);

    SolutionHandle solutionHandle = new SolutionHandle(id, status);

    Problem<String> problem = new Problem<>(request.formula(), ProblemType.SAT);

    SATSolver satSolver = metaSolver.findSolver(problem);
    satSolver.solve(problem, solution);

    return solutionHandle;
  }

  @GetMapping("/solve/sat")
  public SolutionHandle getSolution(@RequestParam(name = "id", required = true) long id) {
    var solution = solutions.stream()
        .filter(s -> s.getId() == id)
        .findFirst()
        .orElse(null);
    if (solution == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          String.format("Unable to find solution process with id %d", id));
    }

    // for demonstration purposes, jobs are marked as solved once a request goes in here
    if (solution.getStatus() == SolutionStatus.COMPUTING) solution.complete();

    return new SolutionHandle(id, solution.getStatus());
  }
}
