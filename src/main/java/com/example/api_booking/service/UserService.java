package com.example.api_booking.service;

import com.example.api_booking.common.exception.BusinessException;
import com.example.api_booking.repository.UserRepository;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository users;
  private final PasswordEncoder passwords;

  public UserService(UserRepository users, PasswordEncoder passwords) {
    this.users = users;
    this.passwords = passwords;
  }

  public record UserResponseDTO(
      long id,
      String email,
      String name,
      String fullName,
      String role,
      List<String> roles,
      String status,
      String phone) {}

  public UserRepository.UserRow current() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null || !a.isAuthenticated())
      throw new BusinessException(401, "AUTHENTICATION_REQUIRED", "Cần đăng nhập");
    var u = users.byEmail(a.getName());
    if (!u.enabled()) throw new BusinessException(403, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa");
    return u;
  }

  public UserRepository.UserRow require(String role) {
    var u = current();
    if (!users.roles(u.id()).contains(role))
      throw new BusinessException(403, "ACCESS_DENIED", "Không có quyền thực hiện");
    return u;
  }

  public UserResponseDTO dto(UserRepository.UserRow u) {
    return new UserResponseDTO(
        u.id(),
        u.email(),
        u.fullName(),
        u.fullName(),
        u.primaryRole(),
        users.roles(u.id()),
        u.enabled() ? "ACTIVE" : "LOCKED",
        u.phone());
  }

  @Transactional
  public UserResponseDTO updateProfile(com.example.api_booking.dto.ProfileUpdateRequest request) {
    if (!request.hasFullName() && !request.hasPhone())
      throw BusinessException.invalid("Cần gửi fullName hoặc phone");
    if (request.hasFullName() && (request.getFullName() == null || request.getFullName().isBlank()))
      throw BusinessException.invalid("Họ tên không được rỗng");
    var actor = current();
    var before = users.find(actor.id(), true);
    String name = request.hasFullName() ? request.getFullName().strip() : before.fullName();
    String phone = request.hasPhone() ? request.getPhone() : before.phone();
    users.updateProfile(actor.id(), name, phone);
    users.audit(actor.id(), "PROFILE_UPDATED", actor.id());
    return dto(users.find(actor.id(), false));
  }

  @Transactional
  public void changePassword(String oldPassword, String newPassword, String confirm) {
    var u = users.find(current().id(), true);
    if (!passwords.matches(oldPassword, u.passwordHash()))
      throw BusinessException.invalid("Mật khẩu cũ không đúng");
    if (!newPassword.equals(confirm)
        || newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw BusinessException.invalid("Mật khẩu xác nhận không khớp hoặc vượt 72 byte");
    users.password(u.id(), passwords.encode(newPassword));
    users.audit(u.id(), "PASSWORD_CHANGED", u.id());
  }
}
