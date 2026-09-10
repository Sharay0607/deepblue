CREATE INDEX idx_course_department
    ON course(department_id);

CREATE INDEX idx_enrollment_student_status
    ON enrollment(student_id, status);

CREATE INDEX idx_enrollment_course_status
    ON enrollment(course_id, status);
