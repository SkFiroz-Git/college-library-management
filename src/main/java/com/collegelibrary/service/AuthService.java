package com.collegelibrary.service;

import com.collegelibrary.dto.LoginRequest;
import com.collegelibrary.dto.LoginResponse;
import com.collegelibrary.exception.UnauthorizedException;
import com.collegelibrary.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Login / logout / "who am I".
 *
 * Flow of a login:
 *   form -> AuthController -> this service -> AuthenticationManager
 *        -> UserDetailsService loads the account
 *        -> PasswordEncoder compares the hash
 *        -> session is created (JSESSIONID cookie) + context saved
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.identifier().trim(), request.password()));

            // Put the authenticated user into the context and store it in the session
            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();
            return toResponse(principal);

        } catch (DisabledException ex) {
            throw new UnauthorizedException("This account is deactivated. Please contact the library administrator.");
        } catch (AuthenticationException ex) {
            // Same message for "unknown user" and "wrong password" on purpose:
            // otherwise an attacker could guess valid email addresses.
            throw new UnauthorizedException("Invalid college ID/email or password.");
        }
    }

    public LoginResponse currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            throw new UnauthorizedException("You are not signed in");
        }
        return toResponse(principal);
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 1. forget the user on this thread
        SecurityContextHolder.clearContext();
        // 2. destroy the session -> the stored SecurityContext goes with it
        if (httpRequest.getSession(false) != null) {
            httpRequest.getSession(false).invalidate();
        }
    }

    private static LoginResponse toResponse(AppPrincipal principal) {
        return new LoginResponse(
                principal.getId(),
                principal.getName(),
                principal.getEmail(),
                principal.getCollegeId(),
                principal.getRole().name()
        );
    }
}
