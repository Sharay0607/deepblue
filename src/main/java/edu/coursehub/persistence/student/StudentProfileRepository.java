package edu.coursehub.persistence.student;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    // TODO-STUDENT S05
}
