package edu.kit.provideq.toolbox.maxCut;

import edu.kit.provideq.toolbox.SolveRequest;

/**
 * POST Requests to /solve/max-cut should have a response body of this form.
 * The needed formula the maxCut formula to solve in the GML format.
 */
public class SolveMaxCutRequest extends SolveRequest<String> {}
