package io.template.shared.utilities;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HibernateValidatorUtilityTest {

    @Test
    void validatesSelectedPropertiesWithoutRequiringUnselectedProperties() {
        Settings settings = new Settings("valid", null);

        assertDoesNotThrow(() -> HibernateValidatorUtility.validateProperties(settings, "first"));
        assertThrows(ValidationException.class, () -> HibernateValidatorUtility.validate(settings));
    }

    @Test
    void rejectsAnInvalidPropertyAmongSelectedProperties() {
        Settings settings = new Settings("valid", " ");

        assertThrows(
                ValidationException.class,
                () -> HibernateValidatorUtility.validateProperties(settings, "first", "second")
        );
    }

    @Test
    void rejectsNullObjects() {
        assertThrows(
                ValidationException.class,
                () -> HibernateValidatorUtility.validateProperties(null, "first")
        );
    }

    @Test
    void rejectsUnknownPropertyNames() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HibernateValidatorUtility.validateProperties(new Settings("valid", "valid"), "missing")
        );
    }

    private record Settings(@NotBlank String first, @NotBlank String second) { }
}
