package com.collegelibrary.security;

import com.collegelibrary.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Spring Security asks this class "who is this identifier?"
 * We look the person up in MySQL and return an AppPrincipal.
 * Used by the AuthenticationManager during login.
 */
@Component
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        return userRepository.findByEmailOrCollegeId(identifier == null ? "" : identifier.trim())
                .map(AppPrincipal::new)
                // The message is never shown to the client — AuthService turns
                // every failure into the same generic "invalid credentials".
                .orElseThrow(() -> new UsernameNotFoundException("No account matches that identifier"));
    }
}
