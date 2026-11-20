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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import com.example.api_booking.dto.ForgotPasswordRequest;
import com.example.api_booking.dto.VerifyResetCodeRequest;
import com.example.api_booking.dto.ResetPasswordRequest;
import com.example.api_booking.entity.PasswordResetToken;
import com.example.api_booking.repository.PasswordResetTokenRepository;
import com.example.api_booking.dto.RegisterRequest;
import com.example.api_booking.dto.RegisterResponse;
import com.example.api_booking.entity.UserRole;
import com.example.api_booking.entity.AppUser;
import com.example.api_booking.repository.AppUserRepository;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository resetTokens;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder,
                       PasswordResetTokenRepository resetTokens, JavaMailSender mailSender) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.resetTokens = resetTokens;
        this.mailSender = mailSender;
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

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = normalize(request.email());
        users.findByEmailIgnoreCase(email).orElseThrow(EmailNotFoundException::new);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        resetTokens.save(new PasswordResetToken(email, passwordEncoder.encode(code), Instant.now().plus(Duration.ofMinutes(10))));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã xác nhận đặt lại mật khẩu Web Booking");
        message.setText("Mã xác nhận của bạn là: " + code + "\nMã có hiệu lực trong 10 phút.");
        mailSender.send(message);
    }

    @Transactional(readOnly = true)
    public void verifyResetCode(VerifyResetCodeRequest request) {
        findValidToken(request.email(), request.code());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = findValidToken(request.email(), request.code());
        AppUser user = users.findByEmailIgnoreCase(normalize(request.email())).orElseThrow(InvalidCredentialsException::new);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.markUsed();
        users.save(user);
        resetTokens.save(token);
    }

    private PasswordResetToken findValidToken(String email, String code) {
        PasswordResetToken token = resetTokens.findTopByEmailOrderByIdDesc(normalize(email)).orElseThrow(InvalidResetCodeException::new);
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now()) || !passwordEncoder.matches(code, token.getCodeHash())) {
            throw new InvalidResetCodeException();
        }
        return token;
    }

    private String normalize(String email) { return email.trim().toLowerCase(); }

    public record LoginCommand(String email, String password) {}
    public record LoginResult(Long id, String email, String role) {}

    public static class InvalidCredentialsException extends RuntimeException {}

    public static class EmailAlreadyRegisteredException extends RuntimeException {}
    public static class EmailNotFoundException extends RuntimeException {}
    public static class InvalidResetCodeException extends RuntimeException {}
}
