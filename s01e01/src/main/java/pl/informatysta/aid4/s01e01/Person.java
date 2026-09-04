package pl.informatysta.aid4.s01e01;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

public record Person(
    String name,
    String surname,
    String gender,
    LocalDate birthDate,
    String birthPlace,
    String birthCountry,
    String job
) {
    public String fingerprint() {
        var birthDateString = birthDate != null
                ? birthDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : null;

        String sourceString = String.join("|", name, surname, gender, birthDateString, birthPlace, birthCountry, job);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(sourceString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not instantiate SHA-256 algorithm", e);
        }
    }
}