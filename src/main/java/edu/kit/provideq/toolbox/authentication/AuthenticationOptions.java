package edu.kit.provideq.toolbox.authentication;

/**
 * Options for authentication that a solver provides.
 */
public record AuthenticationOptions(
    String authenticationAgent,
    String authenticationDescription,
    boolean supportsToken
) {

}
