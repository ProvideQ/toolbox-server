package edu.kit.provideq.toolbox.featureModel.anomaly;

import edu.kit.provideq.toolbox.SolveRequest;

/**
 * POST Requests to /solve/featureModel should have a response body of this form.
 * The needed formula the featureModel formula to solve in the DIMACS CNF - SAT format.
 */
public class SolveFeatureModelAnomalyRequest extends SolveRequest<String> {}
