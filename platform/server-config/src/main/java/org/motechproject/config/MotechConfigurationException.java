package org.motechproject.config;

public class MotechConfigurationException extends RuntimeException {
    public MotechConfigurationException(String message, Exception exception) {
        super(message, exception);
    }

    public MotechConfigurationException(String message) {
        super(message);
    }
}