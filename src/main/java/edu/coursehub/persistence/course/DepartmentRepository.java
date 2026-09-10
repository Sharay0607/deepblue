package edu.coursehub.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // TODO-STUDENT S05
}
