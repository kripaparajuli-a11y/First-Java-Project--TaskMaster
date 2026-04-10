CREATE DATABASE taskmaster;

USE taskmaster;

CREATE TABLE student (
    student_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE course (
    course_id INT PRIMARY KEY AUTO_INCREMENT,
    course_name VARCHAR(100) NOT NULL,
    credit_hours INT
);

CREATE TABLE task (
    task_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT,
    course_id INT,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    deadline DATE,
    priority VARCHAR(20),
    status VARCHAR(20),
    task_type VARCHAR(30),
    FOREIGN KEY (student_id) REFERENCES student(student_id),
    FOREIGN KEY (course_id) REFERENCES course(course_id)
);

INSERT INTO student (name, email, password) VALUES ('Kripa Parajuli', 'student@taskmaster.com', '1234');

SELECT * FROM student;

show tables;

DESCRIBE study_session;

ALTER TABLE study_session 
  ADD COLUMN session_type VARCHAR(50),
  ADD COLUMN session_date DATE,
  ADD COLUMN notes VARCHAR(255),
  CHANGE duration_minutes duration_min INT;
  