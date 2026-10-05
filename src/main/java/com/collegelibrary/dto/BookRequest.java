package com.collegelibrary.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Create or edit a book. totalCopies = how many copies the library owns. */
public record BookRequest(
        @NotBlank(message = "ISBN is required")
        @Size(max = 20, message = "ISBN must be at most 20 characters")
        String isbn,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Author is required")
        @Size(max = 160)
        String author,

        @Size(max = 160)
        String publisher,

        @Size(max = 80)
        String category,

        @Min(value = 1000, message = "Publication year looks incorrect")
        Integer publicationYear,

        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price is too large")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "Rating must be 0 or more")
        @DecimalMax(value = "5.0", message = "Rating cannot be more than 5")
        BigDecimal rating,

        @NotNull(message = "Total copies is required")
        @Min(value = 1, message = "Total copies must be at least 1")
        Integer totalCopies,

        Boolean active
) {}
