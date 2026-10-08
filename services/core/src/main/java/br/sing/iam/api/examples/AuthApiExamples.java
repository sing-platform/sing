package br.sing.iam.api.examples;

public final class AuthApiExamples {

    public static final String SEED_PASSWORD = "SING@123";

    public static final String AUTH_ADMIN =
            "{\"email\":\"admin@sing.local\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_PROFESSOR =
            "{\"email\":\"teacher@sing.local\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_STUDENT =
            "{\"email\":\"student@sing.local\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_PARENT =
            "{\"email\":\"parent@sing.local\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_SECRETARY =
            "{\"email\":\"secretary@sing.local\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_PSYCHOLOGIST =
            "{\"email\":\"psychologist@sing.local\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_FINANCE =
            "{\"email\":\"financeiro@sing.local\",\"password\":\"" + SEED_PASSWORD + "\"}";

    private AuthApiExamples() {
    }
}
