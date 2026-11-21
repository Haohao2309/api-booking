package com.example.api_booking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends the reset code. Runs after the database commit; a mail failure is logged and the user can
 * request a new code after the cooldown. Logs never contain the recipient or the code.
 */
@Component
public class PasswordResetMailer {
  private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

  private final ObjectProvider<JavaMailSender> sender;
  private final boolean enabled;
  private final String from;

  public PasswordResetMailer(
      ObjectProvider<JavaMailSender> sender,
      @Value("${app.mail.enabled:false}") boolean enabled,
      @Value("${app.mail.from:}") String from) {
    this.sender = sender;
    this.enabled = enabled;
    this.from = from;
  }

  public void send(String recipient, String code) {
    if (!enabled) {
      log.warn("Password reset code was not emailed because MAIL_ENABLED=false");
      return;
    }
    var mail = sender.getIfAvailable();
    if (mail == null || from == null || from.isBlank()) {
      log.error("Password reset code was not emailed: SMTP sender or MAIL_FROM is not configured");
      return;
    }
    try {
      var message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(recipient);
      message.setSubject("Mã đặt lại mật khẩu Web Booking");
      message.setText(
          "Mã xác nhận của bạn là: "
              + code
              + "\nMã có hiệu lực trong 10 phút và chỉ dùng được một lần."
              + "\nNếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");
      mail.send(message);
    } catch (RuntimeException e) {
      log.error("Password reset email failed type={}", e.getClass().getName());
    }
  }
}
