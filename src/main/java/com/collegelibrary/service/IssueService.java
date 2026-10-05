package com.collegelibrary.service;

import com.collegelibrary.dto.IssueCreateRequest;
import com.collegelibrary.dto.IssueResponse;
import com.collegelibrary.dto.PageResponse;
import com.collegelibrary.entity.Book;
import com.collegelibrary.entity.BookIssue;
import com.collegelibrary.entity.User;
import com.collegelibrary.enums.IssueStatus;
import com.collegelibrary.exception.BadRequestException;
import com.collegelibrary.exception.ResourceNotFoundException;
import com.collegelibrary.repository.BookRepository;
import com.collegelibrary.repository.IssueRepository;
import com.collegelibrary.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lending and returning books — the core business logic:
 *   issue  = check stock -> create record -> decrease available copies
 *   return = mark record -> increase available copies
 */
@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final int loanDays;

    public IssueService(IssueRepository issueRepository,
                        BookRepository bookRepository,
                        UserRepository userRepository,
                        @Value("${app.loan.days:14}") int loanDays) {
        this.issueRepository = issueRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.loanDays = loanDays;
    }

    @Transactional
    public IssueResponse issue(IssueCreateRequest request) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book", request.bookId()));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        if (!book.isActive()) {
            throw new BadRequestException("This book is archived and cannot be issued");
        }
        if (!user.isActive()) {
            throw new BadRequestException("This member account is deactivated");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BadRequestException("No copies of this book are available right now");
        }

        BookIssue issue = new BookIssue();
        issue.setBook(book);
        issue.setUser(user);
        issue.setIssuedAt(LocalDateTime.now());
        issue.setDueDate(LocalDate.now().plusDays(loanDays));
        issue.setStatus(IssueStatus.ISSUED);

        book.setAvailableCopies(book.getAvailableCopies() - 1);

        // Everything below still runs inside the transaction, so lazy
        // loading of book/user works when IssueResponse reads their names.
        return IssueResponse.from(issueRepository.save(issue));
    }

    @Transactional
    public IssueResponse returnBook(Long id) {
        BookIssue issue = issueRepository.findByIdAndStatus(id, IssueStatus.ISSUED)
                .orElseThrow(() -> new BadRequestException("This record is not an active loan"));

        issue.setReturnedAt(LocalDateTime.now());
        issue.setStatus(IssueStatus.RETURNED);

        Book book = issue.getBook();
        int restored = Math.min(book.getTotalCopies(), book.getAvailableCopies() + 1);
        book.setAvailableCopies(restored);

        return IssueResponse.from(issueRepository.save(issue));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> list(String search, IssueStatus status, int page, int size) {
        Page<BookIssue> result = issueRepository.search(
                blankToNull(search),
                status,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.from(result.map(IssueResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> overdue(int page, int size) {
        Page<BookIssue> result = issueRepository.findOverdue(
                IssueStatus.ISSUED,
                LocalDate.now(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return PageResponse.from(result.map(IssueResponse::from));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
