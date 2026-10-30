package com.stadtmeldung.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository)
            throws Exception {

        http
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/api/health").permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/reports",
                    "/api/reports/**"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/me"
                ).authenticated()


                // Meldungen dürfen auch anonym erstellt werden
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/reports"
                ).permitAll()


                // Eigene Meldungen bearbeiten: Anmeldung erforderlich
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/reports/{id}"
                ).authenticated()


                // Nur Mitarbeiter und Administratoren dürfen den Status ändern
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/reports/{id}/status"
                ).hasAnyRole("EMPLOYEE", "ADMIN")


                // Kommentare öffentlich anzeigen
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/reports/{reportId}/comments"
                ).permitAll()

                // Kommentare nur mit Anmeldung erstellen
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/reports/{reportId}/comments"
                ).authenticated()

                // Eigene Kommentare nur mit Anmeldung bearbeiten
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/comments/{commentId}"
                ).authenticated()

                // Eigene Kommentare nur mit Anmeldung löschen
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/comments/{commentId}"
                ).authenticated()


                // Fotos einer Meldung öffentlich auflisten
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/reports/{reportId}/attachments"
                ).permitAll()

                // Einzelne Bilddateien öffentlich anzeigen
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/attachments/{attachmentId}/file"
                ).permitAll()

                // Fotos nur als angemeldeter Benutzer hochladen
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/reports/{reportId}/attachments"
                ).authenticated()


                .requestMatchers(
                    HttpMethod.POST,
                    "/api/reports/with-photo"
                ).permitAll()


                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/attachments/{attachmentId}"
                ).authenticated()



                .anyRequest().denyAll()
            )

            // Bei fehlender Anmeldung HTTP 401 liefern
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                )
            )

            // Sitzung sicher beenden
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(
                    new HttpStatusReturningLogoutSuccessHandler(
                        HttpStatus.NO_CONTENT
                    )
                )
            )

            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }
}