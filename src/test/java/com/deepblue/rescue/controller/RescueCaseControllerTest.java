package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService service;

    // ------------------------------------------------------------ GET /{caseCode}

    @Test
    void shouldReturnRescueCaseByCode() throws Exception {
        // ARRANGE
        when(service.findByCode("RES-2026-001"))
                .thenReturn(rescueCase("RES-2026-001", RescueStatus.IN_REHABILITATION));

        // ACT + ASSERT
        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-2026-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-2026-001"))
                .andExpect(jsonPath("$.status").value("IN_REHABILITATION"))
                .andExpect(jsonPath("$.centerCode").value("DB-CAR"))
                .andExpect(jsonPath("$.animalCode").value("AN-2026-001"));

        verify(service).findByCode("RES-2026-001");
    }

    @Test
    void shouldReturn404WhenCaseDoesNotExist() throws Exception {
        // ARRANGE
        when(service.findByCode("RES-999"))
                .thenThrow(new ResourceNotFoundException("Rescue case not found: RES-999"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rescue case not found: RES-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // -------------------------------------------------------- GET ?status=...

    @Test
    void shouldReturnCasesByStatus() throws Exception {
        // ARRANGE
        when(service.findByStatus(RescueStatus.IN_REHABILITATION)).thenReturn(List.of(
                rescueCase("RES-2026-001", RescueStatus.IN_REHABILITATION),
                rescueCase("RES-2026-002", RescueStatus.IN_REHABILITATION)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/rescue-cases").param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IN_REHABILITATION"))
                .andExpect(jsonPath("$[1].status").value("IN_REHABILITATION"));

        verify(service).findByStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldReturn400WhenStatusQueryParamIsInvalid() throws Exception {
        // ACT + ASSERT: FLYING no pertenece a RescueStatus
        mockMvc.perform(get("/api/rescue-cases").param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status").exists());

        verify(service, never()).findByStatus(any());
    }

    // ------------------------------------------------------- PATCH /{code}/status

    @Test
    void shouldChangeStatus() throws Exception {
        // ARRANGE
        when(service.changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class)))
                .thenReturn(rescueCase("RES-001", RescueStatus.READY_FOR_RELEASE));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "READY_FOR_RELEASE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_RELEASE"));

        verify(service).changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class));
    }

    @Test
    void shouldReturn400WhenChangeStatusBodyIsEmpty() throws Exception {
        // ACT + ASSERT: body {} → status == null
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.status").value("Status is required"));

        verify(service, never()).changeStatus(anyString(), any());
    }

    @Test
    void shouldReturn400WhenChangeStatusIsExplicitlyNull() throws Exception {
        // ACT + ASSERT: reto integrador, operación 5
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-2026-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": null }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.status").value("Status is required"));

        verify(service, never()).changeStatus(anyString(), any());
    }

    @Test
    void shouldReturn409WhenStatusTransitionIsInvalid() throws Exception {
        // ARRANGE
        when(service.changeStatus(eq("RES-001"), any()))
                .thenThrow(new BusinessRuleException("Invalid status transition"));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "RELEASED" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Invalid status transition"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturn404WhenChangingStatusOfMissingCase() throws Exception {
        // ARRANGE
        when(service.changeStatus(eq("RES-999"), any()))
                .thenThrow(new ResourceNotFoundException("Rescue case not found: RES-999"));

        // ACT + ASSERT
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "UNDER_EVALUATION" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rescue case not found: RES-999"));
    }

    // ------------------------------------------------------------- JSON inválido

    @Test
    void shouldReturn400WhenJsonEnumValueIsInvalid() throws Exception {
        // ACT + ASSERT: FLYING no pertenece a RescueStatus
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "FLYING" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"))
                .andExpect(jsonPath("$.details.body").exists());

        verify(service, never()).changeStatus(anyString(), any());
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        // ACT + ASSERT: llave sin cerrar
        mockMvc.perform(patch("/api/rescue-cases/{code}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(service, never()).changeStatus(anyString(), any());
    }

    // ----------------------------------------------------------- errores globales

    @Test
    void shouldReturn500WhenUnexpectedErrorOccurs() throws Exception {
        // ARRANGE
        when(service.findByCode("RES-500"))
                .thenThrow(new IllegalStateException("db password is secret123"));

        // ACT + ASSERT: el detalle interno NO se expone al cliente
        mockMvc.perform(get("/api/rescue-cases/{code}", "RES-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("secret123"));
    }

    @Test
    void shouldReturn405WhenHttpMethodIsNotSupported() throws Exception {
        // ACT + ASSERT: no existe DELETE; no debe degradarse a un 500
        mockMvc.perform(delete("/api/rescue-cases/{code}", "RES-001"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    // -------------------------------------------------------------------- helpers

    private static RescueCaseResponse rescueCase(String caseCode, RescueStatus status) {
        return new RescueCaseResponse(
                1L,
                caseCode,
                LocalDate.of(2026, 8, 20),
                "Bahia Concha",
                status,
                "DB-CAR",
                "AN-2026-001");
    }
}
