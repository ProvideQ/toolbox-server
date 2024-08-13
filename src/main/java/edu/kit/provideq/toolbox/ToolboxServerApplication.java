package edu.kit.provideq.toolbox;

import edu.kit.provideq.toolbox.exception.MissingSpringProfileException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ToolboxServerApplication {

  public static void main(String[] args) throws MissingSpringProfileException {
    String springProfileArg = "--spring.profiles.active=" + getSpringProfileForOs();
    SpringApplication.run(ToolboxServerApplication.class, springProfileArg);
  }

  private static String getSpringProfileForOs() throws MissingSpringProfileException {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("win")) {
      return "win";
    } else if (osName.contains("mac")) {
      return "mac";
    } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
      return "unix";
    } else {
      throw new MissingSpringProfileException("Could not start Toolbox, "
              + "there is no Spring Profile that matches your OS specification.");
    }
  }

}
