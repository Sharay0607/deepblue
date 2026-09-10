package edu.coursehub.persistence.student;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
class StudentRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Disabled("TODO-STUDENT S08: implementar y habilitar")
    @Test
    void shouldPersistAndRetrieveStudent() {
        // Arrange
        // Act
        // Assert
    }

    @Disabled("TODO-STUDENT S08: comprobar constraint UNIQUE real")
    @Test
    void shouldRejectDuplicatedEmail() {
    }
}
