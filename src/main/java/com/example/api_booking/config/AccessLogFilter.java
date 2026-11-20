package com.example.api_booking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Records one safe access log entry for every HTTP request. */
@Component
public class AccessLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actor = Optional.ofNullable(authentication)
                    .filter(Authentication::isAuthenticated)
                    .map(Authentication::getName)
                    .orElse("anonymous");
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            log.info("ACCESS method={} path={} status={} durationMs={} actor={} ip={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(),
                    durationMs, actor, request.getRemoteAddr());
        }
    }
}
