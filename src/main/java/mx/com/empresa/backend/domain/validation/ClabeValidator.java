package mx.com.empresa.backend.domain.validation;

public final class ClabeValidator {

    private static final int CLABE_LENGTH = 18;
    private static final int[] WEIGHTS = {3, 7, 1};

    private ClabeValidator() {
    }

    // Valida la integridad matemática de la CLABE.
    public static boolean isValid(String clabe) {
        if (clabe == null || clabe.length() != CLABE_LENGTH) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < CLABE_LENGTH - 1; i++) {
            char character = clabe.charAt(i);
            if (character < '0' || character > '9') {
                return false;
            }

            int digit = character - '0';

            // Repite los pesos 3, 7 y 1 mediante el operador módulo y acumula el residuo de cada producto.
            sum += (digit * WEIGHTS[i % WEIGHTS.length]) % 10;
        }

        char verifierCharacter = clabe.charAt(CLABE_LENGTH - 1);
        if (verifierCharacter < '0' || verifierCharacter > '9') {
            return false;
        }

        // El último módulo garantiza que el dígito verificador esté entre 0 y 9.
        int expectedVerifier = (10 - (sum % 10)) % 10;
        return expectedVerifier == verifierCharacter - '0';
    }
}
