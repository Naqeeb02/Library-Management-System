package librarysystem.service;

import librarysystem.model.Book;
import librarysystem.model.BookStatus;
import librarysystem.model.LibraryActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Provides validated in-memory CRUD operations for library books. */
public class LibraryService {
    private static final int MAX_RECENT_ACTIVITIES = 30;
    public static final long DAILY_OVERDUE_FINE_PKR = 50;
    private final List<Book> books = new ArrayList<>();
    private final List<LibraryActivity> activities = new ArrayList<>();
    private final Clock clock;

    public LibraryService() {
        this(Clock.systemDefaultZone());
    }

    public LibraryService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized Book addBook(String id, String title, String author) throws LibraryException {
        String normalizedId = validateBookId(id);
        String normalizedTitle = validateTitle(title);
        String normalizedAuthor = validateAuthor(author);
        if (getBookById(normalizedId) != null) {
            throw new LibraryException("A book with ID '" + normalizedId + "' already exists.");
        }
        if (books.stream().anyMatch(book -> book.getTitle().equalsIgnoreCase(normalizedTitle))) {
            throw new LibraryException("A book with title '" + normalizedTitle + "' already exists.");
        }
        Book book = new Book(normalizedId, normalizedTitle, normalizedAuthor, BookStatus.AVAILABLE);
        books.add(book);
        recordActivity("Added", book);
        return book;
    }

    public synchronized List<Book> getAllBooks() {
        return Collections.unmodifiableList(new ArrayList<>(books));
    }

    /** Returns recent book operations, newest first. */
    public synchronized List<LibraryActivity> getRecentActivities() {
        List<LibraryActivity> recent = new ArrayList<>(activities);
        Collections.reverse(recent);
        return Collections.unmodifiableList(recent);
    }

    public synchronized Book getBookById(String id) {
        if (id == null) return null;
        String normalized = id.trim();
        return books.stream()
                .filter(book -> book.getId().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public synchronized Book updateBook(String id, String title, String author) throws LibraryException {
        String normalizedId = validateBookId(id);
        String normalizedTitle = validateTitle(title);
        String normalizedAuthor = validateAuthor(author);
        Book book = getBookById(normalizedId);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + normalizedId + "'.");
        }
        if (books.stream().anyMatch(existing -> !existing.getId().equals(normalizedId)
                && existing.getTitle().equalsIgnoreCase(normalizedTitle))) {
            throw new LibraryException("A book with title '" + normalizedTitle + "' already exists.");
        }
        book.setTitle(normalizedTitle);
        book.setAuthor(normalizedAuthor);
        recordActivity("Updated", book);
        return book;
    }

