package edu.coursehub.persistence.enrollment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
class EnrollmentRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Disabled("TODO-STUDENT S08/S09: promedio de notas mediante JPQL")
    @Test
    void shouldCalculateAverageGrade() {
    }

    @Disabled("TODO-STUDENT S11: demostrar y resolver N+1")
    @Test
    void shouldLoadEnrollmentGraphEfficiently() {
    }
}
