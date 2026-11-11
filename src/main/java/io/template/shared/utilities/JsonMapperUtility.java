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
 * - Missing record/constructor fields become null for boxed and other reference types, just like JSON null.
 *   Hibernate's @NotNull decides whether null is allowed when using JsonSanitizer.parseAndValidate.
 *   For objects created with a no-arg constructor, missing fields keep their constructor/default values.
 *   @JsonProperty(required = true) still makes Jackson reject a missing record/constructor field.
 *
 * - Do not use primitives in input models. They can't hold null, so explicit JSON null fails.
 *   Missing primitive record/constructor fields fail too.
 *
 * - short/Short, int/Integer, long/Long: integer 1 works; quoted "1", decimal 1.0, and
 *   exponent 1e0 fail. Ranges: short -32768 through 32767, int -2147483648 through
 *   2147483647, long -9223372036854775808 through 9223372036854775807. Overflow fails.
 *
 * - float/Float and double/Double: JSON numbers like 1, 1.0, and 1e2 work.
 *   Quoted numbers like "1" or "1.0" and booleans fail. 0.1 is rounded; 1e400 becomes infinity.
 *
 * - boolean/Boolean: JSON true/false, strings "TRUE"/"False", and integer 0/2 work
 *   (0 is false; nonzero is true). Mixed-case "tRuE" and decimal 1.0 fail.
 *
 * - String: strings stay as written; 1 becomes "1", true becomes "true", and null
 *   becomes Java null. Arrays and objects fail.
 *
 * - Instant: ISO text ("2025-01-02T03:04:05Z"), numbers, and numeric strings work.
 *   Timestamps are seconds, not milliseconds: 1 and "1" are one second after the epoch,
 *   1.5 is 1.5 seconds, and 1000 is 1000 seconds.
 *
 * - Some other date/time shapes: LocalDate accepts "2024-03-01" or [2024, 3, 1];
 *   integers are epoch-day counts (0 is 1970-01-01). LocalDateTime accepts ISO text or
 *   [2024, 3, 1, 12, 30, 0].
 *
 * - Enums accept exact, case-sensitive names or ordinals: 0 and "0" select the first
 *   value. Wrong case, unknown names, and out-of-range ordinals fail.
 *
 * - Element arrays (for example, String[]), List<T>, and Set<T> need JSON arrays.
 *   Scalars aren't wrapped; one-item arrays aren't unwrapped. Lists keep order, sets
 *   drop duplicates, and reference elements may be null.
 *
 * - Map<K, V> needs a JSON object. Keys are text: "1" becomes integer key 1 for
 *   Map<Integer, V>; invalid keys fail. Values follow their target type.
 *
 * - Nested records/objects use the same rules. Object gets maps, lists, and natural
 *   scalar values; JsonNode keeps the tree, including integer vs decimal number nodes.
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

                .withCoercionConfig(LogicalType.Integer,
                        config -> config.setCoercion(CoercionInputShape.String, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Float,
                        config -> config.setCoercion(CoercionInputShape.String, CoercionAction.Fail))

                .addModule(new JavaTimeModule())

                .build();
    }

}
