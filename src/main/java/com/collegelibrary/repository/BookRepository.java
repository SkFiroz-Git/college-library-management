package com.collegelibrary.repository;

import com.collegelibrary.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    /** Sums for the dashboard. JPA maps sum(Integer) to Long. */
    @Query("select coalesce(sum(b.totalCopies), 0) from Book b where b.active = true")
    Long sumTotalCopies();

    @Query("select coalesce(sum(b.availableCopies), 0) from Book b where b.active = true")
    Long sumAvailableCopies();

    long countByActiveTrue();
}
