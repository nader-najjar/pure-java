package io.template.composition;

import java.util.Map;

import io.template.environment.EnvironmentVariables;
import io.template.shared.utilities.JsonMapperUtility;
import io.template.testsupport.SampleEnvironmentMaps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AWSClientsModuleTest {

    @ParameterizedTest
    @ValueSource(strings = {"us-fake-1", "US-EAST-1", " us-east-1", "us-east-1 "})
    void rejectsUnknownRegionBeforeCreatingClient(String region) {
        Map<String, String> environment = SampleEnvironmentMaps.validEnvironment();
        environment.put("AWS_REGION", region);
        EnvironmentVariables values = JsonMapperUtility.MAPPER.convertValue(environment, EnvironmentVariables.class);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AWSClientsModule().provideDynamoDBClient(values)
        );
        assertEquals("AWS_REGION must be a known AWS region", exception.getMessage());
    }
}
