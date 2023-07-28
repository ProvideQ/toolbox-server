package edu.kit.provideq.toolbox.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.format.gml.Gml;
import edu.kit.provideq.toolbox.maxcut.SolveMaxCutRequest;
import edu.kit.provideq.toolbox.maxcut.solvers.GamsMaxCutSolver;
import edu.kit.provideq.toolbox.maxcut.solvers.QiskitMaxCutSolver;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class MaxCutSolversTest {
  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  public static Stream<String> provideMaxCutSolverIds() {
    return Stream.of(
        GamsMaxCutSolver.class.getName(),
        QiskitMaxCutSolver.class.getName()
    );
  }

  @ParameterizedTest
  @MethodSource("provideMaxCutSolverIds")
  void testMaxCutSolver(String solverId) throws Exception {
    var req = new SolveMaxCutRequest();
    req.requestedSolverId = solverId;
    req.requestContent = """
        graph [
            id 42
            node [
                id 1
                label "1"
            ]
            node [
                id 2
                label "2"
            ]
            node [
                id 3
                label "3"
            ]
            edge [
                source 1
                target 2
            ]
            edge [
                source 2
                target 3
            ]
            edge [
                source 3
                target 1
            ]
        ]""";

    var requestBuilder = MockMvcRequestBuilders
        .post("/solve/max-cut")
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
        .isSameAs(SolutionStatus.SOLVED);
  }
}
