
package com.ram.project.documentsummerizer.service;

import com.ram.project.documentsummerizer.model.User;
import com.ram.project.documentsummerizer.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration; 

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    private AuthenticationManager authenticationManager; 

    private static final String SECRET_STRING = "aSuperSecretKeyForBriefifyAppThatIsAtLeast32BytesLongAndShouldBeVerySecure";
    private final SecretKey jwtSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_STRING));

    private final long jwtExpirationMinutes = 1440; 

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
//using @Lazy here
    public void setAuthenticationManager(@Lazy AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public User registerUser(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }
        User newUser = new User(username, passwordEncoder.encode(password));
        return userRepository.save(newUser);
    }

    public String authenticateUserAndGenerateToken(String username, String password) {
        
        if (this.authenticationManager == null) { 
            throw new IllegalStateException("AuthenticationManager has not been set in AuthService.");
        }
        Authentication authentication;
        try {
            authentication = this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (Exception e) {
            System.err.println("Authentication failed for user " + username + ": " + e.getMessage());
            throw new RuntimeException("Authentication failed: Invalid credentials or internal error.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return generateJwtToken(authentication);
    }

    private String generateJwtToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("id", userPrincipal.getId())
                .claim("username", userPrincipal.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(jwtExpirationMinutes, ChronoUnit.MINUTES)))
                .signWith(jwtSecret)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }
}
