package si.helpdesk.auth;

public class TokenResponse {
    public String token;
    public long expiresIn;

    public TokenResponse(String token, long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}
