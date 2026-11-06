package io.template.shared.utilities;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonMapperUtility {

    public static final JsonMapper MAPPER = createStrictMapper();

    private JsonMapperUtility() { }

    private static JsonMapper createStrictMapper() {
        // Reject scalar coercions between JSON text, number, and boolean values. Integer JSON numbers can populate
        // floating fields, but decimal values cannot be truncated into integer fields. Enum ordinals and numeric
        // Instant timestamps are rejected, while unknown fields remain allowed.
        // Missing and null reference creator properties stay null for Bean Validation; use boxed types when @NotNull
        // must decide requiredness. Missing and null primitive creator values fail because primitives cannot hold null.
        JsonMapper mapper = JsonMapper.builder()
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .disable(MapperFeature.AUTO_DETECT_FIELDS)
                .disable(MapperFeature.AUTO_DETECT_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_SETTERS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)

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

                .addModule(new JavaTimeModule())
                .addModule(new SimpleModule().addDeserializer(Instant.class, new StrictInstantDeserializer()))

                .build();

        configureScalarCoercions(mapper);
        return mapper;
    }

    private static void configureScalarCoercions(JsonMapper mapper) {
        configureCoercions(mapper, LogicalType.Textual,
                CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean);
        configureCoercions(mapper, LogicalType.Integer, CoercionInputShape.String, CoercionInputShape.Boolean);
        configureCoercions(mapper, LogicalType.Float, CoercionInputShape.String, CoercionInputShape.Boolean);
        configureCoercions(mapper, LogicalType.Boolean,
                CoercionInputShape.String, CoercionInputShape.Integer, CoercionInputShape.Float);
        configureCoercions(mapper, LogicalType.DateTime,
                CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean);
        configureCoercions(mapper, LogicalType.Enum,
                CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean);
        configureCoercions(mapper, LogicalType.OtherScalar,
                CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean);
    }

    private static void configureCoercions(
            JsonMapper mapper,
            LogicalType logicalType,
            CoercionInputShape... inputShapes
    ) {
        for (CoercionInputShape inputShape : inputShapes) {
            mapper.coercionConfigFor(logicalType).setCoercion(inputShape, CoercionAction.Fail);
        }
    }

    private static final class StrictInstantDeserializer extends JsonDeserializer<Instant> {

        @Override
        public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (Instant) context.handleUnexpectedToken(Instant.class, parser);
            }

            String value = parser.getText();
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException exception) {
                return (Instant) context.handleWeirdStringValue(
                        Instant.class,
                        value,
                        "Expected an ISO-8601 instant"
                );
            }
        }
    }

}
