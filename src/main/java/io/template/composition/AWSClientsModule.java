package io.template.composition;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.template.environment.models.EnvironmentVariables;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class AWSClientsModule extends AbstractModule {

    @Provides
    @Singleton
    DynamoDbClient provideDynamoDBClient(EnvironmentVariables environmentVariables) {
        Region region = Region.of(environmentVariables.awsRegion());
        if (!Region.regions().contains(region)) {
            throw new IllegalArgumentException("AWS_REGION must be a known AWS region");
        }

        return DynamoDbClient.builder()
                .region(region)
                .build();
    }

    public static void closeResources(Injector injector) {
        injector.getInstance(DynamoDbClient.class).close();
    }
}
