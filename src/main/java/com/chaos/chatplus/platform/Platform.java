package com.chaos.chatplus.platform;

import java.nio.file.Path;
import java.util.Optional;

/** Loader services installed before common initialization. No client types are referenced here. */
public final class Platform {
    public interface Environment {
        Path configDir();
        boolean loaded(String modId);
        Optional<Path> resource(String modId, String path);
        String version();
    }
    private static Environment environment;
    private Platform() {}
    public static void install(Environment value) { environment = value; }
    public static Environment get() {
        if (environment == null) throw new IllegalStateException("Loader services have not been installed");
        return environment;
    }
}
