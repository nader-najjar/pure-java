package io.template.shared.utilities;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMapperNumberTest {

    @ParameterizedTest
    @MethodSource("numericTypes")
    void rejectsQuotedNumbers(Class<?> type) {
        for (String json : List.of("\"1\"", "\"-1\"", "\"1.0\"", "\"1e0\"", "\" 1 \"")) {
            assertThrows(
                    JsonProcessingException.class,
                    () -> JsonMapperUtility.MAPPER.readValue(json, type),
                    () -> type.getSimpleName() + " must reject " + json
            );
        }
    }

    @ParameterizedTest
    @MethodSource("numericTypes")
    void stillAcceptsJsonNumbers(Class<?> type) throws JsonProcessingException {
        Number value = (Number) JsonMapperUtility.MAPPER.readValue("1", type);

        assertEquals(1.0, value.doubleValue());
    }

    @ParameterizedTest
    @MethodSource("numericTypes")
    void keepsExistingNullBehavior(Class<?> type) throws JsonProcessingException {
        if (type.isPrimitive()) {
            assertThrows(JsonProcessingException.class, () -> JsonMapperUtility.MAPPER.readValue("null", type));
        } else {
            assertNull(JsonMapperUtility.MAPPER.readValue("null", type));
        }
    }

    @Test
    void stillAcceptsJsonDecimalsAndExponentsForFloatingPoint() throws JsonProcessingException {
        assertEquals(1.5f, JsonMapperUtility.MAPPER.readValue("1.5", Float.class));
        assertEquals(100.0, JsonMapperUtility.MAPPER.readValue("1e2", Double.class));
        assertEquals(new BigDecimal("1.5"), JsonMapperUtility.MAPPER.readValue("1.5", BigDecimal.class));
    }

    @Test
    void rejectsQuotedNumbersInRecordsAndContainers() {
        assertThrows(
                JsonProcessingException.class,
                () -> JsonMapperUtility.MAPPER.readValue("{\"value\":\"1\"}", NumericInput.class)
        );
        assertThrows(
                JsonProcessingException.class,
                () -> JsonMapperUtility.MAPPER.readValue("[\"1\"]", Integer[].class)
        );
        assertThrows(
                JsonProcessingException.class,
                () -> JsonMapperUtility.MAPPER.readValue("[\"1\"]", int[].class)
        );
        assertThrows(
                JsonProcessingException.class,
                () -> JsonMapperUtility.MAPPER.readValue("[\"1\"]", new TypeReference<List<Double>>() { })
        );
        assertThrows(
                JsonProcessingException.class,
                () -> JsonMapperUtility.MAPPER.readValue("{\"x\":\"1\"}", new TypeReference<Map<String, Long>>() { })
        );
    }

    @Test
    void leavesNonNumericTargetsAndMapKeysAlone() throws JsonProcessingException {
        assertEquals("1", JsonMapperUtility.MAPPER.readValue("\"1\"", String.class));
        assertEquals("1", JsonMapperUtility.MAPPER.readValue("1", String.class));
        assertEquals(true, JsonMapperUtility.MAPPER.readValue("\"TRUE\"", Boolean.class));
        assertEquals(true, JsonMapperUtility.MAPPER.readValue("2", Boolean.class));
        assertEquals(Instant.ofEpochSecond(1), JsonMapperUtility.MAPPER.readValue("\"1\"", Instant.class));
        assertEquals(ExampleChoice.FIRST, JsonMapperUtility.MAPPER.readValue("\"0\"", ExampleChoice.class));
        assertEquals(
                Map.of(1, "x"),
                JsonMapperUtility.MAPPER.readValue("{\"1\":\"x\"}", new TypeReference<Map<Integer, String>>() { })
        );
    }

    static Stream<Class<?>> numericTypes() {
        return Stream.of(
                byte.class,
                Byte.class,
                short.class,
                Short.class,
                int.class,
                Integer.class,
                long.class,
                Long.class,
                float.class,
                Float.class,
                double.class,
                Double.class,
                BigInteger.class,
                BigDecimal.class,
                Number.class
        );
    }

    record NumericInput(Integer value) { }

    enum ExampleChoice { FIRST }
}
