package com.collegelibrary.service;

import com.collegelibrary.dto.BookRequest;
import com.collegelibrary.dto.BookResponse;
import com.collegelibrary.dto.PageResponse;
import com.collegelibrary.entity.Book;
import com.collegelibrary.enums.IssueStatus;
import com.collegelibrary.exception.ConflictException;
import com.collegelibrary.exception.ResourceNotFoundException;
import com.collegelibrary.repository.BookRepository;
import com.collegelibrary.repository.IssueRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catalogue management: add / edit / search / archive / delete books. */
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final IssueRepository issueRepository;

    public BookService(BookRepository bookRepository, IssueRepository issueRepository) {
        this.bookRepository = bookRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> list(String search, String category, Boolean active, int page, int size) {
        Specification<Book> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like),
                        cb.like(cb.lower(root.get("isbn")), like)));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Book> result = bookRepository.findAll(
                specification,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.from(result.map(BookResponse::from));
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return BookResponse.from(findOrThrow(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        String isbn = request.isbn().trim();

        if (bookRepository.existsByIsbn(isbn)) {
            throw new ConflictException("A book with this ISBN already exists");
        }

        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setPublisher(trimToNull(request.publisher()));
        book.setCategory(trimToNull(request.category()));
        book.setPublicationYear(request.publicationYear());
        book.setPrice(request.price());
        book.setRating(request.rating());
        book.setTotalCopies(request.totalCopies());
        book.setAvailableCopies(request.totalCopies());   // all copies start on the shelf
        book.setActive(request.active() == null || request.active());

        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = findOrThrow(id);
        String isbn = request.isbn().trim();

        if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new ConflictException("A book with this ISBN already exists");
        }

        book.setIsbn(isbn);
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setPublisher(trimToNull(request.publisher()));
        book.setCategory(trimToNull(request.category()));
        book.setPublicationYear(request.publicationYear());
        book.setPrice(request.price());
        book.setRating(request.rating());
        book.setTotalCopies(request.totalCopies());
        book.setActive(request.active() == null || request.active());

        // available = total - how many are currently out. Never below 0.
        long issuedNow = issueRepository.countByBookIdAndStatus(id, IssueStatus.ISSUED);
        book.setAvailableCopies((int) Math.max(0, request.totalCopies() - issuedNow));

        return BookResponse.from(bookRepository.save(book));
    }

    /** Archive = hide from the catalogue, keep the history (safe). */
    @Transactional
    public BookResponse updateActive(Long id, boolean active) {
        Book book = findOrThrow(id);
        book.setActive(active);
        return BookResponse.from(bookRepository.save(book));
    }

    /**
     * Hard delete only when the book has never been borrowed — otherwise
     * book_issues.book_id would point to a row that no longer exists.
     */
    @Transactional
    public void delete(Long id) {
        findOrThrow(id);

        if (issueRepository.countByBookId(id) > 0) {
            throw new ConflictException("This book has borrowing history. Archive it instead of deleting.");
        }
        bookRepository.deleteById(id);
    }

    private Book findOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id));
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
