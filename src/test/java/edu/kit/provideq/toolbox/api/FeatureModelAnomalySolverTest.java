package edu.kit.provideq.toolbox.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.featuremodel.SolveFeatureModelRequest;
import edu.kit.provideq.toolbox.featuremodel.anomaly.solvers.FeatureModelAnomalySolver;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
public class FeatureModelAnomalySolverTest {
  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  public static Stream<Arguments> provideAnomalySolverIds() {
    String solverId = FeatureModelAnomalySolver.class.getName();
    return Stream.of(
        Arguments.of(solverId, "void", SolutionStatus.SOLVED),
        Arguments.of(solverId, "dead", SolutionStatus.SOLVED),

        // not implemented yet, change to SOLVED when they have been implemented!
        Arguments.of(solverId, "false-optional", SolutionStatus.INVALID),
        Arguments.of(solverId, "redundant-constraints", SolutionStatus.INVALID)
    );
  }

  @ParameterizedTest
  @MethodSource("provideAnomalySolverIds")
  void testFeatureModelAnomalySolver(String solverId, String anomalyType,
                                     SolutionStatus expectedStatus) throws Exception {
    var req = new SolveFeatureModelRequest();
    req.requestedSolverId = solverId;
    req.requestContent = """
        namespace Sandwich
                        
        features
            Sandwich {extended__}   \s
                mandatory
                    Bread   \s
                        alternative
                            "Full Grain" {Calories 203, Price 1.99, Organic true}
                            Flatbread {Calories 90, Price 0.79, Organic true}
                            Toast {Calories 250, Price 0.99, Organic false}
                optional
                    Cheese   \s
                        optional
                            Gouda   \s
                                alternative
                                    Sprinkled {Fat {value 35, unit "g"}}
                                    Slice {Fat {value 35, unit "g"}}
                            Cheddar
                            "Cream Cheese"
                    Meat   \s
                        or
                            "Salami" {Producer "Farmer Bob"}
                            Ham {Producer "Farmer Sam"}
                            "Chicken Breast" {Producer "Farmer Sam"}
                    Vegetables   \s
                        optional
                            "Cucumber"
                            Tomatoes
                            Lettuce
        """;

    var requestBuilder = MockMvcRequestBuilders
        .post("/solve/feature-model/anomaly/" + anomalyType)
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(req));

    var result = mvc.perform(requestBuilder)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse().getContentAsString();

    JavaType solutionType =
        mapper.getTypeFactory().constructParametricType(Solution.class, String.class);
    Solution<String> solution = mapper.readValue(result, solutionType);

    assertThat(solution.getStatus())
        .isSameAs(expectedStatus);
  }
}