    public synchronized boolean deleteBook(String id) throws LibraryException {
        String normalizedId = validateBookId(id);
        Book book = getBookById(normalizedId);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + normalizedId + "'.");
        }
        if (book.getStatus() != BookStatus.AVAILABLE) {
            throw new LibraryException(
                    "Issued or reserved books cannot be deleted. Make the book available before deleting.");
        }
        recordActivity("Deleted", book);
        return books.remove(book);
    }

    public synchronized Book updateBookStatus(String id, BookStatus status) throws LibraryException {
        String normalizedId = validateBookId(id);
        if (status == null) {
            throw new LibraryException("A valid status must be selected.");
        }
        Book book = getBookById(normalizedId);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + normalizedId + "'.");
        }
        if (status == BookStatus.ISSUED) {
            throw new LibraryException("Use the Issue Book action to enter a return-by date.");
        }
        if (book.getStatus() == BookStatus.ISSUED && status != BookStatus.ISSUED) {
            throw new LibraryException("Return the issued book before changing its status.");
        }
        if (book.getStatus() != status) {
            recordActivity("Status changed to " + status.name(), book);
        }
        book.setStatus(status);
        return book;
    }

    /** Issues a book and records the issue date and required return-by date. */
    public synchronized Book issueBook(String id, LocalDate dueDate) throws LibraryException {
        String normalizedId = validateBookId(id);
        if (dueDate == null) {
            throw new LibraryException("A return-by date is required.");
        }
        LocalDate issueDate = LocalDate.now(clock);
        if (dueDate.isBefore(issueDate)) {
            throw new LibraryException("The return-by date cannot be before the issue date.");
        }
        Book book = getBookById(normalizedId);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + normalizedId + "'.");
        }
        if (book.getStatus() == BookStatus.ISSUED) {
            throw new LibraryException("This book is already issued. Return it before issuing again.");
        }
        book.setIssueDate(issueDate);
        book.setDueDate(dueDate);
        book.setReturnedDate(null);
        book.setStatus(BookStatus.ISSUED);
        recordActivity("Issued; due " + dueDate, book);
        return book;
    }

    /** Returns an issued book and calculates its final overdue fine in PKR. */
    public synchronized long returnBook(String id) throws LibraryException {
        String normalizedId = validateBookId(id);
        Book book = getBookById(normalizedId);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + normalizedId + "'.");
        }
        if (book.getStatus() != BookStatus.ISSUED || book.getDueDate() == null) {
            throw new LibraryException("This book is not currently issued.");
        }
        LocalDate returnDate = LocalDate.now(clock);
        long fine = calculateOverdueFine(book, returnDate);
        book.setReturnedDate(returnDate);
        book.setStatus(BookStatus.AVAILABLE);
        recordActivity(fine > 0
                ? "Returned; overdue fine PKR " + fine
                : "Returned on time; no fine", book);
        return fine;
    }

    /** Calculates the currently accrued fine for an issued book in PKR. */
    public synchronized long getOverdueFine(String id) throws LibraryException {
        String normalizedId = validateBookId(id);
        Book book = getBookById(normalizedId);
        if (book == null) {
            throw new LibraryException("No book exists with ID '" + normalizedId + "'.");
        }
        if (book.getStatus() != BookStatus.ISSUED) {
            return 0;
        }
        return calculateOverdueFine(book, LocalDate.now(clock));
    }

    /** Loads meaningful sample books for demonstration and manual testing. */
    public synchronized void seedSampleBooks() {
        if (!books.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        Random random = new Random();

        String[][] entries = {
                {"ACL-1001", "Principles of Leadership", "Ahmed Khan"},
                {"ACL-1002", "Military History of Pakistan", "Imran Raza"},
                {"ACL-1003", "Strategic Studies Reader", "Nadia Saeed"},
                {"ACL-1004", "Introduction to Information Systems", "Bilal Hussain"},
                {"ACL-1005", "Library Science Fundamentals", "Ayesha Malik"},
                {"ACL-1006", "Pakistan Studies", "Farhan Ali"},
                {"ACL-1007", "World Atlas and Geography", "Sana Iqbal"},
                {"ACL-1008", "English Communication Skills", "Hina Noor"},
                {"ACL-1009", "Basic Mathematics 1", "Usman Tariq"},
                {"ACL-1010", "Science and Technology Today", "Kamran Shah"},
                {"ACL-1011", "Clean Code", "Robert C Martin"},
                {"ACL-1012", "The Pragmatic Programmer", "Andrew Hunt and David Thomas"},
                {"ACL-1013", "Code Complete", "Steve McConnell"},
                {"ACL-1014", "Introduction to Algorithms", "Thomas H Cormen Charles E Leiserson Ronald L Rivest and Clifford Stein"},
                {"ACL-1015", "The Art of Computer Programming", "Donald Knuth"},
                {"ACL-1016", "Design Patterns", "Erich Gamma Richard Helm Ralph Johnson and John Vlissides"},
                {"ACL-1017", "Refactoring", "Martin Fowler"},
                {"ACL-1018", "Head First Design Patterns", "Eric Freeman and Elisabeth Robson"},
                {"ACL-1019", "Programming Pearls", "Jon Bentley"},
                {"ACL-1020", "The Mythical Man Month", "Frederick P Brooks Jr"},
                {"ACL-1021", "Effective Java", "Joshua Bloch"},
                {"ACL-1022", "Java The Complete Reference", "Herbert Schildt"},
                {"ACL-1023", "Python Crash Course", "Eric Matthes"},
                {"ACL-1024", "Automate the Boring Stuff with Python", "Al Sweigart"},
                {"ACL-1025", "Eloquent JavaScript", "Marijn Haverbeke"},
                {"ACL-1026", "You Dont Know JS", "Kyle Simpson"},
                {"ACL-1027", "Cracking the Coding Interview", "Gayle Laakmann McDowell"},
                {"ACL-1028", "Algorithms", "Robert Sedgewick and Kevin Wayne"},
                {"ACL-1029", "Grokking Algorithms", "Aditya Bhargava"},
                {"ACL-1030", "The C Programming Language", "Brian W Kernighan and Dennis M Ritchie"},
                {"ACL-1031", "C Plus Plus Primer", "Stanley B Lippman Josee Lajoie and Barbara E Moo"},
                {"ACL-1032", "Programming in C", "Stephen G Kochan"},
                {"ACL-1033", "Structure and Interpretation of Computer Programs", "Harold Abelson and Gerald Jay Sussman"},
                {"ACL-1034", "The Little Schemer", "Daniel P Friedman and Matthias Felleisen"},
                {"ACL-1035", "Dont Make Me Think", "Steve Krug"},
                {"ACL-1036", "Code The Hidden Language of Computer Hardware and Software", "Charles Petzold"},
                {"ACL-1037", "Patterns of Enterprise Application Architecture", "Martin Fowler"},
                {"ACL-1038", "Domain Driven Design", "Eric Evans"},
                {"ACL-1039", "Working Effectively with Legacy Code", "Michael Feathers"},
                {"ACL-1040", "The Clean Coder", "Robert C Martin"},
                {"ACL-2001", "To Kill a Mockingbird", "Harper Lee"},
                {"ACL-2002", "Nineteen Eighty Four", "George Orwell"},
                {"ACL-2003", "Animal Farm", "George Orwell"},
                {"ACL-2004", "The Great Gatsby", "F Scott Fitzgerald"},
                {"ACL-2005", "Pride and Prejudice", "Jane Austen"},
                {"ACL-2006", "Jane Eyre", "Charlotte Bronte"},
                {"ACL-2007", "Wuthering Heights", "Emily Bronte"},
                {"ACL-2008", "The Catcher in the Rye", "J D Salinger"},
                {"ACL-2009", "The Lord of the Rings", "J R R Tolkien"},
                {"ACL-2010", "The Hobbit", "J R R Tolkien"},
                {"ACL-2011", "Harry Potter and the Philosophers Stone", "J K Rowling"},
                {"ACL-2012", "The Alchemist", "Paulo Coelho"},
                {"ACL-2013", "The Little Prince", "Antoine de Saint Exupery"},
                {"ACL-2014", "Don Quixote", "Miguel de Cervantes"},
                {"ACL-2015", "War and Peace", "Leo Tolstoy"},
                {"ACL-2016", "Crime and Punishment", "Fyodor Dostoevsky"},
                {"ACL-2017", "The Brothers Karamazov", "Fyodor Dostoevsky"},
                {"ACL-2018", "Anna Karenina", "Leo Tolstoy"},
                {"ACL-2019", "Les Miserables", "Victor Hugo"},
                {"ACL-2020", "The Count of Monte Cristo", "Alexandre Dumas"},
                {"ACL-2021", "The Picture of Dorian Gray", "Oscar Wilde"},
                {"ACL-2022", "Dracula", "Bram Stoker"},
                {"ACL-2023", "Frankenstein", "Mary Shelley"},
                {"ACL-2024", "Moby Dick", "Herman Melville"},
                {"ACL-2025", "The Old Man and the Sea", "Ernest Hemingway"},
                {"ACL-2026", "One Hundred Years of Solitude", "Gabriel Garcia Marquez"},
                {"ACL-2027", "Love in the Time of Cholera", "Gabriel Garcia Marquez"},
                {"ACL-2028", "The Kite Runner", "Khaled Hosseini"},
                {"ACL-2029", "A Thousand Splendid Suns", "Khaled Hosseini"},
                {"ACL-2030", "The Book Thief", "Markus Zusak"},
                {"ACL-2031", "The Diary of a Young Girl", "Anne Frank"},
                {"ACL-2032", "The Grapes of Wrath", "John Steinbeck"},
                {"ACL-2033", "Of Mice and Men", "John Steinbeck"},
                {"ACL-2034", "Brave New World", "Aldous Huxley"},
                {"ACL-2035", "Fahrenheit 451", "Ray Bradbury"},
                {"ACL-2036", "The Stranger", "Albert Camus"},
                {"ACL-2037", "The Metamorphosis", "Franz Kafka"},
                {"ACL-2038", "The Trial", "Franz Kafka"},
                {"ACL-2039", "Siddhartha", "Hermann Hesse"},
                {"ACL-2040", "The Divine Comedy", "Dante Alighieri"},
                {"ACL-2041", "The Iliad", "Homer"},
                {"ACL-2042", "The Odyssey", "Homer"},
                {"ACL-2043", "The Republic", "Plato"},
                {"ACL-2044", "Meditations", "Marcus Aurelius"},
                {"ACL-2045", "The Art of War", "Sun Tzu"},
                {"ACL-2046", "Mans Search for Meaning", "Viktor E Frankl"},
                {"ACL-2047", "The Power of Now", "Eckhart Tolle"},
                {"ACL-2048", "Atomic Habits", "James Clear"},
                {"ACL-2049", "The 7 Habits of Highly Effective People", "Stephen R Covey"},
                {"ACL-2050", "How to Win Friends and Influence People", "Dale Carnegie"},
                {"ACL-2051", "Think and Grow Rich", "Napoleon Hill"},
                {"ACL-2052", "Rich Dad Poor Dad", "Robert T Kiyosaki"},
                {"ACL-2053", "The Psychology of Money", "Morgan Housel"},
                {"ACL-2054", "Deep Work", "Cal Newport"},
                {"ACL-2055", "The 4 Hour Workweek", "Timothy Ferriss"},
                {"ACL-2056", "Sapiens", "Yuval Noah Harari"},
                {"ACL-2057", "Homo Deus", "Yuval Noah Harari"},
                {"ACL-2058", "A Brief History of Time", "Stephen Hawking"},
                {"ACL-2059", "The Selfish Gene", "Richard Dawkins"},
                {"ACL-2060", "The Origin of Species", "Charles Darwin"},
                {"ACL-2061", "The Republic of Plato", "Plato"},
                {"ACL-2062", "The Prince", "Niccolo Machiavelli"},
                {"ACL-2063", "The Communist Manifesto", "Karl Marx and Friedrich Engels"},
                {"ACL-2064", "The Wealth of Nations", "Adam Smith"},
                {"ACL-2065", "Guns Germs and Steel", "Jared Diamond"},
                {"ACL-2066", "The Immortal Life of Henrietta Lacks", "Rebecca Skloot"},
                {"ACL-2067", "Thinking Fast and Slow", "Daniel Kahneman"},
                {"ACL-2068", "Quiet", "Susan Cain"},
                {"ACL-2069", "The Subtle Art of Not Giving a Damn", "Mark Manson"},
                {"ACL-2070", "The Four Agreements", "Don Miguel Ruiz"},
                {"ACL-2071", "The 48 Laws of Power", "Robert Greene"},
                {"ACL-2072", "The Laws of Human Nature", "Robert Greene"},
                {"ACL-2073", "The 5 AM Club", "Robin Sharma"},
                {"ACL-2074", "The Monk Who Sold His Ferrari", "Robin Sharma"},
                {"ACL-2075", "The Intelligent Investor", "Benjamin Graham"},
                {"ACL-2076", "The Lean Startup", "Eric Ries"},
                {"ACL-2077", "Zero to One", "Peter Thiel"},
                {"ACL-2078", "Start with Why", "Simon Sinek"},
                {"ACL-2079", "The Hard Thing About Hard Things", "Ben Horowitz"},
                {"ACL-2080", "Steve Jobs", "Walter Isaacson"},
                {"ACL-2081", "Elon Musk", "Walter Isaacson"},
                {"ACL-2082", "The Diary of a CEO", "Steven Bartlett"},
                {"ACL-2083", "The Design of Everyday Things", "Don Norman"},
                {"ACL-2084", "Computer Networking A Top Down Approach", "James F Kurose and Keith W Ross"},
                {"ACL-2085", "Operating System Concepts", "Abraham Silberschatz Peter B Galvin and Greg Gagne"},
                {"ACL-2086", "Artificial Intelligence A Modern Approach", "Stuart Russell and Peter Norvig"},
                {"ACL-2087", "The Self Taught Programmer", "Cory Althoff"},
                {"ACL-2088", "The Innovators", "Walter Isaacson"},
                {"ACL-2089", "The Code Book", "Simon Singh"}
        };

        for (String[] entry : entries) {
            BookStatus status = BookStatus.values()[random.nextInt(BookStatus.values().length)];
            LocalDate issueDate = null;
            LocalDate dueDate = null;
            if (status == BookStatus.ISSUED) {
                issueDate = today.minusDays(1 + random.nextInt(14));
                dueDate = issueDate.plusDays(3 + random.nextInt(14)); // may be before/today/after 'today'
            }
            addSeedBook(entry[0], entry[1], entry[2], status, issueDate, dueDate);
        }
    }

    private void addSeedBook(String id, String title, String author, BookStatus status,
                             LocalDate issueDate, LocalDate dueDate) {
        Book book = new Book(id, title, author, status);
        if (status == BookStatus.ISSUED) {
            book.setIssueDate(issueDate);
            book.setDueDate(dueDate);
        }
        books.add(book);
    }

    private long calculateOverdueFine(Book book, LocalDate date) {
        if (book.getDueDate() == null || !date.isAfter(book.getDueDate())) {
            return 0;
        }
        long overdueDays = ChronoUnit.DAYS.between(book.getDueDate(), date);
        return overdueDays * DAILY_OVERDUE_FINE_PKR;
    }

    private void recordActivity(String action, Book book) {
        activities.add(new LibraryActivity(action, book.getId(), book.getTitle(),
                LocalDateTime.now(clock)));
        if (activities.size() > MAX_RECENT_ACTIVITIES) {
            activities.remove(0);
        }
    }

    private String validateBookId(String value) throws LibraryException {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryException("Book ID cannot be empty.");
        }
        String normalized = value.trim();
        if (!normalized.startsWith("ACL-") || normalized.length() <= 4) {
            throw new LibraryException("Book ID must start with 'ACL-' followed by at least one character.");
        }
        return normalized;
    }

    private String validateTitle(String value) throws LibraryException {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryException("Title cannot be empty.");
        }
        String normalized = value.trim();
        if (!normalized.matches("[\\p{L}][\\p{L}\\p{N} ]*")) {
            throw new LibraryException(
                    "Title must start with a letter and may contain only letters, numbers, and spaces.");
        }
        return normalized;
    }

    private String validateAuthor(String value) throws LibraryException {
        if (value == null || value.trim().isEmpty()) {
            throw new LibraryException("Author cannot be empty.");
        }
        String normalized = value.trim();
        if (!normalized.matches("[\\p{L} ]+")) {
            throw new LibraryException("Author name must contain letters only and cannot include numbers or special characters.");
        }
        return normalized;
    }
}
