package com.example.api_booking.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 320) private String email;
    @Column(name = "code_hash", nullable = false, length = 255) private String codeHash;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean used = false;

    protected PasswordResetToken() {}
    public PasswordResetToken(String email, String codeHash, Instant expiresAt) {
        this.email = email; this.codeHash = codeHash; this.expiresAt = expiresAt;
    }
    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public void markUsed() { this.used = true; }
}
