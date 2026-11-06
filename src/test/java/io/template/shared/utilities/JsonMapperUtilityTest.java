package io.template.shared.utilities;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonMapperUtilityTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "1.5", "true"})
    void rejectsNonStringJsonScalarsForStringFields(String jsonValue) {
        assertReadFails("{\"value\":" + jsonValue + "}", StringTarget.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"5\"", "true", "5.0"})
    void rejectsNonIntegerJsonTypesForIntegerFields(String jsonValue) {
        assertReadFails("{\"value\":" + jsonValue + "}", IntegerTarget.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"5.5\"", "true"})
    void rejectsNonNumberJsonTypesForFloatingPointFields(String jsonValue) {
        assertReadFails("{\"value\":" + jsonValue + "}", FloatingPointTarget.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"true\"", "1", "1.0"})
    void rejectsNonBooleanJsonTypesForBooleanFields(String jsonValue) {
        assertReadFails("{\"value\":" + jsonValue + "}", BooleanTarget.class);
    }

    @Test
    void acceptsJsonNumberForFloatingPointField() throws JsonProcessingException {
        FloatingPointTarget target = JsonMapperUtility.MAPPER.readValue(
                "{\"value\":5}",
                FloatingPointTarget.class
        );

        assertEquals(5.0, target.value());
    }

    @Test
    void rejectsNumericEnumOrdinal() {
        assertReadFails("{\"value\":0}", EnumTarget.class);
    }

    @Test
    void rejectsNumericInstantTimestamp() {
        assertReadFails("{\"value\":1}", InstantTarget.class);
    }

    @Test
    void rejectsWrongScalarTypesInsideCollections() {
        assertReadFails("{\"values\":[1]}", StringListTarget.class);
    }

    @Test
    void rejectsMissingPrimitiveCreatorProperty() {
        assertReadFails("{}", PrimitiveTarget.class);
    }

    @Test
    void rejectsExplicitNullForPrimitive() {
        assertReadFails("{\"value\":null}", PrimitiveTarget.class);
    }

    private static <T> void assertReadFails(String json, Class<T> type) {
        assertThrows(JsonProcessingException.class, () -> JsonMapperUtility.MAPPER.readValue(json, type));
    }

    private record StringTarget(@JsonProperty("value") String value) { }

    private record IntegerTarget(@JsonProperty("value") Integer value) { }

    private record FloatingPointTarget(@JsonProperty("value") Double value) { }

    private record BooleanTarget(@JsonProperty("value") Boolean value) { }

    private record EnumTarget(@JsonProperty("value") SampleEnum value) { }

    private record InstantTarget(@JsonProperty("value") Instant value) { }

    private record StringListTarget(@JsonProperty("values") List<String> values) { }

    private record PrimitiveTarget(@JsonProperty("value") int value) { }

    private enum SampleEnum { FIRST, SECOND }
}
