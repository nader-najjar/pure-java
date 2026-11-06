package io.template.environment;

import java.util.Arrays;
import java.util.Map;

import io.template.environment.exceptions.EnvironmentVariableException;
import io.template.environment.models.EnvironmentVariables;
import io.template.environment.models.Stage;
import io.template.shared.utilities.HibernateValidatorUtility;
import jakarta.validation.ValidationException;
import software.amazon.awssdk.regions.Region;

/**
 * Creates {@link EnvironmentVariables} instances from raw environment maps.
 * <p>
 * This class contains all logic for extracting, parsing and validating
 * environment variables. {@link io.template.composition.EnvironmentModule}
 * is intentionally kept thin and delegates to this factory, so the logic
 * can be tested without going through Guice.
 */
public final class EnvironmentVariablesFactory {

    private EnvironmentVariablesFactory() { }

    public static EnvironmentVariables from(Map<String, String> environment) {
        EnvironmentVariables environmentVariables = new EnvironmentVariables(
                extractEnum(environment, "STAGE", Stage.class),
                extractAWSRegion(environment, "AWS_REGION"),
                extractString(environment, "CALCULATION_RESULTS_TABLE_NAME"),
                extractString(environment, "EXAMPLE_STRING_VAR"),
                extractInt(environment, "EXAMPLE_INT_VAR"),
                extractBoolean(environment, "EXAMPLE_BOOLEAN_VAR")
        );

        try {
            HibernateValidatorUtility.validate(environmentVariables);

        } catch (ValidationException exception) {
            throw new EnvironmentVariableException("Environment validation failed", exception);
        }

        return environmentVariables;
    }

    static String extractString(Map<String, String> environment, String key) {
        String value = environment.get(key);
        ensureVariableExists(key, value);
        return value;
    }

    static int extractInt(Map<String, String> environment, String key) {
        String value = environment.get(key);
        ensureVariableExists(key, value);

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String message = "Environment variable '" + key + "' must be a valid integer, got: " + value;
            throw new EnvironmentVariableException(message, e);
        }
    }

    static boolean extractBoolean(Map<String, String> environment, String key) {
        String value = environment.get(key);
        ensureVariableExists(key, value);

        return customParseBoolean(key, value);
    }

    static <E extends Enum<E>> E extractEnum(Map<String, String> environment, String key, Class<E> type) {
        String value = environment.get(key);
        ensureVariableExists(key, value);

        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            String allowedValues = Arrays.toString(type.getEnumConstants());
            String message = "Environment variable '" + key + "' must be one of " + allowedValues + ", got: " + value;
            throw new EnvironmentVariableException(message, e);
        }
    }

    static Region extractAWSRegion(Map<String, String> environment, String key) {
        String value = environment.get(key);
        ensureVariableExists(key, value);

        Region region = Region.of(value);
        if (!Region.regions().contains(region)) {
            String allowedValues = Region.regions().toString();
            String message = "Environment variable '" + key + "' must be one of " + allowedValues + ", got: " + value;
            throw new EnvironmentVariableException(message);
        }
        return region;
    }

    static void ensureVariableExists(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new EnvironmentVariableException("Required environment variable '" + key + "' is not set");
        }
    }

    /**
     * The existing Boolean.parseBoolean() returns {@code false} for any value not equal to {@code "true"}.
     * We want an error thrown if the value is unexpected, and we abide by the JSON standard of expecting
     * lowercase {@code "true"} or {@code "false"}.
     */
    static boolean customParseBoolean(String key, String value) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }

        String message = "Environment variable '" + key + "' must be 'true' or 'false', got: " + value;
        throw new EnvironmentVariableException(message);
    }
}
