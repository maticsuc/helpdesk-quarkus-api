package si.helpdesk.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import si.helpdesk.operator.Operator;
import si.helpdesk.user.User;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private static final long EXPIRES_IN_SECONDS = 3600L;
    private static final String ISSUER = "https://helpdesk.si";

    public TokenResponse loginUser(String username, String password) {
        User user = User.findByUsername(username);
        if (user == null || !BcryptUtil.matches(password, user.passwordHash)) {
            throw new UnauthorizedLoginException("Invalid credentials");
        }
        String token = Jwt.issuer(ISSUER)
                .subject(username)
                .upn(username)
                .groups(Set.of("USER"))
                .claim("userId", user.id)
                .expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS))
                .sign();
        return new TokenResponse(token, EXPIRES_IN_SECONDS);
    }

    public TokenResponse loginOperator(String username, String password) {
        Operator operator = Operator.findByUsername(username);
        if (operator == null || !BcryptUtil.matches(password, operator.passwordHash)) {
            throw new UnauthorizedLoginException("Invalid credentials");
        }
        String token = Jwt.issuer(ISSUER)
                .subject(username)
                .upn(username)
                .groups(Set.of("OPERATOR"))
                .claim("operatorId", operator.id)
                .expiresIn(Duration.ofSeconds(EXPIRES_IN_SECONDS))
                .sign();
        return new TokenResponse(token, EXPIRES_IN_SECONDS);
    }

    public static class UnauthorizedLoginException extends RuntimeException {
        public UnauthorizedLoginException(String message) {
            super(message);
        }
    }
}
