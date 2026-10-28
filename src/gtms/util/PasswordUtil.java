package gtms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Băm mật khẩu bằng SHA-256 kèm salt ngẫu nhiên. */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() { }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashed = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            // JDK nào cũng có SHA-256, vào đây là do môi trường Java bị lỗi
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", e);
        }
    }

    /** Ít nhất 6 ký tự. */
    public static boolean isStrong(String password) {
        return password != null && password.length() >= 6;
    }
}
