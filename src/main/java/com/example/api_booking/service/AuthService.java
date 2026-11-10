package com.example.api_booking.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.api_booking.dto.RegisterRequest;
import com.example.api_booking.dto.RegisterResponse;
import com.example.api_booking.entity.UserRole;
import com.example.api_booking.entity.AppUser;
import com.example.api_booking.repository.AppUserRepository;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command, HttpServletRequest request, HttpServletResponse response) {
        String email = command.email().trim().toLowerCase();
        AppUser user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new InvalidCredentialsException());

        if (!user.isEnabled() || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        HttpSession session = request.getSession(true);
        request.changeSessionId();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        return new LoginResult(user.getId(), user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public LoginResult currentUser(String email) {
        AppUser user = users.findByEmailIgnoreCase(email)
            .orElseThrow(InvalidCredentialsException::new);
        return new LoginResult(user.getId(), user.getEmail(), user.getRole().name());
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
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        AppUser user = users.save(new AppUser(
            email,
            passwordEncoder.encode(request.password()),
            UserRole.CUSTOMER));
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole().name());
    }

    public record LoginCommand(String email, String password) {}
    public record LoginResult(Long id, String email, String role) {}

    public static class InvalidCredentialsException extends RuntimeException {}

    public static class EmailAlreadyRegisteredException extends RuntimeException {}
}
