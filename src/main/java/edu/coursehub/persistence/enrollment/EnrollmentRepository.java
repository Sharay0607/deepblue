package edu.coursehub.persistence.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    // TODO-STUDENT S05: exists/count/find derivados.
    // TODO-STUDENT S06: consultas JPQL.
    // TODO-STUDENT S11: resolver fetch para evitar N+1.
}
