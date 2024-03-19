package edu.kit.provideq.toolbox.api;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.google.common.collect.Streams;
import edu.kit.provideq.toolbox.ProblemManager;
import edu.kit.provideq.toolbox.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemSolver;
import edu.kit.provideq.toolbox.meta.TypedProblemType;
import edu.kit.provideq.toolbox.test.Problem;
import edu.kit.provideq.toolbox.test.ProblemDto;
import edu.kit.provideq.toolbox.test.ProblemState;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * This router handles problem-solving requests to the GET and POST {@code /solve/{problemType}}
 * endpoints.
 * Requests are validated and relayed to the corresponding {@link ProblemSolver}.
 */
@Configuration
@EnableWebFlux
public class ProblemRouter {
  public static final String PROBLEM_ID_PARAM_NAME = "problemId";
  private final ProblemManagerProvider problemManagerProvider;
  private final Validator validator;

  /**
   * Constructor to be called by dependency injector.
   */
  public ProblemRouter(
      ProblemManagerProvider problemManagerProvider,
      Validator validator
  ) {
    this.problemManagerProvider = problemManagerProvider;
    this.validator = validator;
  }

  /**
   * Register routes.
   */
  @Bean
  RouterFunction<ServerResponse> getCreateProblemRoutes() {
    var managers = problemManagerProvider.getProblemManagers();
    return Streams.concat(
        managers.stream().map(this::defineCreateRoute),
        managers.stream().map(this::defineListRoute),
        managers.stream().map(this::defineReadRoute),
        managers.stream().map(this::defineUpdateRoute)
    ).reduce(RouterFunction::and).orElseThrow();
  }

  /**
   * Create operation: POST /problem/TYPE.
   */
  private RouterFunction<ServerResponse> defineCreateRoute(
      ProblemManager<?, ?> problemManager
  ) {
    return route().POST(
        getPathWithoutId(problemManager.getProblemType()),
        accept(APPLICATION_JSON),
        req -> handleCreate(problemManager, req),
        ops -> ProblemDocumentation.configureCreateDocs(problemManager, ops)
    ).build();
  }

  /**
   * GET /problem/TYPE/ID.
   */
  private RouterFunction<ServerResponse> defineReadRoute(
      ProblemManager<?, ?> problemManager) {
    var problemType = problemManager.getProblemType();
    return route().GET(
        getPathWithId(problemType),
        accept(APPLICATION_JSON),
        req -> handleReadRoute(problemManager, req),
        ops -> ProblemDocumentation.configureReadDocs(problemManager, ops)
    ).build();
  }

  /**
   * List operation: GET /problem/TYPE.
   */
  private RouterFunction<ServerResponse> defineListRoute(ProblemManager<?, ?> problemManager) {
    var type = problemManager.getProblemType();
    return route().GET(
        getPathWithoutId(type),
        accept(APPLICATION_JSON),
        req -> handleListRoute(problemManager, req),
        ops -> ProblemDocumentation.configureListDocs(problemManager, ops)
    ).build();
  }

  /**
   * Update operation: PATCH /problems/TYPE/ID.
   */
  private RouterFunction<ServerResponse> defineUpdateRoute(
      ProblemManager<?, ?> problemManager) {
    var type = problemManager.getProblemType();
    return route().PATCH(
        getPathWithId(type),
        accept(APPLICATION_JSON),
        req -> handleUpdateRoute(problemManager, req),
        ops -> ProblemDocumentation.configureUpdateDocs(problemManager, ops)
    ).build();
  }

  private <InputT, ResultT> Mono<ServerResponse> handleUpdateRoute(
      ProblemManager<InputT, ResultT> problemManager,
      ServerRequest req
  ) {

    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = findProblemOrThrow(problemManager, problemId);

    var patchedProblem = req
        .bodyToMono(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {})
        .doOnNext(this::validate)
        .map(problemPatch -> {
          applySubmittedProblemPatch(problemManager, problem, problemPatch);
          return problem;
        })
        .map(ProblemDto::fromProblem);

    return ok().body(patchedProblem, new ParameterizedTypeReference<>() {});
  }

  private <InputT, ResultT> Mono<ServerResponse> handleCreate(
      ProblemManager<InputT, ResultT> problemManager,
      ServerRequest req
  ) {
    var problemDto = req
        .bodyToMono(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {})
        .doOnNext(this::validate)
        .map(submittedProblemDto -> {
          var problem = new Problem<>(problemManager.getProblemType());
          applySubmittedProblemPatch(problemManager, problem, submittedProblemDto);
          problemManager.addProblem(problem);
          return problem;
        })
        .map(ProblemDto::fromProblem);

    return ok().body(problemDto, new ParameterizedTypeReference<>() {});
  }

  private <InputT, ResultT> Mono<ServerResponse> handleListRoute(
      ProblemManager<InputT, ResultT> problemManager,
      ServerRequest req
  ) {
    return ok().body(
        Mono.just(problemManager.getProblems()),
        new ParameterizedTypeReference<>() {}
    );
  }

  private <InputT, ResultT> Mono<ServerResponse> handleReadRoute(
      ProblemManager<InputT, ResultT> problemManager,
      ServerRequest req
  ) {
    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = findProblemOrThrow(problemManager, problemId);
    var dto = ProblemDto.fromProblem(problem);
    return ok().body(Mono.just(dto), new ParameterizedTypeReference<>() {});
  }

  private <InputT, ResultT> Problem<InputT, ResultT> findProblemOrThrow(
      ProblemManager<InputT, ResultT> problemManager,
      String id
  ) {
    UUID uuid;
    try {
      uuid = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid problem ID");
    }

    return problemManager.getProblemById(uuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a problem for this type with this problem id!"));
  }

  private <InputT, ResultT> void applySubmittedProblemPatch(
      ProblemManager<InputT, ResultT> problemManager,
      Problem<InputT, ResultT> problem,
      ProblemDto<InputT, ResultT> patch
  ) {
    // update solver first as this can fail and we want to avoid partial updates
    if (patch.getSolverId() != null) {
      var solver = problemManager.getProblemSolverById(patch.getSolverId())
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "The provided solver ID is invalid!"));

      problem.setSolver(solver);
    }

    if (patch.getInput() != null) {
      problem.setInput(patch.getInput());
    }

    // state must be changed at last as this might trigger other processes
    if (patch.getState() == ProblemState.SOLVING) {
      // TODO this can be done better
      problem.solve().subscribe();
    }
  }

  private <RequestT, ResultT> void validate(ProblemDto<RequestT, ResultT> request) {
    Errors errors = new BeanPropertyBindingResult(request, "problem");
    validator.validate(request, errors);
    if (errors.hasErrors()) {
      throw new ServerWebInputException(errors.toString());
    }
  }

  private String getPathWithoutId(TypedProblemType<?, ?> type) {
    return "/problems/%s".formatted(type.getId());
  }

  private String getPathWithId(TypedProblemType<?, ?> type) {
    return "/problems/%s/{%s}".formatted(type.getId(), PROBLEM_ID_PARAM_NAME);
  }
}
