package org.mai.library;


import org.apache.log4j.Logger;
import org.mai.library.entities.Book;
import org.mai.library.entities.Student;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class App {
    private static final Logger logger = Logger.getLogger(App.class);

    public static void main(String[] args) {
        var connectionString = "jdbc:h2:mem:library";
        try {
            var connection = DriverManager.getConnection(connectionString);
            createTables(connection);

            var library = new LibraryDbImpl(connectionString);

            library.addNewBook(new Book(1, "Halo"));
            library.addNewBook(new Book(2, "dadadad"));
            library.addNewBook(new Book(3, "chto"));
            library.addNewBook(new Book(4, "kavo"));
            library.addNewBook(new Book(5, "how to be number one"));
            library.addNewBook(new Book(6, "very usefull book"));
            library.addNewBook(new Book(7, "bored book"));

            library.addStudent(new Student(1, "Mike"));
            library.addStudent(new Student(2, "Jordan"));
            library.addStudent(new Student(3, "Emily"));

            var available = library.findAvailableBooks();
            System.out.println(available);
            var students = library.getAllStudents();
            System.out.println(students);

        } catch (Exception e) {
            logger.debug("main: %s".formatted(e));
        }
    }

    public static void createTables(Connection connection) throws SQLException {
        var statement = connection.createStatement();

        var createStudentTableRequest = readFromResource("database/tables/Student.sql");
        statement.execute(createStudentTableRequest);

        var createBookTableRequest = readFromResource("database/tables/Book.sql");
        statement.execute(createBookTableRequest);
    }

    private static String readFromResource(String resourcePath) {
        var resourceStream = App.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            throw new RuntimeException("Fail to load resource with path \"%s\" (Resource stream is null).".formatted(resourcePath));
        }

        try(var reader = new BufferedReader(new InputStreamReader(resourceStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
        catch (IOException e) {
            throw new RuntimeException("Fail to load resource with path \"%s\" (IOError).".formatted(resourcePath));
        }
    }
}
