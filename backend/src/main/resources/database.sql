-- =====================================================================
-- Lab Supervise — schema with explicit FOREIGN KEYs
-- Purpose: let MySQL Workbench reverse-engineer (Database > Reverse Engineer)
--          and draw the relationship lines between tables.
-- Engine MUST be InnoDB (MyISAM ignores FK constraints -> no diagram lines).
-- No seed data — DDL only.
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS screenshot_capture;
DROP TABLE IF EXISTS student_class_info;
DROP TABLE IF EXISTS student_exam_room_info;
DROP TABLE IF EXISTS student_exam_room;
DROP TABLE IF EXISTS allowed_application;
DROP TABLE IF EXISTS student_class;
DROP TABLE IF EXISTS ban_application;
DROP TABLE IF EXISTS incident_reports;
DROP TABLE IF EXISTS personal_computer;
DROP TABLE IF EXISTS exam_room;
DROP TABLE IF EXISTS classes;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS manage_class;
DROP TABLE IF EXISTS majors;
DROP TABLE IF EXISTS sections;
DROP TABLE IF EXISTS schedules;
DROP TABLE IF EXISTS semesters;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- Base columns (inherited from BaseEntity) repeated on every table:
--   id           INT AUTO_INCREMENT PRIMARY KEY
--   status       INT
--   created_at   DATETIME
--   created_user VARCHAR(255)
--   updated_at   DATETIME
--   updated_user VARCHAR(255)
-- FK columns kept as INT to match parent id type (signedness must match).
-- ---------------------------------------------------------------------

-- ============================ roles ==================================
CREATE TABLE roles (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    name         VARCHAR(255),
    type         INT,
    color        VARCHAR(255)
) ENGINE=InnoDB;

-- ============================ users ==================================
CREATE TABLE users (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    email        VARCHAR(255),
    password     VARCHAR(255),
    raw_password VARCHAR(255),
    full_name    VARCHAR(255),
    phone        VARCHAR(255),
    hometown     VARCHAR(255),
    birthday     DATE,
    role_id      INT
) ENGINE=InnoDB;

-- ========================= departments ===============================
CREATE TABLE departments (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    name         VARCHAR(255)
) ENGINE=InnoDB;

-- ============================ rooms ==================================
CREATE TABLE rooms (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    name         VARCHAR(255),
    capacity     INT NOT NULL
) ENGINE=InnoDB;

-- ========================== semesters ================================
CREATE TABLE semesters (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    name         VARCHAR(255),
    study_year   VARCHAR(255),
    start_date   DATE,
    end_date     DATE
) ENGINE=InnoDB;

-- ========================== schedules ================================
CREATE TABLE schedules (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    name         VARCHAR(255),
    days_of_week VARCHAR(255),
    periods      VARCHAR(255),
    start_time   TIME,
    end_time     TIME
) ENGINE=InnoDB;

-- =========================== sections ================================
CREATE TABLE sections (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    status        INT,
    created_at    DATETIME,
    created_user  VARCHAR(255),
    updated_at    DATETIME,
    updated_user  VARCHAR(255),
    name          VARCHAR(255),
    department_id INT
) ENGINE=InnoDB;

-- ============================ majors =================================
CREATE TABLE majors (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    status        INT,
    created_at    DATETIME,
    created_user  VARCHAR(255),
    updated_at    DATETIME,
    updated_user  VARCHAR(255),
    name          VARCHAR(255),
    department_id INT
) ENGINE=InnoDB;

-- =========================== subjects ================================
CREATE TABLE subjects (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    status        INT,
    created_at    DATETIME,
    created_user  VARCHAR(255),
    updated_at    DATETIME,
    updated_user  VARCHAR(255),
    name          VARCHAR(255),
    code          VARCHAR(255),
    credit_number INT,
    section_id    INT
) ENGINE=InnoDB;

-- =========================== teachers ================================
CREATE TABLE teachers (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    code         VARCHAR(255),
    section_id   INT,
    user_id      INT
) ENGINE=InnoDB;

-- ========================= manage_class ==============================
CREATE TABLE manage_class (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    name         VARCHAR(255),
    max_student  INT,
    teacher_id   INT,
    major_id     INT
) ENGINE=InnoDB;

-- =========================== students ================================
CREATE TABLE students (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    status          INT,
    created_at      DATETIME,
    created_user    VARCHAR(255),
    updated_at      DATETIME,
    updated_user    VARCHAR(255),
    code            VARCHAR(255),
    manage_class_id INT,
    user_id         INT
) ENGINE=InnoDB;

