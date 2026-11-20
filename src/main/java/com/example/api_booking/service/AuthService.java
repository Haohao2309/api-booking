package com.example.api_booking.service;

import com.example.api_booking.dto.RegisterRequest;
import com.example.api_booking.entity.AppUser;
import com.example.api_booking.entity.UserRole;
import com.example.api_booking.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final com.example.api_booking.repository.UserRepository accounts;
  private final UserService userService;

  public AuthService(
      AppUserRepository users,
      PasswordEncoder passwordEncoder,
      com.example.api_booking.repository.UserRepository accounts,
      UserService userService) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.accounts = accounts;
    this.userService = userService;
  }

  @Transactional
  public UserService.UserResponseDTO login(
      LoginCommand command, HttpServletRequest request, HttpServletResponse response) {
    String email = command.email().trim().toLowerCase(java.util.Locale.ROOT);
    AppUser user =
        users.findByEmailIgnoreCase(email).orElseThrow(() -> new InvalidCredentialsException());

    if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    if (!user.isEnabled())
      throw new com.example.api_booking.common.exception.BusinessException(
          403, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa");
    var account = accounts.find(user.getId(), true);
    if (!account.enabled() || !passwordEncoder.matches(command.password(), account.passwordHash()))
      throw new InvalidCredentialsException();
    accounts.login(user.getId());
    HttpSession session = request.getSession(true);
    request.changeSessionId();
    new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository()
        .saveToken(null, request, response);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            new com.example.api_booking.config.SessionIdentity(
                user.getEmail(), account.securityVersion()),
            null,
            accounts.roles(user.getId()).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    return userService.dto(accounts.find(user.getId(), false));
  }

  @Transactional(readOnly = true)
  public UserService.UserResponseDTO currentUser(String email) {
    AppUser user = users.findByEmailIgnoreCase(email).orElseThrow(InvalidCredentialsException::new);
    return userService.dto(accounts.find(user.getId(), false));
  }

  /** Clears the authenticated security context and invalidates the current session. */
  public void logout(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  /**
   * Registers a new customer account with a BCrypt password hash.
   *
   * @param request validated registration data
   * @return public account data; the password hash is never returned
   * @throws EmailAlreadyRegisteredException when the email is already used
   */
  @Transactional
  public UserService.UserResponseDTO register(RegisterRequest request) {
    if (request.confirmPassword() != null && !request.password().equals(request.confirmPassword()))
      throw com.example.api_booking.common.exception.BusinessException.invalid(
          "Xác nhận mật khẩu không khớp");
    if (request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw com.example.api_booking.common.exception.BusinessException.invalid(
          "Mật khẩu tối đa 72 byte UTF-8");
    String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
    if (users.existsByEmailIgnoreCase(email)) {
      throw new EmailAlreadyRegisteredException();
    }

    AppUser user =
        users.saveAndFlush(
            new AppUser(email, passwordEncoder.encode(request.password()), UserRole.CUSTOMER));
    accounts.profile(user.getId(), request.fullName() == null ? null : request.fullName().trim());
    accounts.assign(user.getId(), java.util.List.of(2L));
    return userService.dto(accounts.find(user.getId(), false));
  }

  public record LoginCommand(String email, String password) {}

  public record LoginResult(Long id, String email, String role) {}

  public static class InvalidCredentialsException extends RuntimeException {}

  public static class EmailAlreadyRegisteredException extends RuntimeException {}
}
