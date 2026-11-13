package io.template.execution;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.template.environment.EnvironmentVariables;
import io.template.samplebusinesslayer.CalculationResultStore;
import io.template.samplebusinesslayer.Calculator;
import io.template.samplebusinesslayer.models.CalculationRequest;
import io.template.samplebusinesslayer.models.CalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.template.testsupport.SampleJsonInputs.validInput;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutorTest {

    @Mock
    private Calculator calculator;

    @Mock
    private CalculationResultStore calculationResultStore;

    @Mock
    private EnvironmentVariables environmentVariables;

    private Executor executor;

    @BeforeEach
    void setUp() {
        when(environmentVariables.stage()).thenReturn("BETA");
        when(environmentVariables.awsRegion()).thenReturn("us-east-1");

        executor = new Executor(environmentVariables, calculator, calculationResultStore);
    }

    @Test
    void rejectsMissingArguments() {
        assertThrows(IllegalArgumentException.class, () -> executor.execute(null));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(new String[0]));
    }

    @Test
    void executesWithValidInput() {
        String[] args = new String[]{validInput("hello", 3, true, "2024-01-01T00:00:00Z")};

        CalculationResult calculationResult = new CalculationResult(15.0, "ADD");

        when(calculator.calculate(any(CalculationRequest.class))).thenReturn(calculationResult);

        executor.execute(args);

        verify(environmentVariables).stage();
        verify(environmentVariables).awsRegion();

        verify(calculator).calculate(argThat(request ->
                request.operandA() == 10.0 &&
                request.operandB() == 5.0 &&
                "ADD".equals(request.operation())
        ));

        verify(calculationResultStore).save(new CalculationRequest(10.0, 5.0, "ADD"), calculationResult);
    }

    @SuppressFBWarnings(
            value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT",
            justification = "Mockito in-order verification; return values are irrelevant"
    )
    @Test
    void executesInCorrectOrder() {
        String[] args = new String[]{validInput("hello", 3, true, "2024-01-01T00:00:00Z")};

        CalculationResult calculationResult = new CalculationResult(15.0, "ADD");

        when(calculator.calculate(any(CalculationRequest.class))).thenReturn(calculationResult);

        executor.execute(args);

        InOrder inOrder = inOrder(environmentVariables, calculator, calculationResultStore);
        inOrder.verify(environmentVariables).stage();
        inOrder.verify(environmentVariables).awsRegion();
        inOrder.verify(calculator).calculate(argThat(request ->
                request.operandA() == 10.0 &&
                request.operandB() == 5.0 &&
                "ADD".equals(request.operation())
        ));

        inOrder.verify(calculationResultStore).save(new CalculationRequest(10.0, 5.0, "ADD"), calculationResult);
    }

    @Test
    void rejectsInvalidInputBeforeCalculation() {
        String[] args = new String[]{"invalid-input"};

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(args)
        );

        verify(environmentVariables).stage();
        verify(environmentVariables).awsRegion();

        verify(calculator, never()).calculate(any(CalculationRequest.class));
        verify(calculationResultStore, never()).save(any(CalculationRequest.class), any(CalculationResult.class));
    }
}
