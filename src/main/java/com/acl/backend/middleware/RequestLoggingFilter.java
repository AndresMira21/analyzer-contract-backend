package com.acl.backend.middleware;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String cid = Optional.ofNullable(request.getHeader("X-Correlation-Id")).filter(s -> !s.isBlank()).orElse(UUID.randomUUID().toString());
        MDC.put("cid", cid);
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String user = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                user = ((UserDetails) principal).getUsername();
            } else if (principal != null) {
                user = principal.toString();
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            long dur = System.currentTimeMillis() - start;
            response.setHeader("X-Correlation-Id", cid);
            if (user != null) {
                log.info("{} {} {} {}ms", method, uri, user, dur);
            } else {
                log.info("{} {} {}ms", method, uri, dur);
            }
            MDC.remove("cid");
        }
    }
}

