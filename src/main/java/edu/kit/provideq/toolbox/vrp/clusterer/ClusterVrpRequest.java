package edu.kit.provideq.toolbox.vrp.clusterer;

import edu.kit.provideq.toolbox.SolveRequest;

/**
 * POST Requests to /solve/sat should have a request body of this form.
 * The needed formula is the SAT formula to solve in the DIMACS SAT format.
 */
public class ClusterVrpRequest extends SolveRequest<String> {
  public ClusterVrpRequest() {
    super();
  }
}
