package com.stadtmeldung.config;

import com.stadtmeldung.security.DatabaseUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository)
            throws Exception {
        http
            .securityContext(context -> context
                    .securityContextRepository(securityContextRepository))
            .csrf(csrf -> csrf
                    .csrfTokenRepository(
                            CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(
                            new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/health", "/api/auth/csrf").permitAll()
                    .requestMatchers(HttpMethod.POST,
                            "/api/login", "/api/register").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/session")
                            .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/logout")
                            .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/admin/employees")
                            .hasRole("ADMIN")
                    .requestMatchers("/api/admin/employees/**")
                            .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/reports")
                            .permitAll()
                    .requestMatchers(HttpMethod.GET,
                            "/api/reports/*/comments").permitAll()
                    .requestMatchers(HttpMethod.GET,
                            "/api/reports/*/attachments").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/reports")
                            .permitAll()
                    .requestMatchers(HttpMethod.POST,
                            "/api/reports/*/attachments").hasRole("CITIZEN")
                    .requestMatchers(HttpMethod.DELETE,
                            "/api/attachments/*").hasRole("CITIZEN")
                    .requestMatchers(HttpMethod.POST, "/api/reports/*/votes")
                            .hasRole("CITIZEN")
                    .requestMatchers(HttpMethod.DELETE, "/api/reports/*/votes")
                            .hasRole("CITIZEN")
                    .requestMatchers(HttpMethod.POST,
                            "/api/reports/*/comments/internal")
                            .hasAnyRole("EMPLOYEE", "ADMIN")
                    .requestMatchers(HttpMethod.POST,
                            "/api/reports/*/comments").hasRole("CITIZEN")
                    .requestMatchers(HttpMethod.DELETE, "/api/reports/*")
                            .hasAnyRole("EMPLOYEE", "ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/reports/*")
                            .hasAnyRole("EMPLOYEE", "ADMIN")
                    .requestMatchers("/api/**").denyAll()
                    .anyRequest().permitAll())
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, cause) ->
                            writeError(response,
                                    HttpStatus.UNAUTHORIZED,
                                    "Bitte melden Sie sich an."))
                    .accessDeniedHandler((request, response, cause) ->
                            writeError(response,
                                    HttpStatus.FORBIDDEN,
                                    "Diese Aktion ist nicht erlaubt.")))
            .logout(logout -> logout
                    .logoutUrl("/api/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler(
                            new HttpStatusReturningLogoutSuccessHandler(
                                    HttpStatus.NO_CONTENT)))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(
            repository.interfaces.UserRepository users) {
        return new DatabaseUserDetailsService(users);
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    private static void writeError(
                                   jakarta.servlet.http.HttpServletResponse response,
                                   HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"message\":\"" + message + "\"}");
    }
}
