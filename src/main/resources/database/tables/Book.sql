create table Book
(
    book_id    int,
    book_title varchar(255) not null,
    student_id int null,

    constraint PK_Book primary key (book_id),
    constraint FK_Student foreign key (student_id) references Student (student_id)
);