package edu.kit.provideq.toolbox.integration.planqk;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.provideq.toolbox.RunnerResult;
import edu.kit.provideq.toolbox.exception.AuthenticationException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import javax.annotation.Nullable;
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

  private Date expirationDate = Date.from(Instant.now());

  @Nullable
  private String accessToken;

  public String getAccessToken() throws AuthenticationException {
    // Return early if token is available and still valid
    if (accessToken != null && expirationDate.after(Date.from(Instant.now()))) {
      return accessToken;
    }

    return createToken("", "");
  }

  private String createToken(String key, String secret) throws AuthenticationException {
    WebClient webClient = WebClient.builder()
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + getBasicAuth(key, secret))
        .build();

    // Define the URL for the token endpoint
    final String tokenEndpoint = BASE_URL + "/token";

    // Make the POST request to obtain the access token with client_credentials grant type
    TokenResponse tokenResponse = webClient.post()
        .uri(tokenEndpoint)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
        .retrieve()
        .bodyToMono(TokenResponse.class)
        .block();

    if (tokenResponse == null || tokenResponse.accessToken == null) {
      throw new AuthenticationException("Could not obtain access token");
    }

    expirationDate = Date.from(Instant.now().plus(tokenResponse.expiresIn, ChronoUnit.SECONDS));
    accessToken = tokenResponse.accessToken;

    return accessToken;
  }

  private static String getBasicAuth(String clientId, String clientSecret) {
    String auth = clientId + ":" + clientSecret;
    byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
    return new String(encodedAuth, StandardCharsets.UTF_8);
  }

  /**
   * Solves a problem using the PlanQk API.
   *
   * @param <ProblemT>          The type of the problem.
   * @param <SolutionT>         The type of the solution.
   * @param service             The url path of the service to use.
   * @param problem             The data of the problem to solve.
   * @param authenticationToken The authentication token to use.
   * @param problemProperties   The properties of the problem.
   * @param statusProperties    The properties of the status.
   * @param solutionProperties  The properties of the solution.
   * @return The solution to the problem.
   */
  public <ProblemT, SolutionT, StatusT> Mono<RunnerResult<SolutionT>> solve(
      String service,
      ProblemT problem,
      String authenticationToken,
      PlanQkRunner.ProblemProperties problemProperties,
      PlanQkRunner.StatusProperties<StatusT> statusProperties,
      PlanQkRunner.SolutionProperties<SolutionT> solutionProperties) {
    var webClientBuilder = WebClient.builder();
    if (authenticationToken != null) {
      webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authenticationToken);
    }

    WebClient webClient = webClientBuilder.build();

    // Define the URL for the token endpoint
    final String serviceEndpoint = BASE_URL + service;

    // Make the POST request to obtain the access token with client_credentials grant type
    return webClient.post()
        .uri(serviceEndpoint + problemProperties.jobEndpoint())
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
                    .flatMap(status -> {
                      if (!statusProperties.statusClass().isEnum()) {
                        return Mono.error(new IllegalArgumentException("Not an enum: "
                            + statusProperties.statusClass()));
                      }

                      StatusT[] enumConstants = statusProperties.statusClass().getEnumConstants();
                      for (StatusT enumConstant : enumConstants) {
                        if (enumConstant.toString().equalsIgnoreCase(status)) {
                          return Mono.just(enumConstant);
                        }
                      }
                      return Mono.error(new IllegalArgumentException("Invalid status: " + status));
                    });
              } else {
                // Assume the content type is application/json
                return response.bodyToMono(statusProperties.statusClass());
              }
            })
            .flatMap(customStatus -> {
              // Handle different kinds of status responses
              // Use mapping to default JobStatus enum
              JobStatus status = statusProperties.statusMapper().apply(customStatus);

              switch (status) {
                case SUCCEEDED -> {
                  return Mono.just(status);
                }
                case PENDING, RUNNING -> {
                  return Mono.error(new RuntimeException("The job is still pending."));
                }
                case FAILED -> {
                  return Mono.error(
                      new RuntimeException("The job terminated without a solution."));
                }
                default -> {
                  return Mono.error(
                      new RuntimeException("The job status could not be determined."));
                }
              }
            })
            .retryWhen(Retry.backoff(100, Duration.ofSeconds(1)))
            .flatMap(x -> {
              // The job finished successfully, so get the solution
              Class<SolutionT> solutionClass = solutionProperties.jobResultsClass();

              var webCall = webClient.get()
                  .uri(serviceEndpoint
                      + solutionProperties.jobResultsEndpoint().formatted(jobId))
                  .retrieve();

              // Handle conversion into different kinds of solution responses, custom and default
              Mono<SolutionT> solutionMono;
              if (solutionClass == null) {
                var resultClass =
                    (Class<? extends JobResult<SolutionT>>) new JobResult<SolutionT>().getClass();

                solutionMono = webCall.bodyToMono(resultClass)
                    .map(r -> r.result);
              } else {
                solutionMono = webCall.bodyToMono(solutionClass);
              }

              return solutionMono
                  .map(solution -> {
                    if (solution == null) {
                      return new RunnerResult<>(false, null, "The job result format is invalid.");
                    }

                    return new RunnerResult<>(true, solution, "The job finished successfully.");
                  });
            }));
  }

  public enum JobStatus {
    UNKNOWN,
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
  }

  private static class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    private String scope;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;
  }

  public static class JobInfo {
    /**
     * Job ID.
     */
    public String id;

    /**
     * Job status.
     */
    public JobStatus status;

    /**
     * Date of creation.
     */
    public Date createdAt;
  }

  public static class JobResult<SolutionT> {
    /**
     * Service-specific result object.
     */
    public SolutionT result;

    /**
     * Service-specific metadata object which contains
     * additional information besides the actual results.
     */
    public Object metadata;

    /**
     * Service-specific error code representing the type of problem encountered.
     */
    public String code;

    /**
     * Service-specific error message describing the detail of the problem encountered.
     */
    public String detail;
  }
}
