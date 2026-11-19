package com.example.api_booking.config;

import com.example.api_booking.repository.AppUserRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.cors(Customizer.withDefaults())
        .csrf(Customizer.withDefaults())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.GET, "/api/health", "/api/auth/csrf", "/api/v1/auth/csrf")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/*")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/rooms")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/rooms/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/rooms/*")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/bookings")
                    .hasAnyRole("ADMIN", "RECEPTIONIST")
                    .requestMatchers(HttpMethod.POST, "/api/bookings")
                    .hasRole("CUSTOMER")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/bookings/*/confirm",
                        "/api/bookings/*/check-in",
                        "/api/bookings/*/check-out")
                    .hasAnyRole("ADMIN", "RECEPTIONIST")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) -> {
                          response.setStatus(401);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"success\":false,\"message\":\"Authentication"
                                      + " required\",\"data\":{\"code\":\"AUTHENTICATION_REQUIRED\"},\"status\":401}");
                        })
                    .accessDeniedHandler(
                        (request, response, exception) -> {
                          response.setStatus(403);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"success\":false,\"message\":\"Access denied or invalid CSRF"
                                      + " token\",\"data\":{\"code\":\"ACCESS_DENIED_OR_CSRF_INVALID\"},\"status\":403}");
                        }))
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(AppUserRepository users) {
    return username ->
        users
            .findByEmailIgnoreCase(username)
            .map(
                user ->
                    User.withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .disabled(!user.isEnabled())
                        .build())
            .orElseThrow(
                () ->
                    new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Account not found"));
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String origins) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(
        Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        List.of("Content-Type", "X-CSRF-TOKEN", "Idempotency-Key", "If-Match"));
    config.setAllowCredentials(true);
    config.setExposedHeaders(List.of("ETag", "Location"));
    config.setMaxAge(3600L);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}
