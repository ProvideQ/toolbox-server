package edu.kit.provideq.toolbox.api.router;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import com.google.common.collect.Streams;
import edu.kit.provideq.formats.core.FormattedValue;
import edu.kit.provideq.formats.core.converters.ConversionRegistry;
import edu.kit.provideq.formats.core.converters.ConversionService;
import edu.kit.provideq.formats.core.exceptions.ParseException;
import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.api.dto.ProblemDto;
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
  private ConversionRegistry conversionRegistry;
  private ConversionService conversionService;

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
        .orElseThrow();
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

  private <InT extends FormatFamily, OutT extends FormatFamily> Mono<ServerResponse> handleCreate(
      ProblemManager<InT, OutT> manager,
      ServerRequest req
  ) {
    var createdProblemDto = req
        .bodyToMono(new ParameterizedTypeReference<ProblemDto>() {})
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

  private <InT extends FormatFamily, OutT extends FormatFamily> Mono<ServerResponse> handleRead(
      ProblemManager<InT, OutT> manager,
      ServerRequest req
  ) {
    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = RouterUtility.findProblemOrThrow(manager, problemId);
    var problemDto = ProblemDto.fromProblem(problem);
    return ok().body(Mono.just(problemDto), new ParameterizedTypeReference<>() {});
  }

  private <InT extends FormatFamily, OutT extends FormatFamily> Mono<ServerResponse> handleList(
      ProblemManager<InT, OutT> manager
  ) {
    var problemList = manager.getInstances().stream()
        .map(ProblemDto::fromProblem)
        .toList();
    return ok().body(Mono.just(problemList), new ParameterizedTypeReference<>() {});
  }

  private <InT extends FormatFamily, OutT extends FormatFamily> Mono<ServerResponse> handleUpdate(
      ProblemManager<InT, OutT> manager,
      ServerRequest req
  ) {
    var problemId = req.pathVariable(PROBLEM_ID_PARAM_NAME);
    var problem = RouterUtility.findProblemOrThrow(manager, problemId);
    if (problem.getState() == ProblemState.SOLVING || problem.getState() == ProblemState.SOLVED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Problems that are currently being solved or have been solved cannot be updated!");
    }

    var updatedProblemDto = req
        .bodyToMono(new ParameterizedTypeReference<ProblemDto>() {})
        .doOnNext(this::validate)
        .map(patchDto -> {
          applySubmittedProblemPatch(manager, problem, patchDto);
          return problem;
        })
        .map(ProblemDto::fromProblem);

    return ok().body(updatedProblemDto, new ParameterizedTypeReference<>() {});
  }

  private <InT extends FormatFamily, OutT extends FormatFamily> void applySubmittedProblemPatch(
      ProblemManager<InT, OutT> manager,
      Problem<InT, OutT> problem,
      ProblemDto patch
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
      problem.setInput(parseSubmittedInput(manager.getType(), patch));
    }

    // state must be changed at last as this might trigger other processes
    if (patch.getState() == ProblemState.SOLVING) {
      problem.solve(conversionService).subscribe();
    }
  }

  /**
   * Parses the submitted input text as the declared input format.
   * The declared format must be a member of the problem type's input family.
   */
  private <InT extends FormatFamily, OutT extends FormatFamily> FormattedValue<?>
      parseSubmittedInput(ProblemType<InT, OutT> type, ProblemDto patch) {
    if (patch.getInputFormat() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "The input format must be declared for problem input!");
    }

    var format = conversionRegistry.getFormat(patch.getInputFormat());
    if (format == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Unknown input format: %s".formatted(patch.getInputFormat()));
    }

    var familyFormats = conversionRegistry.getFormats(type.getInputFamily());
    if (!familyFormats.contains(format)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Format %s is not a member of the input family of problem type %s"
              .formatted(patch.getInputFormat(), type.getId()));
    }

    try {
      return FormattedValue.parse(patch.getInput(), format);
    } catch (ParseException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Could not parse input as %s: %s".formatted(patch.getInputFormat(), e.getMessage()));
    }
  }

  private void validate(ProblemDto problem) {
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

  @Autowired
  void setConversionRegistry(ConversionRegistry conversionRegistry) {
    this.conversionRegistry = conversionRegistry;
  }

  @Autowired
  void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }
}