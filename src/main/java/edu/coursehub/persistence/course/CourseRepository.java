package edu.coursehub.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // TODO-STUDENT S05: consultas derivadas.
    // TODO-STUDENT S06: JPQL para cursos sin matrículas.
    // TODO-STUDENT S06: consulta nativa PostgreSQL con ILIKE.
}
