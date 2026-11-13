package io.template.environment;

import java.util.Map;
import java.util.stream.Stream;

import io.template.shared.utilities.HibernateValidatorUtility;
import io.template.shared.utilities.JsonMapperUtility;
import io.template.testsupport.SampleEnvironmentMaps;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentVariablesTest {

    @Test
    void mapsExactStringsAndIgnoresUnrelatedEnvironment() {
        Map<String, String> environment = SampleEnvironmentMaps.validEnvironment();
        environment.put("PATH", "/bin");
        environment.put("EXAMPLE_STRING_VAR", "  keep spaces  ");
        EnvironmentVariables result = map(environment);

        assertEquals("BETA", result.stage());
        assertEquals("us-east-1", result.awsRegion());
        assertEquals("CalculationResults", result.calculationResultsTableName());
        assertEquals("  keep spaces  ", result.exampleStringVar());
        assertEquals("1", result.exampleIntVar());
        assertEquals("true", result.exampleBooleanVar());
        assertDoesNotThrow(() -> HibernateValidatorUtility.validate(result));
    }

    @Test
    void missingValuesBecomeNullBeforeValidation() {
        EnvironmentVariables result = map(Map.of());
        assertNull(result.stage());
        assertNull(result.awsRegion());
        assertNull(result.calculationResultsTableName());
        assertNull(result.exampleStringVar());
        assertNull(result.exampleIntVar());
        assertNull(result.exampleBooleanVar());
        assertThrows(ValidationException.class, () -> HibernateValidatorUtility.validate(result));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STAGE", "AWS_REGION", "CALCULATION_RESULTS_TABLE_NAME", "EXAMPLE_STRING_VAR",
        "EXAMPLE_INT_VAR", "EXAMPLE_BOOLEAN_VAR"})
    void validatesEachRequiredVariable(String key) {
        Map<String, String> environment = SampleEnvironmentMaps.validEnvironment();
        environment.remove(key);
        assertThrows(ValidationException.class, () -> HibernateValidatorUtility.validate(map(environment)));
        environment.put(key, null);
        assertThrows(ValidationException.class, () -> HibernateValidatorUtility.validate(map(environment)));
        environment.put(key, " ");
        assertThrows(ValidationException.class, () -> HibernateValidatorUtility.validate(map(environment)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void preservesBlankInputThenValidationRejectsIt(String value) {
        Map<String, String> environment = SampleEnvironmentMaps.validEnvironment();
        environment.replaceAll((key, previous) -> value);
        EnvironmentVariables result = map(environment);
        assertEquals(value, result.exampleStringVar());
        assertEquals(value, result.exampleIntVar());
        assertEquals(value, result.exampleBooleanVar());
        assertThrows(ValidationException.class, () -> HibernateValidatorUtility.validate(result));
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void rejectsInvalidValuesWithoutEchoingThem(String key, String value) {
        Map<String, String> environment = SampleEnvironmentMaps.validEnvironment();
        environment.put(key, value);
        EnvironmentVariables result = map(environment);
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> HibernateValidatorUtility.validate(result)
        );
        assertFalse(exception.getMessage().contains("got: " + value));
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of("STAGE", "beta"),
                Arguments.of("STAGE", " BETA"),
                Arguments.of("EXAMPLE_INT_VAR", "2147483648"),
                Arguments.of("EXAMPLE_INT_VAR", "-2147483649"),
                Arguments.of("EXAMPLE_INT_VAR", "1.0"),
                Arguments.of("EXAMPLE_INT_VAR", "1e2"),
                Arguments.of("EXAMPLE_INT_VAR", " 1"),
                Arguments.of("EXAMPLE_INT_VAR", "not-a-number"),
                Arguments.of("EXAMPLE_INT_VAR", "𝟙"),
                Arguments.of("EXAMPLE_BOOLEAN_VAR", "True"),
                Arguments.of("EXAMPLE_BOOLEAN_VAR", "1"),
                Arguments.of("EXAMPLE_BOOLEAN_VAR", "false ")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"-2147483648", "2147483647", "0", "+42", "001", "١٢", "１２", "-٢١٤٧٤٨٣٦٤٨"})
    void validatesIntegerTextWithoutConvertingIt(String value) {
        Map<String, String> environment = SampleEnvironmentMaps.validEnvironment();
        environment.put("EXAMPLE_INT_VAR", value);
        EnvironmentVariables result = map(environment);
        assertEquals(value, result.exampleIntVar());
        assertDoesNotThrow(() -> HibernateValidatorUtility.validate(result));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void validatesBooleanTextWithoutConvertingIt(String value) {
        Map<String, String> environment = SampleEnvironmentMaps.validEnvironment();
        environment.put("EXAMPLE_BOOLEAN_VAR", value);
        EnvironmentVariables result = map(environment);
        assertEquals(value, result.exampleBooleanVar());
        assertDoesNotThrow(() -> HibernateValidatorUtility.validate(result));
    }

    private static EnvironmentVariables map(Map<String, String> environment) {
        return JsonMapperUtility.MAPPER.convertValue(environment, EnvironmentVariables.class);
    }
}
