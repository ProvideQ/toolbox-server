package edu.kit.provideq.toolbox.sat;

import edu.kit.provideq.toolbox.SolutionHandle;
import edu.kit.provideq.toolbox.SolutionStatus;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.validation.Valid;
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
  private final ConcurrentMap<Long, SolutionStatus> solutionStatuses = new ConcurrentHashMap<>();

  @PostMapping("/solve/sat")
  public SolutionHandle solveSat(@RequestBody @Valid SolveSatRequest request) {
    var id = nextId.incrementAndGet();
    var status = SolutionStatus.COMPUTING;
    solutionStatuses.put(id, status);
    return new SolutionHandle(id, status);
  }

  @GetMapping("/solve/sat")
  public SolutionHandle getSolution(@RequestParam(name = "id", required = true) long id) {
    var currentStatus = solutionStatuses.get(id);
    if (currentStatus == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          String.format("Unable to find solution process with id %d", id));
    }

    // for demonstration purposes, jobs are marked as solved once a request goes in here
    if (currentStatus == SolutionStatus.COMPUTING) {
      currentStatus = SolutionStatus.SOLVED;
      solutionStatuses.put(id, currentStatus);
    }

    return new SolutionHandle(id, currentStatus);
  }
}
