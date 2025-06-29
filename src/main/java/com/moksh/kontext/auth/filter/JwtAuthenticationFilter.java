package com.moksh.kontext.auth.filter;

import com.moksh.kontext.auth.service.TokenRedisService;
import com.moksh.kontext.auth.util.JwtUtil;
import com.moksh.kontext.user.entity.User;
import com.moksh.kontext.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenRedisService tokenRedisService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // Validate token step by step to provide specific error information
                if (!jwtUtil.isTokenValid(jwt)) {
                    setJwtErrorAttributes(request, "JWT token has expired or is invalid", 4303);
                } else if (!jwtUtil.isAccessToken(jwt)) {
                    setJwtErrorAttributes(request, "Invalid access token provided", 4307);
                } else if (!tokenRedisService.isAccessTokenValid(jwt)) {
                    setJwtErrorAttributes(request, "Token has been blacklisted. Please login again", 4308);
                } else {
                    UUID userId = jwtUtil.getUserIdFromToken(jwt);
                    
                    Optional<User> userOptional = userRepository.findById(userId);
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        
                        // Check if user is active and email is verified
                        if (user.getIsActive() && user.getIsEmailVerified()) {
                            UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(
                                            user, 
                                            null, 
                                            user.getAuthorities()
                                    );
                            
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            log.debug("Successfully authenticated user: {}", user.getEmail());
                        } else {
                            setJwtErrorAttributes(request, "Your account has been deactivated. Please contact support", 4201);
                        }
                    } else {
                        setJwtErrorAttributes(request, "User associated with token not found", 4001);
                    }
                }
            } else {
                setJwtErrorAttributes(request, "JWT token is missing from request", 4305);
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication", e);
            setJwtErrorAttributes(request, "JWT token is malformed or invalid", 4304);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    private void setJwtErrorAttributes(HttpServletRequest request, String errorMessage, Integer errorCode) {
        request.setAttribute("jwt.error", errorMessage);
        request.setAttribute("jwt.error.code", errorCode);
        log.debug("JWT validation failed: {}", errorMessage);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT filter for public endpoints
        return path.startsWith("/api/auth/") ||
               path.startsWith("/api/users/email/") ||
               path.startsWith("/api/users/google/") ||
               path.equals("/api/users") && "POST".equals(request.getMethod()) ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}