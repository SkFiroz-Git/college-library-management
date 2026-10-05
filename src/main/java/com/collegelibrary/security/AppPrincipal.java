package com.collegelibrary.security;

import com.collegelibrary.entity.User;
import com.collegelibrary.enums.Role;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The logged-in user as Spring Security sees it.
 * We keep the entity clean and build this object in the service layer.
 * It carries id / name / role so controllers never need to guess who
 * is logged in.
 */
public class AppPrincipal implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String collegeId;
    private final Role role;
    private final String passwordHash;
    private final boolean active;

    public AppPrincipal(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.collegeId = user.getCollegeId();
        this.role = user.getRole();
        this.passwordHash = user.getPasswordHash();
        this.active = user.isActive();
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        // Spring Security expects the "ROLE_" prefix for hasRole(...) checks
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getCollegeId() { return collegeId; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
}
