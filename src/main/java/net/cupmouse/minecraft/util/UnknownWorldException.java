package net.cupmouse.minecraft.util;

public class UnknownWorldException extends RuntimeException {

    public UnknownWorldException() {
    }

    public UnknownWorldException(String message) {
        super(message);
    }

    public UnknownWorldException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownWorldException(Throwable cause) {
        super(cause);
    }

    public UnknownWorldException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
