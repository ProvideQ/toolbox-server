package edu.kit.provideq.toolbox.api;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.provideq.toolbox.Solution;
import edu.kit.provideq.toolbox.SolutionStatus;
import edu.kit.provideq.toolbox.maxCut.SolveMaxCutRequest;
import edu.kit.provideq.toolbox.sat.SolveSatRequest;
import edu.kit.provideq.toolbox.sat.solvers.GamsSATSolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SatSolverTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    public static Stream<String> provideSatSolverIds() {
        return Stream.of(
                GamsSATSolver.class.getName()
        );
    }

    @ParameterizedTest
    @MethodSource("provideSatSolverIds")
    void testSatSolver(String solverId) throws Exception {
        var req = new SolveSatRequest();
        req.requestedSolverId = solverId;
        req.requestContent = "a and b";

        var requestBuilder = MockMvcRequestBuilders
                .post("/solve/sat")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req));

        var result = mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        JavaType solutionType = mapper.getTypeFactory().constructParametricType(Solution.class, String.class);
        Solution<String> solution = mapper.readValue(result, solutionType);

        assertThat(solution.getStatus())
                .isSameAs(SolutionStatus.SOLVED);
    }
}
