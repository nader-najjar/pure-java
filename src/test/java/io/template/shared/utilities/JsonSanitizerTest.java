package io.template.shared.utilities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.template.execution.models.ApplicationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_EMPTY_STRING;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_MALFORMED;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_NOT_JSON;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_INVALID_TIMESTAMP;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_NON_NUMERIC_INT_FIELD;
import static io.template.testsupport.SampleJsonInputs.INVALID_JSON_WITH_NULL_STRING_FIELD;
import static io.template.testsupport.SampleJsonInputs.validInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSanitizerTest {

    private static final Map<String, String> VALID_FIELDS = Map.of(
            "exampleStringField", "\"a\"",
            "exampleIntField", "5",
            "exampleBooleanField", "true",
            "exampleTimestampField", "\"2026-01-01T00:00:00Z\"",
            "exampleListField", "[\"x\",\"y\"]");

    private static final ApplicationInput EXPECTED = new ApplicationInput(
            "a", 5, true, Instant.parse("2026-01-01T00:00:00Z"), List.of("x", "y"));

    @Test
    void rejectsJSONNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate("null", ApplicationInput.class)
        );
    }

    @Test
    void rejectsNullJSON() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(null, ApplicationInput.class)
        );
    }

    @Test
    void inputOwnsAnUnmodifiableSnapshotOfItsList() {
        List<String> values = new ArrayList<>();
        values.add("a");
        values.add(null);
        ApplicationInput input = new ApplicationInput("a", 5, true, Instant.EPOCH, values);

        values.set(0, "changed");
        assertEquals("a", input.exampleListField().get(0));
        assertEquals(2, input.exampleListField().size());
        assertEquals(null, input.exampleListField().get(1));
        assertThrows(UnsupportedOperationException.class, () -> input.exampleListField().add("b"));
    }

    @Test
    void sanitizesValidInput() {
        String json = validInput(
                "hello",
                3,
                true,
                "2024-01-01T00:00:00Z",
                "a",
                "b"
        );

        ApplicationInput input = JsonSanitizer.parseAndValidate(json, ApplicationInput.class);

        assertEquals("hello", input.exampleStringField());
        assertEquals(3, input.exampleIntField());
        assertTrue(input.exampleBooleanField());
        assertEquals("2024-01-01T00:00:00Z", input.exampleTimestampField().toString());
        assertEquals(Set.of("a", "b"), Set.copyOf(input.exampleListField()));
    }

    @Test
    void sanitizesInputWithFalseBoolean() {
        String json = validInput(
                "test",
                0,
                false,
                "2024-01-01T00:00:00Z"
        );

        ApplicationInput input = JsonSanitizer.parseAndValidate(json, ApplicationInput.class);

        assertFalse(input.exampleBooleanField());
        assertEquals(0, input.exampleIntField());
        assertTrue(input.exampleListField().isEmpty());
    }

    @Test
    void rejectsInvalidJson() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(INVALID_JSON_MALFORMED, ApplicationInput.class)
        );

        assertTrue(exception.getMessage().contains("Invalid ApplicationInput"));
        assertNotNull(exception.getCause());
    }

    @Test
    void rejectsEmptyJsonString() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(INVALID_JSON_EMPTY_STRING, ApplicationInput.class)
        );

        assertTrue(exception.getMessage().contains("Invalid ApplicationInput"));
    }

    @Test
    void rejectsMalformedJson() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(INVALID_JSON_NOT_JSON, ApplicationInput.class)
        );

        assertTrue(exception.getMessage().contains("Invalid ApplicationInput"));
    }

    @Test
    void rejectsJsonWithInvalidFieldTypes() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(INVALID_JSON_WITH_NON_NUMERIC_INT_FIELD, ApplicationInput.class)
        );

        assertTrue(exception.getMessage().contains("Invalid ApplicationInput"));
        assertNotNull(exception.getCause());
    }

    @Test
    void rejectsJsonWithInvalidTimestampFormat() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(INVALID_JSON_WITH_INVALID_TIMESTAMP, ApplicationInput.class)
        );

        assertTrue(exception.getMessage().contains("Invalid ApplicationInput"));
        assertNotNull(exception.getCause());
    }

    @Test
    void rejectsJsonWithNullStringField() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(INVALID_JSON_WITH_NULL_STRING_FIELD, ApplicationInput.class)
        );

        assertTrue(exception.getMessage().contains("Invalid ApplicationInput"));
    }

    @Test
    void ignoresUnknownScalarProperty() {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.put("unknownField", "1");

        assertEquals(EXPECTED, JsonSanitizer.parseAndValidate(toJson(fields), ApplicationInput.class));
    }

    @Test
    void ignoresUnknownNestedProperty() {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.put("unknownObject", "{\"nested\":[1,{\"deep\":true}],\"label\":\"z\"}");

        assertEquals(EXPECTED, JsonSanitizer.parseAndValidate(toJson(fields), ApplicationInput.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "exampleStringField",
        "exampleIntField",
        "exampleBooleanField",
        "exampleTimestampField",
        "exampleListField"
    })
    void rejectsMissingProperty(String missingField) {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.remove(missingField);
        String json = toJson(fields);

        assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(json, ApplicationInput.class)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "exampleStringField",
        "exampleIntField",
        "exampleBooleanField",
        "exampleTimestampField",
        "exampleListField"
    })
    void rejectsNullProperty(String nullField) {
        Map<String, String> fields = new LinkedHashMap<>(VALID_FIELDS);
        fields.put(nullField, "null");
        String json = toJson(fields);

        assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(json, ApplicationInput.class)
        );
    }

    @Test
    void rejectsDuplicateProperty() {
        String json = toJson(VALID_FIELDS).replaceFirst("\\{", "{\"exampleStringField\":\"b\",");

        assertThrows(
                IllegalArgumentException.class,
                () -> JsonSanitizer.parseAndValidate(json, ApplicationInput.class)
        );
    }

    private static String toJson(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(field -> "\"" + field.getKey() + "\":" + field.getValue())
                .collect(Collectors.joining(",", "{", "}"));
    }
}
