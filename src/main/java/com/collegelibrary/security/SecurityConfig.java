package com.collegelibrary.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * Everything about "who may do what".
 *
 * Three separate concerns live here:
 *  1. Authentication  — who are you?           (/api/auth/login)
 *  2. Authorization   — what may you do?       (role rules below)
 *  3. CSRF protection — is the request really
 *                       from our own page?     (XSRF-TOKEN cookie)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfig.class);

    /** Cookie-based CSRF token: readable by JavaScript on purpose,
     *  so our SPA can echo it back in the X-XSRF-TOKEN header. */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /** Passwords are stored as BCrypt hashes, never as plain text. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The object that actually verifies a login attempt:
     * load the account (UserDetailsService) + compare the password (PasswordEncoder).
     * Spring Boot does not create this bean for us once we define our own
     * SecurityFilterChain, so we declare it explicitly.
     */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Where the "who is logged in" context is stored between requests.
     * We use the standard HTTP session (JSESSIONID cookie) and declare the
     * bean explicitly so AuthService can save/expire the very same object.
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CsrfTokenRepository csrfRepository,
                                            SecurityContextRepository securityContextRepository) throws Exception {

        http
            // 1. CSRF: cookie + header instead of a hidden form field (SPA friendly).
            //    Spring Security 7 defaults to the XOR handler, which expects the
            //    header to be base64(xor(mask, token)). Our JS sends the XSRF-TOKEN
            //    cookie value as-is, so we explicitly choose the plain handler that
            //    compares raw values.
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfRepository)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))

            // 2. We log in through our own JSON endpoint, so the default
            //    form login, HTTP basic and logout pages are switched off.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())

            // Store the logged-in user in the HTTP session.
            .securityContext(context -> context.securityContextRepository(securityContextRepository))

            // Sessions are created normally (cookie JSESSIONID), but never
            // created for anonymous API calls.
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // 3. Authorization rules — order matters: first match wins.
            .authorizeHttpRequests(auth -> auth
                    // public: the login page + its assets
                    .requestMatchers("/", "/index.html", "/error", "/favicon.ico").permitAll()
                    .requestMatchers(HttpMethod.GET, "/css/**", "/js/**", "/assets/**").permitAll()

                    // public: start a session
                    .requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll()

                    // admin area (HTML): admin or librarian only
                    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "LIBRARIAN")

                    // user management is ADMIN only
                    .requestMatchers("/api/admin/users/**").hasRole("ADMIN")

                    // books / issues / dashboard: admin or librarian
                    .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "LIBRARIAN")

                    // every other API call needs a logged-in user
                    .requestMatchers("/api/**").authenticated()

                    .anyRequest().authenticated())

            // 4. Nice errors: JSON for APIs, redirect to login for pages
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((request, response, authException) -> {
                        if (request.getRequestURI().startsWith("/api/")) {
                            writeJson(response, 401, "You are not signed in");
                        } else {
                            // No special entry point class needed — just send
                            // the browser back to the login page.
                            response.sendRedirect("/index.html");
                        }
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        // Knowing WHY access was denied (CSRF vs. wrong role)
                        // makes debugging much faster.
                        log.warn("Access denied: {} {} -> {}",
                                request.getMethod(), request.getRequestURI(), accessDeniedException.toString());
                        if (request.getRequestURI().startsWith("/api/")) {
                            writeJson(response, 403, "You do not have permission to do that");
                        } else {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                        }
                    }));

        return http.build();
    }

    private static void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
