package com.collegelibrary.config;

import com.collegelibrary.entity.Book;
import com.collegelibrary.entity.User;
import com.collegelibrary.enums.Role;
import com.collegelibrary.repository.BookRepository;
import com.collegelibrary.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once at every startup and fills gaps in the database:
 *
 *  1. Creates the very first ADMIN account, but ONLY when no admin exists
 *     yet (the "bootstrapping" problem: somebody has to create it, and it
 *     must not be open to the public).
 *  2. Optionally loads a small demo dataset (books + students) so the app
 *     is not empty on the first run. Controlled by app.seed.demo-data and
 *     completely idempotent: a row that already exists is never duplicated.
 *
 * In production: set SEED_ADMIN_* environment variables, switch
 * app.seed.demo-data to false, then remove this class once real data exists.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Demo students all share this password (dev only, printed at startup). */
    private static final String DEMO_STUDENT_PASSWORD = "Student@123";

    private record DemoBook(String isbn, String title, String author, String category,
                            int year, String price, String rating, int copies) {}
    private record DemoStudent(String collegeId, String name, String email,
                               int year, String major) {}

    /** Your sample catalogue: ISBN, title, author, genre, year, price, rating. */
    private static final List<DemoBook> DEMO_BOOKS = List.of(
            new DemoBook("B501", "To Kill a Mockingbird", "Harper Lee", "Classic Fiction", 1960, "12.99", "4.8", 4),
            new DemoBook("B502", "A Brief History of Time", "Stephen Hawking", "Science", 1988, "15.50", "4.7", 3),
            new DemoBook("B503", "The Alchemist", "Paulo Coelho", "Adventure", 1988, "10.00", "4.6", 5),
            new DemoBook("B504", "Clean Code", "Robert C. Martin", "Technology", 2008, "29.99", "4.4", 2),
            new DemoBook("B505", "The Great Gatsby", "F. Scott Fitzgerald", "Classic Fiction", 1925, "9.50", "4.5", 4)
    );

    /** Your sample students: ID, full name, email, academic year, major. */
    private static final List<DemoStudent> DEMO_STUDENTS = List.of(
            new DemoStudent("S1006", "Ishaan Nair", "ishaan.nair@example.com", 1, "Data Science"),
            new DemoStudent("S1007", "Meera Joshi", "meera.joshi@example.com", 3, "Mechanical Engineering"),
            new DemoStudent("S1008", "Aditya Rao", "aditya.rao@example.com", 4, "Psychology"),
            new DemoStudent("S1009", "Zara Khan", "zara.khan@example.com", 2, "Chemistry"),
            new DemoStudent("S1010", "Siddharth Sen", "siddharth.sen@example.com", 3, "Business Administration")
    );

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;
    private final String collegeId;
    private final String email;
    private final String name;
    private final String password;
    private final boolean demoData;

    public DataSeeder(UserRepository userRepository,
                      BookRepository bookRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.seed.admin-college-id}") String collegeId,
                      @Value("${app.seed.admin-email}") String email,
                      @Value("${app.seed.admin-name}") String name,
                      @Value("${app.seed.admin-password}") String password,
                      @Value("${app.seed.demo-data}") boolean demoData) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
        this.collegeId = collegeId;
        this.email = email;
        this.name = name;
        this.password = password;
        this.demoData = demoData;
    }

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfMissing();

        if (demoData) {
            seedDemoBooks();
            seedDemoStudents();
        }
    }

    private void createAdminIfMissing() {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;   // an admin already exists — do nothing
        }

        User admin = new User();
        admin.setCollegeId(collegeId);
        admin.setName(name);
        admin.setEmail(email.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);

        log.info("====================================================");
        log.info(" First admin account created: {}", email);
        log.info(" Password comes from app.seed.admin-password");
        log.info(" CHANGE IT after your first login!");
        log.info("====================================================");
    }

    /** Adds the sample catalogue only for books that are not there yet. */
    private void seedDemoBooks() {
        int added = 0;
        for (DemoBook demo : DEMO_BOOKS) {
            if (bookRepository.existsByIsbn(demo.isbn())) {
                continue;
            }
            Book book = new Book();
            book.setIsbn(demo.isbn());
            book.setTitle(demo.title());
            book.setAuthor(demo.author());
            book.setCategory(demo.category());
            book.setPublicationYear(demo.year());
            book.setPrice(new BigDecimal(demo.price()));
            book.setRating(new BigDecimal(demo.rating()));
            book.setTotalCopies(demo.copies());
            book.setAvailableCopies(demo.copies());   // nobody has borrowed them yet
            book.setActive(true);
            bookRepository.save(book);
            added++;
        }
        if (added > 0) {
            log.info("Seeded {} demo book(s) into the catalogue", added);
        }
    }

    /**
     * Adds the sample students as STUDENT accounts.
     * College ID (S1006…) is the login identifier; grade level becomes
     * academic_year and major becomes course. Age is not part of a library
     * record, so it is not stored.
     */
    private void seedDemoStudents() {
        int added = 0;
        for (DemoStudent demo : DEMO_STUDENTS) {
            if (userRepository.existsByCollegeId(demo.collegeId())
                    || userRepository.existsByEmail(demo.email())) {
                continue;
            }
            User student = new User();
            student.setCollegeId(demo.collegeId());
            student.setName(demo.name());
            student.setEmail(demo.email().toLowerCase());
            student.setPasswordHash(passwordEncoder.encode(DEMO_STUDENT_PASSWORD));
            student.setRole(Role.STUDENT);
            student.setCourse(demo.major());
            student.setYear(demo.year());
            student.setActive(true);
            userRepository.save(student);
            added++;
        }
        if (added > 0) {
            log.info("Seeded {} demo student(s) — login with their College ID / {}",
                    added, DEMO_STUDENT_PASSWORD);
        }
    }
}
