package com.clinic.smartqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login-page",
                                "/register-page",
                                "/auth/login",
                                "/auth/verify-otp",
                                "/auth/resend-otp",
                                "/auth/register",
                                "/doctor/login-page",
                                "/doctor/register-page",
                                "/doctor/login",
                                "/doctor/register",
                                "/receptionist/login-page",
                                "/receptionist/register-page",
                                "/receptionist/login",
                                "/receptionist/register",
                                "/admin/login-page",
                                "/admin/register-page",
                                "/admin/login",
                                "/admin/register",
                                "/css/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/doctor/dashboard", "/doctor/start/**", "/doctor/complete/**", "/doctor/status/**", "/doctor/verify-emergency/**", "/doctor/logout").hasRole("DOCTOR")
                        .requestMatchers("/appointments/all", "/appointments/assign", "/appointments/cancel", "/serve-next-page", "/admin/logout", "/admin/dashboard", "/admin/approve-doctor").hasRole("ADMIN")
                        .requestMatchers("/receptionist/dashboard", "/receptionist/walkin/book", "/receptionist/patient-search", "/receptionist/logout").hasRole("RECEPTIONIST")
                        .requestMatchers("/take-token", "/appointments/take-token", "/appointments/book").hasRole("PATIENT")
                        .requestMatchers("/dashboard", "/appointments-page", "/history-page", "/appointments/prescription/download", "/auth/logout").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers("/appointments/current-token").hasAnyRole("PATIENT", "ADMIN", "DOCTOR", "RECEPTIONIST")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String path = request.getRequestURI();
                            if (path.startsWith("/doctor/")) {
                                response.sendRedirect("/doctor/login-page");
                            } else if (path.startsWith("/receptionist/")) {
                                response.sendRedirect("/receptionist/login-page");
                            } else if (path.startsWith("/admin/") || path.startsWith("/appointments/all") || path.startsWith("/serve-next-page")) {
                                response.sendRedirect("/admin/login-page");
                            } else {
                                response.sendRedirect("/login-page");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> response.sendRedirect("/error?error=Access+Denied"))
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Not used. Authentication is session-backed by custom login flows.");
        };
    }
}
