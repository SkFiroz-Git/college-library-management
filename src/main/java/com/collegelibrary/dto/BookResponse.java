package com.collegelibrary.dto;

import com.collegelibrary.entity.Book;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        String category,
        Integer publicationYear,
        BigDecimal price,
        BigDecimal rating,
        int totalCopies,
        int availableCopies,
        boolean active,
        LocalDateTime createdAt
) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getCategory(),
                book.getPublicationYear(),
                book.getPrice(),
                book.getRating(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.isActive(),
                book.getCreatedAt()
        );
    }
}
