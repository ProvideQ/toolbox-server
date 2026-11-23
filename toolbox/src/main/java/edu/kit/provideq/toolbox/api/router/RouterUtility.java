package edu.kit.provideq.toolbox.api.router;

import edu.kit.provideq.formats.core.families.FormatFamily;
import edu.kit.provideq.toolbox.meta.Problem;
import edu.kit.provideq.toolbox.meta.ProblemManager;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RouterUtility {
  private RouterUtility() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static <InT extends FormatFamily, OutT extends FormatFamily>
      Problem<InT, OutT> findProblemOrThrow(
          ProblemManager<InT, OutT> manager,
          String id) {
    UUID uuid;
    try {
      uuid = UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid problem ID");
    }

    return manager.findInstanceById(uuid)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Could not find a problem for this type with this problem ID!"));
  }
}
