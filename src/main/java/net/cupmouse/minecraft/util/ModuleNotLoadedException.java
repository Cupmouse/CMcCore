package net.cupmouse.minecraft.util;

public class ModuleNotLoadedException extends RuntimeException {

    public ModuleNotLoadedException() {
    }

    public ModuleNotLoadedException(String message) {
        super(message);
    }

    public ModuleNotLoadedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModuleNotLoadedException(Throwable cause) {
        super(cause);
    }

    public ModuleNotLoadedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
