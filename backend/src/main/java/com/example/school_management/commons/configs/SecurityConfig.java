package com.example.school_management.commons.configs;

import com.example.school_management.commons.filter.RateLimitingFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationEntryPoint unauthorizedHandler;
  private final JwtAuthenticationFilter       jwtAuthFilter;
  private final RateLimitingFilter           rateLimitingFilter;

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exc -> exc
                    .authenticationEntryPoint(unauthorizedHandler)
            )
            .sessionManagement(sess -> sess
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                    // Account provisioning is admin-only; must precede the public /api/auth/** rule.
                    .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                    // Changes the caller's own password; must precede the public /api/auth/** rule.
                    .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                    .requestMatchers("/api/auth/**", "/actuator/**").permitAll()
                    // Staff-managed people directories; everything else under /api/admin is admin-only.
                    .requestMatchers(
                            "/api/admin/teachers/**",
                            "/api/admin/parent-management/**",
                            "/api/admin/staff/**"
                    ).hasAnyRole("ADMIN", "STAFF")
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/ws/**", "/ws-native/**").permitAll() // Allow WebSocket endpoints
                    .requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/webjars/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            );


    // Add rate limiting filter before JWT authentication
    http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  // The rate limiter belongs to the security chain only; stop Boot from also registering it as a servlet filter.
  @Bean
  FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(RateLimitingFilter filter) {
    FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(List.of("http://localhost:5173"));
    cors.setAllowedMethods(List.of("GET", "POST", "PUT","PATCH","DELETE", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    cors.setAllowCredentials(true);
    cors.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors); // apply to every API route
    source.registerCorsConfiguration("/ws/**", cors); // apply to WebSocket endpoints
    return source;
  }

  @Bean
  RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("""
            GRADE_WRITE   > GRADE_READ
            STUDENT_DELETE > STUDENT_UPDATE
            STUDENT_UPDATE > STUDENT_READ
            TEACHER_DELETE > TEACHER_UPDATE
            TEACHER_UPDATE > TEACHER_READ
            ADMIN > (GRADE_WRITE STUDENT_DELETE TEACHER_DELETE)
        """);
  }
  @Bean
  public static Jackson2ObjectMapperBuilderCustomizer addDefaultFieldFilter() {
    // Serialize *everything* when no MappingJacksonValue supplies a filter:
    var provider = new SimpleFilterProvider()
            .setDefaultFilter(SimpleBeanPropertyFilter.serializeAll())
            .setFailOnUnknownId(false);           // do NOT throw for unknown ids

    return builder -> builder.filters(provider);
  }
}
