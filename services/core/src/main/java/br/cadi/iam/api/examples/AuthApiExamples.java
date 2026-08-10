package br.cadi.iam.api.examples;

public final class AuthApiExamples {

    public static final String SEED_PASSWORD = "CADI@123";

    public static final String AUTH_ADMIN =
            "{\"email\":\"admin@cadi.com\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_PROFESSOR =
            "{\"email\":\"teacher@cadi.com\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_STUDENT =
            "{\"email\":\"student@cadi.com\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_PARENT =
            "{\"email\":\"parent@cadi.com\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_SECRETARY =
            "{\"email\":\"secretary@cadi.com\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_PSYCHOLOGIST =
            "{\"email\":\"psychologist@cadi.com\",\"password\":\"" + SEED_PASSWORD + "\"}";

    public static final String AUTH_FINANCE =
            "{\"email\":\"financeiro@cadi.com\",\"password\":\"" + SEED_PASSWORD + "\"}";

    private AuthApiExamples() {
    }
}
