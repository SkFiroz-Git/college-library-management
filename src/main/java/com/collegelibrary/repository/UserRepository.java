package com.collegelibrary.repository;

import com.collegelibrary.entity.User;
import com.collegelibrary.enums.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCollegeId(String collegeId);

    boolean existsByEmail(String email);

    boolean existsByCollegeId(String collegeId);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByCollegeIdAndIdNot(String collegeId, Long id);

    long countByRole(Role role);

    long countByRoleAndActiveTrue(Role role);

    long countByActiveTrue();

    boolean existsByRole(Role role);

    /**
     * Login lookup: the user may type either the email or the college ID.
     * lower() on both sides makes the match case-insensitive.
     */
    @Query("select u from User u where lower(u.email) = lower(:identifier) or lower(u.collegeId) = lower(:identifier)")
    Optional<User> findByEmailOrCollegeId(@Param("identifier") String identifier);
}
