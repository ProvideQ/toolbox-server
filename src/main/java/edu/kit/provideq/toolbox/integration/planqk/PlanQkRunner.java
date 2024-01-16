package edu.kit.provideq.toolbox.integration.planqk;

import edu.kit.provideq.toolbox.RunnerResult;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PlanQkRunner {
  private final PlanQkApi planQkApi;
  private String authenticationToken;

  private ProblemProperties problemProperties;
  private StatusProperties<?> statusProperties;
  private SolutionProperties<?> solutionProperties;

  public PlanQkRunner(PlanQkApi planQkApi) {
    this.planQkApi = planQkApi;
    this.problemProperties = ProblemProperties.defaultProblemProperties();
    this.statusProperties = StatusProperties.defaultStatusProperties();
  }

  public PlanQkRunner addAuthentication(String token) {
    authenticationToken = token;

    return this;
  }

  public PlanQkRunner problemProperties(ProblemProperties problemProperties) {
    this.problemProperties = problemProperties;

    return this;
  }

  public <StatusT> PlanQkRunner statusProperties(StatusProperties<StatusT> statusProperties) {
    this.statusProperties = statusProperties;

    return this;
  }

  /**
   * Set the endpoint to get the result of a job.
   * Must contain %s placeholder for the job ID.
   *
   * @param solutionProperties Solution properties.
   * @return Returns this instance for chaining.
   */
  public <SolutionT> PlanQkRunner solutionProperties(
      SolutionProperties<SolutionT> solutionProperties) {
    this.solutionProperties = solutionProperties;

    return this;
  }

  public <ProblemT, SolutionT> Mono<RunnerResult<SolutionT>> run(
      String service,
      ProblemT problem,
      Class<SolutionT> solutionClass) {
    return planQkApi.solve(
        service,
        problem,
        authenticationToken,
        problemProperties,
        statusProperties,
        solutionProperties == null
            ? SolutionProperties.defaultSolutionProperties(solutionClass)
            : (SolutionProperties<SolutionT>) solutionProperties
    );
  }

  /**
   * Properties for the problem.
   *
   * @param jobEndpoint The endpoint to post a job. Must contain %s placeholder for the job ID.
   */
  public record ProblemProperties(
      String jobEndpoint) {
    public static ProblemProperties defaultProblemProperties() {
      return new ProblemProperties("/jobs");
    }
  }

  /**
   * Properties for the status.
   *
   * @param statusClass       The class of the custom status.
   * @param jobStatusEndpoint The endpoint to get the status of a job.
   *                          Must contain %s placeholder for the job ID.
   * @param statusMapper      Maps the custom status to a status of the PlanQK API.
   * @param <StatusT>         The type of the custom status.
   */
  public record StatusProperties<StatusT>(
      Class<StatusT> statusClass,
      String jobStatusEndpoint,
      Function<StatusT, PlanQkApi.JobStatus> statusMapper) {
    public static StatusProperties<PlanQkApi.JobInfo> defaultStatusProperties() {
      return new StatusProperties<>(
          PlanQkApi.JobInfo.class,
          "/jobs/%s",
          jobStatus -> jobStatus.status);
    }
  }

  /**
   * Properties for the solution.
   *
   * @param jobResultsClass    The class of the custom result.
   * @param jobResultsEndpoint The endpoint to get the result of a job.
   */
  public record SolutionProperties<SolutionT>(
      @Nullable
      Class<SolutionT> jobResultsClass,
      String jobResultsEndpoint) {

    private static <SolutionT> SolutionProperties<SolutionT> defaultSolutionProperties(
        Class<SolutionT> solutionClass) {
      return new SolutionProperties<>(null, "/jobs/%s/results");
    }
  }
}
