package edu.kit.provideq.toolbox.integration.planqk;

import edu.kit.provideq.toolbox.integration.planqk.exception.PlanQkJobFailedException;
import edu.kit.provideq.toolbox.integration.planqk.exception.PlanQkJobPendingException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class PlanQkApi {
  private static final String BASE_URL = "https://gateway.platform.planqk.de";

  /**
   * Calls a service using the PlanQk API.
   *
   * @param <ProblemT>          The type of the problem.
   * @param <StatusT>           The type of the status.
   * @param <ResultT>           The type of the result.
   * @param service             The url path of the service to use.
   * @param problem             The data of the problem to solve.
   * @param authenticationToken The authentication token to use.
   * @param problemProperties   The properties of the problem.
   * @param statusProperties    The properties of the status.
   * @param resultProperties    The properties of the result.
   * @return The result to the problem.
   */
  public <ProblemT, StatusT, ResultT> Mono<ResultT> call(
      String service,
      ProblemT problem,
      String authenticationToken,
      ProblemProperties problemProperties,
      StatusProperties<StatusT> statusProperties,
      ResultProperties<ResultT> resultProperties) {
    var webClientBuilder = WebClient.builder();
    if (authenticationToken != null) {
      webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationToken);
    }

    WebClient webClient = webClientBuilder.build();

    // Define the URL for the token endpoint
    final String serviceEndpoint = BASE_URL + service;

    // Make the POST request to obtain the access token with client_credentials grant type
    return webClient.post()
        .uri(serviceEndpoint + problemProperties.createJobEndpoint())
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(problem))
        .retrieve()
        .bodyToMono(String.class)
        .flatMap(jobId -> webClient.get()
            .uri(serviceEndpoint + statusProperties.jobStatusEndpoint().formatted(jobId))
            .exchangeToMono(response -> {
              // Handle different kinds of status responses
              // Usually the JobStatus is returned as json object
              // but some might use just an enum in plain text
              MediaType contentType = response.headers().contentType().orElseThrow();
              if (MediaType.TEXT_PLAIN.equals(contentType)) {
                return response.bodyToMono(String.class)
                    .map(status -> parseEnumStatus(statusProperties, status));
              }

              // Otherwise, assume the content type is application/json
              return response.bodyToMono(statusProperties.statusClass());
            })
            .map(customStatus -> statusProperties.statusMapper().apply(customStatus))
            .flatMap(status -> {
              switch (status) {
                case SUCCEEDED -> {
                  return Mono.just(status);
                }
                case FAILED -> {
                  return Mono.error(new PlanQkJobFailedException());
                }
                default -> {
                  return Mono.error(new PlanQkJobPendingException());
                }
              }
            })
            .retryWhen(Retry.backoff(100, Duration.ofSeconds(1))
                .filter(PlanQkJobPendingException.class::isInstance))
            .flatMap(succeededJobStatus ->
                // The job finished successfully, so get the result
                webClient.get()
                    .uri(serviceEndpoint
                        + resultProperties.jobResultsEndpoint().formatted(jobId))
                    .retrieve()
                    .bodyToMono(resultProperties.jobResultsClass())
            )
        );
  }

  private static <StatusT> StatusT parseEnumStatus(
      StatusProperties<StatusT> statusProperties,
      String status) throws IllegalArgumentException {
    if (!statusProperties.statusClass().isEnum()) {
      throw new IllegalArgumentException("Not an enum: " + statusProperties.statusClass());
    }

    return Arrays.stream(statusProperties
            .statusClass()
            .getEnumConstants())
        .filter(enumValue -> enumValue.toString().equalsIgnoreCase(status))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid status: " + status));
  }

  public enum JobStatus {
    UNKNOWN,
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
  }

  public static class JobInfo {
    private String id;
    private JobStatus status;
    private Date createdAt;
    private Date startedAt;
    private Date endedAt;

    /**
     * Job ID.
     */
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    /**
     * Job status.
     */
    public JobStatus getStatus() {
      return status;
    }

    public void setStatus(JobStatus status) {
      this.status = status;
    }

    /**
     * Date of creation.
     */
    public Date getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
    }

    /**
     * Date of starting.
     */
    public Date getStartedAt() {
      return startedAt;
    }

    public void setStartedAt(Date startedAt) {
      this.startedAt = startedAt;
    }

    /**
     * Date of ending.
     */
    public Date getEndedAt() {
      return endedAt;
    }

    public void setEndedAt(Date endedAt) {
      this.endedAt = endedAt;
    }
  }

  /**
   * Properties for the problem.
   *
   * @param createJobEndpoint The POST endpoint to create a job.
   *                          Must contain %s placeholder for the job ID.
   */
  public record ProblemProperties(
      String createJobEndpoint) {
    public static PlanQkApi.ProblemProperties defaultProblemProperties() {
      return new PlanQkApi.ProblemProperties("/jobs");
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
      Function<StatusT, JobStatus> statusMapper) {
    public static PlanQkApi.StatusProperties<JobInfo> defaultStatusProperties() {
      return new PlanQkApi.StatusProperties<>(
          PlanQkApi.JobInfo.class,
          "/jobs/%s",
          PlanQkApi.JobInfo::getStatus);
    }
  }

  /**
   * Properties for the result.
   *
   * @param jobResultsClass    The class of the custom result.
   * @param jobResultsEndpoint The endpoint to get the result of a job.
   */
  public record ResultProperties<ResultT>(
      Class<ResultT> jobResultsClass,
      String jobResultsEndpoint) {
    public static <ResultT> PlanQkApi.ResultProperties<ResultT> defaultResultProperties(
        Class<ResultT> resultClass) {
      return new PlanQkApi.ResultProperties<>(resultClass, "/jobs/%s/results");
    }
  }
}
