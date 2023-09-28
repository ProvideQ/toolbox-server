package edu.kit.provideq.toolbox.featuremodel;

import edu.kit.provideq.toolbox.SolveRequest;

/**
 * POST Requests to /solve/featureModel should have a request body of this form.
 * The needed formula the featureModel formula to solve in the DIMACS CNF - SAT format.
 */
public class SolveFeatureModelRequest extends SolveRequest<String> {
}
