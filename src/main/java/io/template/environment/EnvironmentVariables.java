package io.template.environment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// Environment values stay strings. Convert them where the application needs a different type.
public record EnvironmentVariables(

        @JsonProperty("STAGE")
        @NotNull
        @NotBlank
        @Pattern(regexp = "ALPHA|BETA|GAMMA|PRODUCTION")
        String stage,

        @JsonProperty("AWS_REGION")
        @NotNull
        @NotBlank
        String awsRegion,

        @JsonProperty("CALCULATION_RESULTS_TABLE_NAME")
        @NotNull
        @NotBlank
        String calculationResultsTableName,

        @JsonProperty("EXAMPLE_STRING_VAR")
        @NotNull
        @NotBlank
        String exampleStringVar,

        @JsonProperty("EXAMPLE_INT_VAR")
        @NotNull
        @NotBlank
        @Pattern(regexp = "[+-]?\\p{javaDigit}+")
        @DecimalMin("-2147483648")
        @DecimalMax("2147483647")
        String exampleIntVar,

        @JsonProperty("EXAMPLE_BOOLEAN_VAR")
        @NotNull
        @NotBlank
        @Pattern(regexp = "true|false")
        String exampleBooleanVar

) { }
