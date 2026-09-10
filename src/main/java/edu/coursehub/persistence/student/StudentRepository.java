package edu.coursehub.persistence.student;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    // TODO-STUDENT S05: findByEmail, existsByEmail,
    // findByNameContainingIgnoreCaseAndActiveTrue y paginación.
}
