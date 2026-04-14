package si.helpdesk.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtSignatureBuilder;
import io.smallrye.jwt.util.KeyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import si.helpdesk.operator.Operator;
import si.helpdesk.user.User;

import java.security.PrivateKey;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private static final long EXPIRES_IN_SECONDS = 3600L;
    private static final String ISSUER = "https://helpdesk.si";

    @ConfigProperty(name = "jwt.private.key")
    Optional<String> privateKeyPem;

    private PrivateKey privateKey;

    @PostConstruct
    void init() {
        privateKeyPem.filter(s -> !s.isBlank()).ifPresent(pem -> {
            try {
                privateKey = KeyUtils.decodePrivateKey(pem);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode JWT_PRIVATE_KEY", e);
            }
        });
    }

    private String sign(JwtSignatureBuilder builder) throws Exception {
        if (privateKey != null) {
            return builder.sign(privateKey);
        }
        return builder.sign();
    }

    public TokenResponse loginUser(String username, String password) {
        User user = User.findByUsername(username);
        if (user == null || !BcryptUtil.matches(password, user.passwordHash)) {
            throw new UnauthorizedLoginException("Invalid credentials");
        }
        try {
            String token = sign(Jwt.issuer(ISSUER)
                    .subject(username)
                    .upn(username)
                    .groups(Set.of("USER"))
                    .claim("userId", user.id)
                    .expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS)));
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
            String token = sign(Jwt.issuer(ISSUER)
                    .subject(username)
                    .upn(username)
                    .groups(Set.of("OPERATOR"))
                    .claim("operatorId", operator.id)
                    .expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS)));
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
