package com.collegelibrary.service;

import com.collegelibrary.dto.DashboardStats;
import com.collegelibrary.dto.IssueResponse;
import com.collegelibrary.entity.BookIssue;
import com.collegelibrary.enums.IssueStatus;
import com.collegelibrary.enums.Role;
import com.collegelibrary.repository.BookRepository;
import com.collegelibrary.repository.IssueRepository;
import com.collegelibrary.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Aggregates the numbers shown on the admin dashboard. */
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final IssueRepository issueRepository;

    public DashboardService(UserRepository userRepository,
                            BookRepository bookRepository,
                            IssueRepository issueRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        List<IssueResponse> recent = issueRepository.findTop5ByOrderByIssuedAtDesc().stream()
                .map(IssueResponse::from)
                .toList();

        return new DashboardStats(
                userRepository.count(),
                userRepository.countByRole(Role.ADMIN),
                userRepository.countByRole(Role.LIBRARIAN),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByActiveTrue(),

                bookRepository.count(),
                bookRepository.countByActiveTrue(),
                bookRepository.sumTotalCopies() == null ? 0 : bookRepository.sumTotalCopies(),
                bookRepository.sumAvailableCopies() == null ? 0 : bookRepository.sumAvailableCopies(),

                issueRepository.countByStatus(IssueStatus.ISSUED),
                issueRepository.countOverdue(IssueStatus.ISSUED, LocalDate.now()),
                issueRepository.countByStatus(IssueStatus.RETURNED),

                recent
        );
    }
}
