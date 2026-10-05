package com.collegelibrary.service;

import com.collegelibrary.dto.PageResponse;
import com.collegelibrary.dto.UserCreateRequest;
import com.collegelibrary.dto.UserResponse;
import com.collegelibrary.dto.UserUpdateRequest;
import com.collegelibrary.entity.User;
import com.collegelibrary.enums.Role;
import com.collegelibrary.exception.ConflictException;
import com.collegelibrary.exception.ResourceNotFoundException;
import com.collegelibrary.repository.IssueRepository;
import com.collegelibrary.repository.UserRepository;
import com.collegelibrary.security.AppPrincipal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin operations on user accounts (create / edit / activate / delete).
 * All rules about "is this data allowed?" live here — never in the
 * controller and never in the frontend.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       IssueRepository issueRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, Role role, Boolean active, int page, int size) {
        Specification<User> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("collegeId")), like)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<User> result = userRepository.findAll(
                specification,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.from(result.map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByCollegeId(request.collegeId().trim())) {
            throw new ConflictException("An account with this college ID already exists");
        }

        User user = new User();
        user.setCollegeId(request.collegeId().trim());
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setCourse(trimToNull(request.course()));
        user.setYear(request.year());
        user.setActive(true);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findOrThrow(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByCollegeIdAndIdNot(request.collegeId().trim(), id)) {
            throw new ConflictException("An account with this college ID already exists");
        }

        // Safety rule: an admin must not lock themselves out.
        if (isCurrentUser(id) && !Boolean.TRUE.equals(request.active())) {
            throw new ConflictException("You cannot deactivate your own account");
        }
        if (isCurrentUser(id) && request.role() != Role.ADMIN) {
            throw new ConflictException("You cannot change your own role");
        }

        user.setCollegeId(request.collegeId().trim());
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setCourse(trimToNull(request.course()));
        user.setYear(request.year());
        user.setActive(Boolean.TRUE.equals(request.active()));

        // Password is only changed when the admin typed a new one.
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    /** Activate / deactivate without touching anything else. */
    @Transactional
    public UserResponse updateActive(Long id, boolean active) {
        User user = findOrThrow(id);

        if (!active && isCurrentUser(id)) {
            throw new ConflictException("You cannot deactivate your own account");
        }
        if (!active && user.getRole() == Role.ADMIN && user.isActive() && isLastActiveAdmin()) {
            throw new ConflictException("At least one active admin must remain");
        }

        user.setActive(active);
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Hard delete is only allowed when the person has no borrowing history.
     * Otherwise the record would break the link table (book_issues.user_id),
     * so we ask the admin to deactivate instead.
     */
    @Transactional
    public void delete(Long id) {
        User user = findOrThrow(id);

        if (issueRepository.countByUserId(id) > 0) {
            throw new ConflictException("This account has borrowing history. Deactivate it instead of deleting.");
        }
        if (isCurrentUser(id)) {
            throw new ConflictException("You cannot delete your own account");
        }
        if (user.getRole() == Role.ADMIN && isLastActiveAdmin()) {
            throw new ConflictException("At least one active admin must remain");
        }

        userRepository.delete(user);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private boolean isLastActiveAdmin() {
        return userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1;
    }

    private boolean isCurrentUser(Long id) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppPrincipal principal)) {
            return false;
        }
        return id.equals(principal.getId());
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
