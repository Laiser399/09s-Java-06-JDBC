package org.mai.library;

import org.mai.library.entities.Book;
import org.mai.library.entities.Student;

import java.util.List;

public interface Library {
    /* Регистрация новой книги */
    boolean addNewBook(Book book);

    /* Добавление нового абонента */
    boolean addStudent(Student student);

    /* Студент берет книгу */
    boolean borrowBook(Book book, Student student);

    /* Студент возвращает книгу */
    boolean returnBook(Book book, Student student);

    /* Получить список свободных книг */
    List<Book> findAvailableBooks();

    /* Список всех записанных в библиотеку студентов*/
    List<Student> getAllStudents();
}
