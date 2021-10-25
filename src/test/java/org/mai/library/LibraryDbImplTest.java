package org.mai.library;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mai.library.entities.Book;
import org.mai.library.entities.Student;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class LibraryDbImplTest {
    private static final String connectionString = "jdbc:h2:mem:library";

    private Connection connection;
    private LibraryDbImpl library;

    private final Book book1 = new Book(1, "bored book");
    private final Book book2 = new Book(2, "useful book");
    private final Book book3 = new Book(3, "Теория всего");
    private final Book duplicatedBook1Id = new Book(1, "This is wrong book");
    private final Book book1SameName = new Book(4, "bored book");
    private final Book nonExistingBook = new Book(399, "aga");

    private final Student student1 = new Student(1, "Mike");
    private final Student student2 = new Student(2, "Jordan");
    private final Student student3 = new Student(3, "Emily");
    private final Student duplicatedStudent1Id = new Student(1, "noname");
    private final Student student1SameName = new Student(4, "Mike");
    private final Student nonExistingStudent = new Student(399, "who?");

    @Before
    public void setUp() throws Exception {
        connection = DriverManager.getConnection(connectionString);
        App.createTables(connection);
        library = new LibraryDbImpl(connection);
    }

    @After
    public void tearDown() throws Exception {
        library.close();
        library = null;
        connection = null;
    }

    @Test
    public void addNewBook() {
        assertThat(library.addNewBook(book1), equalTo(true));
        assertThat(library.addNewBook(duplicatedBook1Id), equalTo(false));
        assertThat(library.addNewBook(book1SameName), equalTo(true));
        assertThat(library.addNewBook(book2), equalTo(true));
        assertThat(library.addNewBook(book3), equalTo(true));
    }

    @Test
    public void addNewBookDb() throws Exception {
        library.addNewBook(book1);
        library.addNewBook(duplicatedBook1Id);

        var selectStatement = connection.prepareStatement("""
                select book_id, book_title, student_id from Book;""");

        var resultSet = selectStatement.executeQuery();

        var hasFirst = resultSet.next();
        assertThat(hasFirst, equalTo(true));
        assertThat(resultSet.getInt("book_id"), equalTo(book1.getId()));
        assertThat(resultSet.getString("book_title"), equalTo(book1.getTitle()));
        resultSet.getInt("student_id");
        assertThat(resultSet.wasNull(), equalTo(true));

        var hasSecond = resultSet.next();
        assertThat(hasSecond, equalTo(false));
    }

    @Test
    public void addStudent() {
        assertThat(library.addStudent(student1), equalTo(true));
        assertThat(library.addStudent(duplicatedStudent1Id), equalTo(false));
        assertThat(library.addStudent(student1SameName), equalTo(true));
        assertThat(library.addStudent(student2), equalTo(true));
        assertThat(library.addStudent(student3), equalTo(true));
    }

    @Test
    public void addStudentDb() throws SQLException {
        library.addStudent(student1);
        library.addStudent(duplicatedStudent1Id);

        var selectStatement = connection.prepareStatement("""
                select student_id, student_name from Student;""");

        var resultSet = selectStatement.executeQuery();

        var hasFirst = resultSet.next();
        assertThat(hasFirst, equalTo(true));
        assertThat(resultSet.getInt("student_id"), equalTo(student1.getId()));
        assertThat(resultSet.getString("student_name"), equalTo(student1.getName()));

        var hasSecond = resultSet.next();
        assertThat(hasSecond, equalTo(false));
    }

    @Test
    public void borrowBook() {
        addBooks();
        addStudents();

        assertThat(library.borrowBook(nonExistingBook, student1), equalTo(false));
        assertThat(library.borrowBook(book1, nonExistingStudent), equalTo(false));

        assertThat(library.borrowBook(book1, student1), equalTo(true));
        assertThat(library.borrowBook(book1, student1), equalTo(false));
        assertThat(library.borrowBook(book1, student2), equalTo(false));

        assertThat(library.borrowBook(book2, student1), equalTo(true));
        assertThat(library.borrowBook(book3, student2), equalTo(true));
    }

    @Test
    public void borrowBookDb() throws SQLException {
        library.addNewBook(book1);
        library.addStudent(student1);

        library.borrowBook(nonExistingBook, student1);
        library.borrowBook(book1, nonExistingStudent);

        var selectStatement = connection.prepareStatement("""
                select student_id from Book;""");

        var resultSet = selectStatement.executeQuery();
        resultSet.next();
        resultSet.getInt("student_id");
        assertThat(resultSet.wasNull(), equalTo(true));

        library.borrowBook(book1, student1);
        library.borrowBook(book1, student2);

        resultSet = selectStatement.executeQuery();
        resultSet.next();
        var borrowedStudentId = resultSet.getInt("student_id");
        assertThat(borrowedStudentId, equalTo(student1.getId()));
    }

    @Test
    public void returnBook() {
        addBooks();
        addStudents();

        assertThat(library.returnBook(book1, student1), equalTo(false));

        library.borrowBook(book1, student1);

        assertThat(library.returnBook(book1, student2), equalTo(false));
        assertThat(library.returnBook(book1, nonExistingStudent), equalTo(false));
        assertThat(library.returnBook(book1, student1), equalTo(true));
        assertThat(library.returnBook(book1, student1), equalTo(false));
    }

    @Test
    public void returnBookDb() throws SQLException {
        library.addNewBook(book1);
        library.addStudent(student1);

        library.returnBook(book1, student1);

        var selectStatement = connection.prepareStatement("""
                select student_id from Book;""");

        var resultSet = selectStatement.executeQuery();
        resultSet.next();
        resultSet.getInt("student_id");
        assertThat(resultSet.wasNull(), equalTo(true));

        library.borrowBook(book1, student1);
        library.returnBook(book1, student2);

        resultSet = selectStatement.executeQuery();
        resultSet.next();
        var borrowedStudentId = resultSet.getInt("student_id");
        assertThat(borrowedStudentId, equalTo(student1.getId()));

        library.returnBook(book1, student1);

        resultSet = selectStatement.executeQuery();
        resultSet.next();
        resultSet.getInt("student_id");
        assertThat(resultSet.wasNull(), equalTo(true));
    }

    @Test
    public void findAvailableBooks() {
        addBooks();
        addStudents();

        var availableBooks = library.findAvailableBooks();
        assertThat(availableBooks, allOf(
                hasItems(book1, book2, book3),
                not(hasItems(book1SameName, duplicatedBook1Id))));

        library.borrowBook(book1, student1);
        library.borrowBook(book2, nonExistingStudent);
        library.returnBook(book1, student2);
        library.returnBook(book2, student2);

        availableBooks = library.findAvailableBooks();
        assertThat(availableBooks, allOf(
                hasItems(book2, book3),
                not(hasItem(book1))));

        library.returnBook(book1, student1);

        availableBooks = library.findAvailableBooks();
        assertThat(availableBooks, hasItems(book1, book2, book3));
    }

    @Test
    public void getAllStudents() {
        var students = library.getAllStudents();
        assertThat(students.size(), equalTo(0));

        addStudents();

        students = library.getAllStudents();
        assertThat(students, hasItems(student1, student2, student3));
    }

    private void addBooks() {
        library.addNewBook(book1);
        library.addNewBook(book2);
        library.addNewBook(book3);
    }

    private void addStudents() {
        library.addStudent(student1);
        library.addStudent(student2);
        library.addStudent(student3);
    }
}