package si.helpdesk.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import si.helpdesk.operator.Operator;
import si.helpdesk.user.User;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private static final long EXPIRES_IN_SECONDS = 3600L;
    private static final String ISSUER = "https://helpdesk.si";

    @ConfigProperty(name = "jwt.sign.key.location")
    String keyLocation;

    private PrivateKey privateKey;

    @PostConstruct
    void init() {
        try {
            String pem = keyLocation.startsWith("file:")
                    ? Files.readString(Path.of(URI.create(keyLocation)))
                    : new String(getClass().getClassLoader().getResourceAsStream(keyLocation).readAllBytes(), StandardCharsets.UTF_8);
            privateKey = KeyUtils.decodePrivateKey(pem);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load signing key from: " + keyLocation, e);
        }
    }

    public TokenResponse loginUser(String username, String password) {
        User user = User.findByUsername(username);
        if (user == null || !BcryptUtil.matches(password, user.passwordHash)) {
            throw new UnauthorizedLoginException("Invalid credentials");
        }
        try {
            String token = Jwt.issuer(ISSUER)
                    .subject(username)
                    .upn(username)
                    .groups(Set.of("USER"))
                    .claim("userId", user.id)
                    .expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS))
                    .sign(privateKey);
            return new TokenResponse(token, EXPIRES_IN_SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    public TokenResponse loginOperator(String username, String password) {
        Operator operator = Operator.findByUsername(username);
        if (operator == null || !BcryptUtil.matches(password, operator.passwordHash)) {
            throw new UnauthorizedLoginException("Invalid credentials");
        }
        try {
            String token = Jwt.issuer(ISSUER)
                    .subject(username)
                    .upn(username)
                    .groups(Set.of("OPERATOR"))
                    .claim("operatorId", operator.id)
                    .expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS))
                    .sign(privateKey);
            return new TokenResponse(token, EXPIRES_IN_SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    public static class UnauthorizedLoginException extends RuntimeException {
        public UnauthorizedLoginException(String message) {
            super(message);
        }
    }
}
