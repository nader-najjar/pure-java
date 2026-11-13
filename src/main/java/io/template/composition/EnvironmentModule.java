package io.template.composition;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.template.environment.EnvironmentVariables;
import io.template.shared.utilities.HibernateValidatorUtility;
import io.template.shared.utilities.JsonMapperUtility;

public class EnvironmentModule extends AbstractModule {

    @Provides
    @Singleton
    EnvironmentVariables provideEnvironmentVariables() {
        EnvironmentVariables environmentVariables = JsonMapperUtility.MAPPER.convertValue(
                System.getenv(),
                EnvironmentVariables.class
        );
        HibernateValidatorUtility.validate(environmentVariables);
        return environmentVariables;
    }
}
