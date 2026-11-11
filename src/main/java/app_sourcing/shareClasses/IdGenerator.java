package app_sourcing.shareClasses;

import java.security.SecureRandom;
import java.time.Instant;

public class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DIGITS_LENGTH = 7;

    private IdGenerator() {}

    public static String generateId(String prefix) {
        if (prefix == null || prefix.length() != 2 || !prefix.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Prefix must be exactly 2 uppercase letters");
        }

        int bound = (int) Math.pow(10, DIGITS_LENGTH);
        int number = RANDOM.nextInt(bound);

        return prefix + String.format("%0" + DIGITS_LENGTH + "d", number);
    }
}
