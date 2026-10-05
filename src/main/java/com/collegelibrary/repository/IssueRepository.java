package com.collegelibrary.repository;

import com.collegelibrary.entity.BookIssue;
import com.collegelibrary.enums.IssueStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<BookIssue, Long> {

    /**
     * join fetch loads user and book in the SAME SQL query.
     * Required because spring.jpa.open-in-view=false: accessing
     * issue.getUser().getName() outside a session would otherwise fail.
     */
    @Query(
            value = """
                    select i from BookIssue i
                    join fetch i.user u
                    join fetch i.book b
                    where (:status is null or i.status = :status)
                      and (:q is null or :q = ''
                           or lower(u.name) like lower(concat('%', :q, '%'))
                           or lower(b.title) like lower(concat('%', :q, '%')))
                    order by i.issuedAt desc
                    """,
            countQuery = """
                    select count(i) from BookIssue i
                    where (:status is null or i.status = :status)
                      and (:q is null or :q = ''
                           or lower(i.user.name) like lower(concat('%', :q, '%'))
                           or lower(i.book.title) like lower(concat('%', :q, '%')))
                    """)
    Page<BookIssue> search(@Param("q") String q, @Param("status") IssueStatus status, Pageable pageable);

    Optional<BookIssue> findByIdAndStatus(Long id, IssueStatus status);

    long countByStatus(IssueStatus status);

    /** Overdue = still issued AND the due date is in the past. */
    @Query("select count(i) from BookIssue i where i.status = :status and i.dueDate < :today")
    long countOverdue(@Param("status") IssueStatus status, @Param("today") LocalDate today);

    @Query(
            value = """
                    select i from BookIssue i
                    join fetch i.user u
                    join fetch i.book b
                    where i.status = :status and i.dueDate < :today
                    order by i.dueDate asc
                    """,
            countQuery = """
                    select count(i) from BookIssue i
                    where i.status = :status and i.dueDate < :today
                    """)
    Page<BookIssue> findOverdue(@Param("status") IssueStatus status,
                                @Param("today") LocalDate today,
                                Pageable pageable);

    /** Latest activity for the dashboard (user + book loaded eagerly). */
    @EntityGraph(attributePaths = {"user", "book"})
    List<BookIssue> findTop5ByOrderByIssuedAtDesc();

    long countByUserIdAndStatus(Long userId, IssueStatus status);

    long countByUserId(Long userId);

    long countByBookId(Long bookId);

    long countByBookIdAndStatus(Long bookId, IssueStatus status);
}
