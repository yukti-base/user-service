package org.yuktisetu.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import java.security.PublicKey;

@Component
public class JwtTokenProvider {

    private final PublicKey publicKey;

    public JwtTokenProvider(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * @return parsed claims, or throws if the token is expired/malformed/forged.
     */
    public Claims verify(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException e) {
            throw new IllegalArgumentException("Token signature invalid", e);
        }
    }
}
