package io.template.execution;

import com.google.inject.Inject;
import io.template.environment.EnvironmentVariables;
import io.template.execution.models.ApplicationInput;
import io.template.samplebusinesslayer.CalculationResultStore;
import io.template.samplebusinesslayer.Calculator;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.samplebusinesslayer.models.CalculationResult;
import io.template.shared.models.Stage;
import io.template.shared.utilities.JsonSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

/**
 * Main business logic executor.
 */
public class Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Executor.class);

    private final EnvironmentVariables environmentVariables;
    private final Calculator calculator;
    private final CalculationResultStore calculationResultStore;

    @Inject
    public Executor(
            EnvironmentVariables environmentVariables,
            Calculator calculator,
            CalculationResultStore calculationResultStore
    ) {
        this.environmentVariables = environmentVariables;
        this.calculator = calculator;
        this.calculationResultStore = calculationResultStore;
    }

    public void execute(String[] args) {
        Stage stage = Stage.valueOf(environmentVariables.stage());
        Region awsRegion = Region.of(environmentVariables.awsRegion());
        LOGGER.info("Executing with stage: {}, awsRegion: {}", stage, awsRegion);

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No input provided");
        }

        ApplicationInput input = JsonSanitizer.parseAndValidate(args[0], ApplicationInput.class);
        LOGGER.info("Sanitized input: {}", input);

        invokeSampleLogic();
    }

    private void invokeSampleLogic() {
        CalculationRequest request = new CalculationRequest(10.0, 5.0, "ADD");
        CalculationResult result = calculator.calculate(request);
        calculationResultStore.save(request, result);
    }
}
