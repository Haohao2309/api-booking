package com.example.api_booking.service;

import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.repository.PasswordResetRepository;
import com.example.api_booking.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Forgot-password use case: request a 6-digit code by email, verify it, then set a new password.
 *
 * <p>Responses never reveal whether an email is registered. Each account has at most one usable
 * code; it expires after 10 minutes, is retired after 5 wrong attempts and is single-use. All
 * steps lock the account row so concurrent attempts are serialised.
 */
@Service
public class PasswordResetService {
  static final Duration CODE_TTL = Duration.ofMinutes(10);
  static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
  static final int MAX_FAILED_ATTEMPTS = 5;

  private static final String DUMMY_CODE = "000000";

  private final UserRepository users;
  private final PasswordResetRepository codes;
  private final PasswordEncoder encoder;
  private final PasswordResetMailer mailer;
  private final SecureRandom random = new SecureRandom();

  public PasswordResetService(
      UserRepository users,
      PasswordResetRepository codes,
      PasswordEncoder encoder,
      PasswordResetMailer mailer) {
    this.users = users;
    this.codes = codes;
    this.encoder = encoder;
    this.mailer = mailer;
  }

  private static BusinessException invalidCode() {
    return new BusinessException(
        400, "INVALID_RESET_CODE", "Mã xác nhận không đúng, đã hết hạn hoặc đã bị khóa");
  }

  private static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  @Transactional
  public void requestReset(String rawEmail) {
    String email = normalize(rawEmail);
    var found = users.findByEmail(email);
    if (found.isEmpty() || !found.get().enabled()) {
      // Spend comparable hashing time so response timing does not reveal registered emails.
      encoder.encode(DUMMY_CODE);
      return;
    }
    var user = users.find(found.get().id(), true);
    Instant now = Instant.now();
    var latest = codes.latest(user.id());
    if (latest.isPresent()
        && latest.get().usedAt() == null
        && latest.get().createdAt().plus(RESEND_COOLDOWN).isAfter(now)) return;
    codes.retireActive(user.id(), now);
    String code = String.format("%06d", random.nextInt(1_000_000));
    codes.create(user.id(), encoder.encode(code), now.plus(CODE_TTL), now);
    users.audit(user.id(), "PASSWORD_RESET_REQUESTED", user.id());
    String recipient = user.email();
    // Mail goes out only after the code is committed; SMTP never runs inside the transaction.
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            mailer.send(recipient, code);
          }
        });
  }

  /** Wrong codes are counted, so the failure is committed even though the call throws. */
  @Transactional(noRollbackFor = BusinessException.class)
  public void verify(String email, String code) {
    requireValidCode(email, code);
  }

  @Transactional(noRollbackFor = BusinessException.class)
  public void reset(String email, String code, String newPassword, String confirmPassword) {
    if (!newPassword.equals(confirmPassword))
      throw BusinessException.invalid("Xác nhận mật khẩu không khớp");
    if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72)
      throw BusinessException.invalid("Mật khẩu tối đa 72 byte UTF-8");
    var valid = requireValidCode(email, code);
    Instant now = Instant.now();
    // password() bumps security_version, which invalidates every existing session.
    users.password(valid.userId(), encoder.encode(newPassword));
    codes.used(valid.codeId(), now);
    users.audit(valid.userId(), "PASSWORD_RESET", valid.userId());
  }

  private record ValidCode(long userId, long codeId) {}

  private ValidCode requireValidCode(String rawEmail, String code) {
    var found = users.findByEmail(normalize(rawEmail));
    if (found.isEmpty()) {
      encoder.matches(code, encoder.encode(DUMMY_CODE));
      throw invalidCode();
    }
    var user = users.find(found.get().id(), true);
    if (!user.enabled()) throw invalidCode();
    var latest = codes.latest(user.id()).orElseThrow(PasswordResetService::invalidCode);
    if (latest.usedAt() != null
        || !Instant.now().isBefore(latest.expiresAt())
        || latest.failedAttempts() >= MAX_FAILED_ATTEMPTS) throw invalidCode();
    if (!encoder.matches(code, latest.codeHash())) {
      codes.failed(latest.id());
      throw invalidCode();
    }
    return new ValidCode(user.id(), latest.id());
  }
}
