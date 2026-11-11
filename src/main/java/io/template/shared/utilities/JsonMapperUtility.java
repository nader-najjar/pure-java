package io.template.shared.utilities;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/*
 * Quick guide to common MAPPER inputs (Jackson 2.20.1). These are the defaults; annotations or custom deserializers can
 * change a property.
 *
 * - One JSON value only. Malformed JSON, duplicate keys in one object, or a second top-level value fails. Unknown
 *   properties are ignored.
 *
 * - Jackson can fill in records from JSON without extra annotations. For regular classes,
 *   use annotations like @JsonProperty or @JsonCreator to tell it which fields, getters,
 *   setters, constructors, or factory methods to use. The no-arg constructor doesn't
 *   need an annotation.
 *
 * - Boxed types below accept JSON null as Java null. Missing boxed record/constructor fields also become null.
 *   Hibernate checks @NotNull and other validation constraints when using JsonSanitizer.parseAndValidate.
 *   For objects created with a no-arg constructor, missing fields keep their constructor/default values.
 *   @JsonProperty(required = true) still makes Jackson reject a missing record/constructor field.
 *
 * - Do not use primitives in input models.
 *
 * - Short, Integer, Long: integer 1 works; quoted "1", decimal 1.0, and
 *   exponent 1e0 fail. Ranges: Short -32768 through 32767, Integer -2147483648 through
 *   2147483647, Long -9223372036854775808 through 9223372036854775807. Out-of-range values are rejected.
 *
 * - Float and Double: JSON numbers like 1, 1.0, and 1e2 work.
 *   Quoted numbers like "1" or "1.0" and booleans fail. 0.1 is rounded.
 *   Overflow is accepted: 1e400 becomes Float.POSITIVE_INFINITY or Double.POSITIVE_INFINITY;
 *   -1e400 becomes Float.NEGATIVE_INFINITY or Double.NEGATIVE_INFINITY.
 *
 * - Boolean: JSON true/false and null are accepted. Strings (including "true"/"false" and "")
 *   and numbers fail. Missing fields follow the boxed-type rules above.
 *
 * - String: use quoted JSON strings, including "" for an empty string. Numbers, booleans,
 *   arrays, and objects fail. JSON null and missing record/constructor fields become Java null.
 *   Hibernate handles @NotNull; the no-arg constructor and required-field exceptions above still apply.
 *
 * - Instant: ISO text ("2025-01-02T03:04:05Z"), numbers, and numeric strings work.
 *   Timestamps are seconds, not milliseconds: 1 and "1" are one second after the epoch,
 *   1.5 is 1.5 seconds, and 1000 is 1000 seconds.
 *
 * - Some other date/time shapes: LocalDate accepts "2024-03-01" or [2024, 3, 1];
 *   integers are epoch-day counts (0 is 1970-01-01). LocalDateTime accepts ISO text or
 *   [2024, 3, 1, 12, 30, 0].
 *
 * - Enums accept case-sensitive names, not number positions. Both 0 and "0" fail.
 *   Wrong case and unknown names fail. JSON null and missing record/constructor fields
 *   become null, with the same validation and required-field rules as above.
 *
 * - Element arrays (for example, String[]), List<T>, and Set<T> need JSON arrays.
 *   Scalars aren't wrapped; one-item arrays aren't unwrapped. Lists keep order, sets
 *   drop duplicates, and reference elements may be null.
 *
 * - Only use Map<String, V> for input maps, and don't use Java Object as an input type
 *   (including fields, array/collection elements, or map values).
 *   Behavior is not guaranteed if you use other map key types or Object.
 *
 * - MAPPER.readValue turns JSON into Java values; it doesn't run Hibernate validation.
 *   JsonSanitizer.parseAndValidate maps first, then validates. Nested constraints
 *   cascade only with @Valid.
 */
public final class JsonMapperUtility {

    public static final JsonMapper MAPPER = createStrictMapper();

    private JsonMapperUtility() { }

    private static JsonMapper createStrictMapper() {
        return JsonMapper.builder()
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .disable(MapperFeature.AUTO_DETECT_FIELDS)
                .disable(MapperFeature.AUTO_DETECT_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_SETTERS)

                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)

                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .disable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
                .disable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .disable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .disable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)

                .disable(MapperFeature.USE_BASE_TYPE_AS_DEFAULT_IMPL)

                .withCoercionConfig(
                        LogicalType.Integer,
                        config -> config.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                )
                .withCoercionConfig(
                        LogicalType.Float,
                        config -> config.setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                )

                .withCoercionConfig(
                        LogicalType.Boolean,
                        config -> config
                                .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                )
                .withCoercionConfig(
                        LogicalType.Textual,
                        config -> config
                                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
                )

                .addModule(new JavaTimeModule())

                .build();
    }

}
