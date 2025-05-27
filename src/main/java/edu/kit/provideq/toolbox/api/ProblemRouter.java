package edu.kit.provideq.toolbox.api;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.google.common.collect.Streams;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import edu.kit.provideq.toolbox.meta.ProblemManagerProvider;
import edu.kit.provideq.toolbox.meta.ProblemState;
import edu.kit.provideq.toolbox.meta.ProblemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * This router handles requests regarding {@link Problem} instances.
 */
@Configuration
@EnableWebFlux
public class ProblemRouter {
  private static final String PROBLEMS_BASE_PATH = "/problems";
  public static final String PROBLEM_ID_PARAM_NAME = "problemId";
  private ProblemManagerProvider managerProvider;
  private Validator validator;

  @Bean
  RouterFunction<ServerResponse> getProblemRoutes() {
    var managers = this.managerProvider.getProblemManagers();
    return Streams.concat(
            managers.stream().map(this::defineCreateRoute),
            managers.stream().map(this::defineReadRoute),
            managers.stream().map(this::defineListRoute),
            managers.stream().map(this::defineUpdateRoute)
        )
        .reduce(RouterFunction::and)
        .orElseThrow()
        .and(defineListTypesRoute());
  }

  /**
   * Create operation: GET /problems.
   */
  private RouterFunction<ServerResponse> defineListTypesRoute() {
    return route().POST(
        PROBLEMS_BASE_PATH,
        accept(APPLICATION_JSON),
        req -> handleListTypes(),
        ops -> ProblemRouteDocumentation.configureListTypesDocs(managerProvider, ops)
    ).build();
  }

  /**
   * Create operation: POST /problems/TYPE.
   */
  private RouterFunction<ServerResponse> defineCreateRoute(ProblemManager<?, ?> manager) {
    return route().POST(
        getPathWithoutId(manager.getType()),
        accept(APPLICATION_JSON),
        req -> handleCreate(manager, req),
        ops -> ProblemRouteDocumentation.configureCreateDocs(manager, ops)
    ).build();
  }

  /**
   * Read operation: GET /problems/TYPE/ID.
   */
  private RouterFunction<ServerResponse> defineReadRoute(ProblemManager<?, ?> manager) {
    return route().GET(
        getPathWithId(manager.getType()),
        accept(APPLICATION_JSON),
        req -> handleRead(manager, req),
        ops -> ProblemRouteDocumentation.configureReadDocs(manager, ops)
    ).build();
  }

  /**
   * List operation: GET /problems/TYPE.
   */
  private RouterFunction<ServerResponse> defineListRoute(ProblemManager<?, ?> manager) {
    return route().GET(
        getPathWithoutId(manager.getType()),
        accept(APPLICATION_JSON),
        req -> handleList(manager),
        ops -> ProblemRouteDocumentation.configureListDocs(manager, ops)
    ).build();
  }

  /**
   * Update operation: PATCH /problems/TYPE/ID.
   */
  private RouterFunction<ServerResponse> defineUpdateRoute(ProblemManager<?, ?> manager) {
    return route().PATCH(
        getPathWithId(manager.getType()),
        accept(APPLICATION_JSON),
        req -> handleUpdate(manager, req),
        ops -> ProblemRouteDocumentation.configureUpdateDocs(manager, ops)
    ).build();
  }

  private Mono<ServerResponse> handleListTypes() {
    var problemTypes = managerProvider.getProblemManagers().stream()
        .map(ProblemManager::getType)
        .map(ProblemTypeDto::fromProblemType)
        .toList();

    return ok().body(Mono.just(problemTypes), new ParameterizedTypeReference<>() {
    });
  }

  private <InputT, ResultT> Mono<ServerResponse> handleCreate(
      ProblemManager<InputT, ResultT> manager,
      ServerRequest req
  ) {
    var createdProblemDto = req
        .bodyToMono(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {})
        .doOnNext(this::validate)
        .map(submittedDto -> {
          var problem = new Problem<>(manager.getType());
          applySubmittedProblemPatch(manager, problem, submittedDto);
          manager.addInstance(problem);
          return problem;
        })
        .map(ProblemDto::fromProblem);

    return ok().body(createdProblemDto, new ParameterizedTypeReference<>(){});
  }

  private <InputT, ResultT> Mono<ServerResponse> handleRead(
      ProblemManager<InputT, ResultT> manager,
      ServerRequest req
  ) {
    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = RouterUtility.findProblemOrThrow(manager, problemId);
    var problemDto = ProblemDto.fromProblem(problem);
    return ok().body(Mono.just(problemDto), new ParameterizedTypeReference<>() {});
  }

  private <InputT, ResultT> Mono<ServerResponse> handleList(
      ProblemManager<InputT, ResultT> manager
  ) {
    var problemList = manager.getInstances().stream()
        .map(ProblemDto::fromProblem)
        .toList();
    return ok().body(Mono.just(problemList), new ParameterizedTypeReference<>() {});
  }

  private <InputT, ResultT> Mono<ServerResponse> handleUpdate(
      ProblemManager<InputT, ResultT> manager,
      ServerRequest req
  ) {
    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = RouterUtility.findProblemOrThrow(manager, problemId);
    if (problem.getState() == ProblemState.SOLVING || problem.getState() == ProblemState.SOLVED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Problems that are currently being solved or have been solved cannot be updated!");
    }

    var updatedProblemDto = req
        .bodyToMono(new ParameterizedTypeReference<ProblemDto<InputT, ResultT>>() {})
        .doOnNext(this::validate)
        .map(patchDto -> {
          applySubmittedProblemPatch(manager, problem, patchDto);
          return problem;
        })
        .map(ProblemDto::fromProblem);

    return ok().body(updatedProblemDto, new ParameterizedTypeReference<>() {});
  }

  private <InputT, ResultT> void applySubmittedProblemPatch(
      ProblemManager<InputT, ResultT> manager,
      Problem<InputT, ResultT> problem,
      ProblemDto<InputT, ResultT> patch
  ) {
    // update solver first as this can fail and we want to avoid partial updates
    if (patch.getSolverId() != null) {
      if (patch.getSolverId().isEmpty()) {
        problem.setSolver(null);
      } else {
        var solver = manager.findSolverById(patch.getSolverId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "The provided solver ID is invalid!"));

        problem.setSolver(solver);
      }
    }

    var solverSettings = patch.getSolverSettings();
    if (solverSettings != null && !solverSettings.isEmpty()) {
      problem.setSolverSettings(solverSettings);
    }

    if (patch.getInput() != null) {
      problem.setInput(patch.getInput());
    }

    // state must be changed at last as this might trigger other processes
    if (patch.getState() == ProblemState.SOLVING) {
      problem.solve().subscribe();
    }
  }

  private <InputT, ResultT> void validate(ProblemDto<InputT, ResultT> problem) {
    var errors = new BeanPropertyBindingResult(problem, "problem");
    validator.validate(problem, errors);
    if (errors.hasErrors()) {
      throw new ServerWebInputException(errors.toString());
    }
  }

  private String getPathWithoutId(ProblemType<?, ?> type) {
    return "%s/%s".formatted(PROBLEMS_BASE_PATH, type.getId());
  }

  private String getPathWithId(ProblemType<?, ?> type) {
    var pathWithoutId = getPathWithoutId(type);
    return "%s/{%s}".formatted(pathWithoutId, PROBLEM_ID_PARAM_NAME);
  }

  @Autowired
  void setManagerProvider(ProblemManagerProvider managerProvider) {
    this.managerProvider = managerProvider;
  }

  @Autowired
  void setValidator(Validator validator) {
    this.validator = validator;
  }
}
