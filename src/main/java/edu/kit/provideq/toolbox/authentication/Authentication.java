package edu.kit.provideq.toolbox.authentication;

import javax.annotation.Nullable;

public record Authentication(
    @Nullable String token
) {
}
