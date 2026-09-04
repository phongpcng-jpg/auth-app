package com.example.authapp.security;

import com.example.authapp.entity.User;
import com.example.authapp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the JWT from the httpOnly cookie (not a header) and, if valid,
 * authenticates the request against the current DB state of the user.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String cookieName;

    public JwtAuthFilter(
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${app.jwt.cookie-name}") String cookieName
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        extractTokenFromCookies(request).ifPresent(token -> {
            if (jwtService.isTokenValid(token)) {
                UUID userId = jwtService.extractUserId(token);
                userRepository.findById(userId).ifPresent(this::authenticate);
            }
        });

        filterChain.doFilter(request, response);
    }

    private void authenticate(User user) {
        var authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private Optional<String> extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