-- ============================ classes ================================
CREATE TABLE classes (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    status           INT,
    created_at       DATETIME,
    created_user     VARCHAR(255),
    updated_at       DATETIME,
    updated_user     VARCHAR(255),
    name             VARCHAR(255),
    max_student      INT,
    session_number   INT,
    subject_id       INT,
    teacher_id       INT,
    schedule_id      INT,
    semester_id      INT,
    start_date       DATE,
    end_date         DATE,
    room_id          INT,
    wifi_ssid        VARCHAR(255),
    tracking_enabled BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

-- ========================== exam_room ================================
CREATE TABLE exam_room (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    status           INT,
    created_at       DATETIME,
    created_user     VARCHAR(255),
    updated_at       DATETIME,
    updated_user     VARCHAR(255),
    code             VARCHAR(255) UNIQUE,
    room_id          INT,
    teacher1_id      INT,
    teacher2_id      INT,
    subject_id       INT,
    semester_id      INT,
    max_student      INT,
    exam_date        DATE,
    periods          VARCHAR(255),
    start_time       TIME,
    end_time         TIME,
    tracking_enabled BOOLEAN DEFAULT FALSE,
    wifi_ssid        VARCHAR(255)
) ENGINE=InnoDB;

-- ======================= personal_computer ===========================
CREATE TABLE personal_computer (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    ip_address   VARCHAR(255),
    user_id      INT
) ENGINE=InnoDB;

-- ========================= student_class =============================
CREATE TABLE student_class (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    class_id     INT,
    student_id   INT
) ENGINE=InnoDB;

-- ======================= student_exam_room ===========================
CREATE TABLE student_exam_room (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    status       INT,
    created_at   DATETIME,
    created_user VARCHAR(255),
    updated_at   DATETIME,
    updated_user VARCHAR(255),
    exam_room_id INT,
    student_id   INT
) ENGINE=InnoDB;

-- ======================= allowed_application =========================
CREATE TABLE allowed_application (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    status           INT,
    created_at       DATETIME,
    created_user     VARCHAR(255),
    updated_at       DATETIME,
    updated_user     VARCHAR(255),
    exam_room_id     INT,
    application_name TEXT,
    image_url        TEXT
) ENGINE=InnoDB;

-- ========================= ban_application ===========================
CREATE TABLE ban_application (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    status           INT,
    created_at       DATETIME,
    created_user     VARCHAR(255),
    updated_at       DATETIME,
    updated_user     VARCHAR(255),
    teacher_id       INT,
    application_name TEXT,
    image_url        TEXT
) ENGINE=InnoDB;

-- ======================== incident_reports ===========================
-- reporter_id is polymorphic (resolved by reporter_role) and handler_id
-- references different role tables -> left WITHOUT FK on purpose.
-- room_id has a real FK below.
CREATE TABLE incident_reports (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    status        INT,
    created_at    DATETIME,
    created_user  VARCHAR(255),
    updated_at    DATETIME,
    updated_user  VARCHAR(255),
    title         VARCHAR(255),
    room_id       INT,
    reporter_id   INT,
    reporter_role VARCHAR(20),
    handler_id    INT
) ENGINE=InnoDB;

-- ======================== student_class_info =========================
CREATE TABLE student_class_info (
    id                       INT AUTO_INCREMENT PRIMARY KEY,
    status                   INT,
    created_at               DATETIME,
    created_user             VARCHAR(255),
    updated_at               DATETIME,
    updated_user             VARCHAR(255),
    student_class_id         INT,
    application_name         TEXT,
    is_ban_application       BOOLEAN DEFAULT FALSE,
    connection_type          VARCHAR(20),
    action                   INT DEFAULT 0,
    clipboard_text_encrypted TEXT,
    clipboard_key_encrypted  TEXT,
    clipboard_iv             TEXT
) ENGINE=InnoDB;

-- ====================== student_exam_room_info =======================
CREATE TABLE student_exam_room_info (
    id                       INT AUTO_INCREMENT PRIMARY KEY,
    status                   INT,
    created_at               DATETIME,
    created_user             VARCHAR(255),
    updated_at               DATETIME,
    updated_user             VARCHAR(255),
    student_exam_room_id     INT,
    application_name         TEXT,
    is_violation             BOOLEAN DEFAULT FALSE,
    connection_type          VARCHAR(20),
    action                   INT DEFAULT 0,
    clipboard_text_encrypted TEXT,
    clipboard_key_encrypted  TEXT,
    clipboard_iv             TEXT
) ENGINE=InnoDB;

-- ======================== screenshot_capture =========================
CREATE TABLE screenshot_capture (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    status               INT,
    created_at           DATETIME,
    created_user         VARCHAR(255),
    updated_at           DATETIME,
    updated_user         VARCHAR(255),
    student_class_id     INT,
    student_exam_room_id INT,
    image_path           TEXT
) ENGINE=InnoDB;

-- =====================================================================
-- FOREIGN KEYS — these draw the relationship lines in Workbench.
-- =====================================================================

ALTER TABLE users
    ADD CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE sections
    ADD CONSTRAINT fk_sections_department
        FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE majors
    ADD CONSTRAINT fk_majors_department
        FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE subjects
    ADD CONSTRAINT fk_subjects_section
        FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE teachers
    ADD CONSTRAINT fk_teachers_section
        FOREIGN KEY (section_id) REFERENCES sections (id),
    ADD CONSTRAINT fk_teachers_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE manage_class
    ADD CONSTRAINT fk_manage_class_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    ADD CONSTRAINT fk_manage_class_major
        FOREIGN KEY (major_id) REFERENCES majors (id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_manage_class
        FOREIGN KEY (manage_class_id) REFERENCES manage_class (id),
    ADD CONSTRAINT fk_students_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE classes
    ADD CONSTRAINT fk_classes_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id),
    ADD CONSTRAINT fk_classes_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers (id),
    ADD CONSTRAINT fk_classes_schedule
        FOREIGN KEY (schedule_id) REFERENCES schedules (id),
    ADD CONSTRAINT fk_classes_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id),
    ADD CONSTRAINT fk_classes_room
        FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE exam_room
    ADD CONSTRAINT fk_exam_room_room
        FOREIGN KEY (room_id) REFERENCES rooms (id),
    ADD CONSTRAINT fk_exam_room_teacher1
        FOREIGN KEY (teacher1_id) REFERENCES teachers (id),
    ADD CONSTRAINT fk_exam_room_teacher2
        FOREIGN KEY (teacher2_id) REFERENCES teachers (id),
    ADD CONSTRAINT fk_exam_room_subject
        FOREIGN KEY (subject_id) REFERENCES subjects (id),
    ADD CONSTRAINT fk_exam_room_semester
        FOREIGN KEY (semester_id) REFERENCES semesters (id);

