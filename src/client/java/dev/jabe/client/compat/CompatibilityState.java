package dev.jabe.client.compat;

import java.util.concurrent.atomic.AtomicReference;

public final class CompatibilityState {
    private static final CompatibilityState INSTANCE = new CompatibilityState();

    private final AtomicReference<CompatibilityMode> mode =
            new AtomicReference<>(CompatibilityMode.JAVA);

    private CompatibilityState() {
    }

    public static CompatibilityState getInstance() {
        return INSTANCE;
    }

    public CompatibilityMode mode() {
        return mode.get();
    }

    public void enterBedrockMode() {
        mode.set(CompatibilityMode.BEDROCK);
    }

    public void enterJavaMode() {
        mode.set(CompatibilityMode.JAVA);
    }
}
