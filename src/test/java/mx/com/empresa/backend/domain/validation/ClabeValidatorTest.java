package mx.com.empresa.backend.domain.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClabeValidatorTest {

    @Test
    void shouldAcceptValidClabe() {
        assertTrue(ClabeValidator.isValid("032180000118359719"));
    }

    @Test
    void shouldRejectIncorrectLength() {
        assertFalse(ClabeValidator.isValid("03218000011835971"));
    }

    @Test
    void shouldRejectNonNumericCharacters() {
        assertFalse(ClabeValidator.isValid("03218000011835971A"));
    }

    @Test
    void shouldRejectIncorrectVerifierDigit() {
        assertFalse(ClabeValidator.isValid("032180000118359718"));
    }

    @Test
    void shouldRejectNullValue() {
        assertFalse(ClabeValidator.isValid(null));
    }

    @Test
    void shouldRejectEmptyValue() {
        assertFalse(ClabeValidator.isValid(""));
    }

    @Test
    void shouldRejectClabeWithSpaces() {
        assertFalse(ClabeValidator.isValid("03218000011835971 "));
    }

    @Test
    void shouldAcceptValidClabeWithLeadingZeros() {
        assertTrue(ClabeValidator.isValid("000000000000000000"));
    }
}
