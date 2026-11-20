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
}