ALTER TABLE personal_computer
    ADD CONSTRAINT fk_personal_computer_user
        FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE student_class
    ADD CONSTRAINT fk_student_class_class
        FOREIGN KEY (class_id) REFERENCES classes (id),
    ADD CONSTRAINT fk_student_class_student
        FOREIGN KEY (student_id) REFERENCES students (id);

ALTER TABLE student_exam_room
    ADD CONSTRAINT fk_student_exam_room_exam_room
        FOREIGN KEY (exam_room_id) REFERENCES exam_room (id),
    ADD CONSTRAINT fk_student_exam_room_student
        FOREIGN KEY (student_id) REFERENCES students (id);

ALTER TABLE allowed_application
    ADD CONSTRAINT fk_allowed_application_exam_room
        FOREIGN KEY (exam_room_id) REFERENCES exam_room (id);

ALTER TABLE ban_application
    ADD CONSTRAINT fk_ban_application_teacher
        FOREIGN KEY (teacher_id) REFERENCES teachers (id);

ALTER TABLE incident_reports
    ADD CONSTRAINT fk_incident_reports_room
        FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE student_class_info
    ADD CONSTRAINT fk_student_class_info_student_class
        FOREIGN KEY (student_class_id) REFERENCES student_class (id);

ALTER TABLE student_exam_room_info
    ADD CONSTRAINT fk_student_exam_room_info_student_exam_room
        FOREIGN KEY (student_exam_room_id) REFERENCES student_exam_room (id);

ALTER TABLE screenshot_capture
    ADD CONSTRAINT fk_screenshot_capture_student_class
        FOREIGN KEY (student_class_id) REFERENCES student_class (id),
    ADD CONSTRAINT fk_screenshot_capture_student_exam_room
        FOREIGN KEY (student_exam_room_id) REFERENCES student_exam_room (id);
