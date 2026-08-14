package br.cadi.iam.application;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${cadi.jwt.secret}")
    private String secretKey;

    @Value("${cadi.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Alem de {@code role}, inclui {@code uid} (id do usuario) quando disponivel.
     *
     * <p>Numa arquitetura distribuida o token precisa ser autossuficiente: sem
     * {@code uid}/{@code pid} cada servico teria de consultar o core a cada
     * requisicao so para saber quem e o portador. O {@code pid} (id da pessoa)
     * e injetado por quem monta o login, via {@code extraClaims}.</p>
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("role", userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null));
        if (userDetails instanceof br.cadi.iam.domain.User user) {
            claims.put("uid", user.getId());
        }
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }


    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        // Updated to use a safer approach for the provided Hex/Base64 key
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (Exception e) {
            keyBytes = secretKey.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Long getAuthenticatedUserId() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String userId = authentication.getName();

        return Long.parseLong(userId);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
}
