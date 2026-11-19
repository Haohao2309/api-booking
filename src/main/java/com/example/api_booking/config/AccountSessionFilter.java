package com.example.api_booking.config;

import com.example.api_booking.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccountSessionFilter extends OncePerRequestFilter {
  private final UserRepository users;

  public AccountSessionFilter(UserRepository users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof SessionIdentity principal) {
      try {
        var user = users.byEmail(principal.email());
        if (!user.enabled() || user.securityVersion() != principal.version()) {
          SecurityContextHolder.clearContext();
          var session = request.getSession(false);
          if (session != null) session.invalidate();
        } else {
          var updated =
              new UsernamePasswordAuthenticationToken(
                  principal,
                  null,
                  users.roles(user.id()).stream()
                      .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                      .toList());
          var context = SecurityContextHolder.createEmptyContext();
          context.setAuthentication(updated);
          SecurityContextHolder.setContext(context);
        }
      } catch (com.example.api_booking.common.exception.BusinessException ex) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
      }
    }
    chain.doFilter(request, response);
  }
}
