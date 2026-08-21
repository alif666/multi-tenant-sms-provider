package one.formwork.channel.sms.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import one.formwork.channel.sms.auth.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long expirationMinutes;

    public JwtService(@Value("${formwork.security.jwt-secret:change-this-development-secret-which-must-be-at-least-32-bytes}") String secret,
                      @Value("${formwork.security.jwt-issuer:formwork-sms}") String issuer,
                      @Value("${formwork.security.jwt-expiration-minutes:60}") long expirationMinutes) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .claim("userId", user.getId().toString())
                .claim("roles", user.getAuthorities().stream().map(a -> a.getAuthority()).toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
