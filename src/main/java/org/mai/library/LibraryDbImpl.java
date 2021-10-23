package org.mai.library;

import org.apache.log4j.Logger;
import org.mai.library.entities.Book;
import org.mai.library.entities.Student;
import org.mai.library.exceptions.DbConnectionException;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class LibraryDbImpl implements Library, Closeable {
    private static final Logger logger = Logger.getLogger(LibraryDbImpl.class);

    private final Connection connection;
    private final PreparedStatement addNewBookStatement;
    private final PreparedStatement addStudentStatement;
    private final PreparedStatement borrowBookStatement;
    private final PreparedStatement returnBookStatement;
    private final PreparedStatement findAvailableBooksStatement;
    private final PreparedStatement getAllStudentsStatement;

    public LibraryDbImpl(String connectionString) {
        this(getConnection(connectionString));
    }

    private static Connection getConnection(String connectionString) {
        try {
            return DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            throw new DbConnectionException("Error on create connection.", e);
        }
    }

    public LibraryDbImpl(Connection connection) {
        this.connection = connection;

        try {
            addNewBookStatement = connection.prepareStatement("""
                    insert into Book (book_id, book_title)
                    values(?, ?);""");
            addStudentStatement = connection.prepareStatement("""
                    insert into Student (student_id, student_name)
                    values(?, ?);""");
            borrowBookStatement = connection.prepareStatement("""
                    update Book
                    set student_id = ?
                    where book_id = ? and student_id is null;""");
            returnBookStatement = connection.prepareStatement("""
                    update Book
                    set student_id = null
                    where book_id = ? and student_id = ?""");
            findAvailableBooksStatement = connection.prepareStatement("""
                    select book_id, book_title
                    from Book
                    where student_id is null;""");
            getAllStudentsStatement = connection.prepareStatement("""
                    select student_id, student_name
                    from Student;""");
        } catch (SQLException e) {
            throw new DbConnectionException("Error on prepare statements.", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean addNewBook(Book book) {
        try {
            addNewBookStatement.setInt(1, book.getId());
            addNewBookStatement.setString(2, book.getTitle());
            addNewBookStatement.execute();
            return true;
        } catch (SQLException e) {
            logger.debug("On addNewBook: %s".formatted(e));
            return false;
        }
    }

    @Override
    public boolean addStudent(Student student) {
        try {
            addStudentStatement.setInt(1, student.getId());
            addStudentStatement.setString(2, student.getName());
            addStudentStatement.execute();
            return true;
        } catch (SQLException e) {
            logger.debug("On addStudent: %s".formatted(e));
            return false;
        }
    }

    @Override
    public boolean borrowBook(Book book, Student student) {
        try {
            borrowBookStatement.setInt(1, student.getId());
            borrowBookStatement.setInt(2, book.getId());
            var updatedCount = borrowBookStatement.executeUpdate();
            return updatedCount > 0;
        } catch (SQLException e) {
            logger.debug("On borrowBook: %s".formatted(e));
            return false;
        }
    }

    @Override
    public boolean returnBook(Book book, Student student) {
        try {
            returnBookStatement.setInt(1, book.getId());
            returnBookStatement.setInt(2, student.getId());
            var updatedCount = returnBookStatement.executeUpdate();
            return updatedCount > 0;
        } catch (SQLException e) {
            logger.debug("On returnBook: %s".formatted(e));
            return false;
        }
    }

    @Override
    public List<Book> findAvailableBooks() {
        try {
            var resultSet = findAvailableBooksStatement.executeQuery();

            var result = new ArrayList<Book>();
            while (resultSet.next()) {
                var id = resultSet.getInt("book_id");
                var title = resultSet.getString("book_title");
                result.add(new Book(id, title));
            }
            return result;
        } catch (SQLException e) {
            logger.debug("On findAvailableBook: %s".formatted(e));
            return new ArrayList<>();
        }
    }

    @Override
    public List<Student> getAllStudents() {
        try {
            var resultSet = getAllStudentsStatement.executeQuery();

            var result = new ArrayList<Student>();
            while (resultSet.next()) {
                var id = resultSet.getInt("student_id");
                var name = resultSet.getString("student_name");
                result.add(new Student(id, name));
            }
            return result;
        } catch (SQLException e) {
            logger.debug("On getAllStudents: %s".formatted(e));
            return new ArrayList<>();
        }
    }
}
