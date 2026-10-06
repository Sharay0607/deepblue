package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@Import(GlobalExceptionHandler.class)
class AnimalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    // ---------------------------------------------------------- GET /{animalCode}

    @Test
    void shouldReturnAnimalByCode() throws Exception {
        // ARRANGE
        when(animalService.findByCode("AN-001")).thenReturn(animal("AN-001"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/animals/{code}", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.commonName").value("Green Sea Turtle"))
                .andExpect(jsonPath("$.rescueStatus").value("IN_REHABILITATION"));

        verify(animalService).findByCode("AN-001");
        verifyNoInteractions(treatmentService);
    }

    @Test
    void shouldReturn404WhenAnimalDoesNotExist() throws Exception {
        // ARRANGE
        when(animalService.findByCode("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/animals/{code}", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    // ------------------------------------------------------ GET /in-rehabilitation

    @Test
    void shouldReturnAnimalsInRehabilitation() throws Exception {
        // ARRANGE
        when(animalService.findAnimalsInRehabilitation())
                .thenReturn(List.of(animal("AN-001"), animal("AN-002")));

        // ACT + ASSERT: "/in-rehabilitation" no se confunde con "/{animalCode}"
        mockMvc.perform(get("/api/animals/in-rehabilitation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].animalCode").value("AN-001"))
                .andExpect(jsonPath("$[1].animalCode").value("AN-002"));

        verify(animalService).findAnimalsInRehabilitation();
    }

    // ------------------------------------------------- GET /{animalCode}/treatments

    @Test
    void shouldReturnAnimalTreatments() throws Exception {
        // ARRANGE
        when(treatmentService.findByAnimalCode("AN-001")).thenReturn(List.of(
                treatment(1L, TreatmentType.WOUND_CARE),
                treatment(2L, TreatmentType.HYDRATION)));

        // ACT + ASSERT
        mockMvc.perform(get("/api/animals/{code}/treatments", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("WOUND_CARE"))
                .andExpect(jsonPath("$[1].type").value("HYDRATION"));

        verify(treatmentService).findByAnimalCode("AN-001");
        verifyNoInteractions(animalService);
    }

    // ------------------------------------ GET /{animalCode}/treatment-eligibility

    @Test
    void shouldReturnTreatmentEligibility() throws Exception {
        // ARRANGE
        when(animalService.canReceiveTreatment("AN-001")).thenReturn(true);

        // ACT + ASSERT
        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-001"))
                .andExpect(jsonPath("$.eligible").value(true));

        verify(animalService).canReceiveTreatment("AN-001");
    }

    @Test
    void shouldReturnNotEligibleWhenAnimalCannotReceiveTreatment() throws Exception {
        // ARRANGE
        when(animalService.canReceiveTreatment("AN-002")).thenReturn(false);

        // ACT + ASSERT: false es una respuesta válida (200), no un error
        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalCode").value("AN-002"))
                .andExpect(jsonPath("$.eligible").value(false));

        verify(animalService).canReceiveTreatment("AN-002");
    }

    @Test
    void shouldReturn404WhenEligibilityIsRequestedForMissingAnimal() throws Exception {
        // ARRANGE
        when(animalService.canReceiveTreatment("AN-999"))
                .thenThrow(new ResourceNotFoundException("Animal not found: AN-999"));

        // ACT + ASSERT
        mockMvc.perform(get("/api/animals/{code}/treatment-eligibility", "AN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Animal not found: AN-999"))
                .andExpect(jsonPath("$.details").isMap());

        verify(animalService).canReceiveTreatment("AN-999");
    }

    // -------------------------------------------------------------------- helpers

    private static AnimalResponse animal(String code) {
        return new AnimalResponse(
                1L,
                code,
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                "RES-2026-001",
                RescueStatus.IN_REHABILITATION);
    }

    private static TreatmentResponse treatment(Long id, TreatmentType type) {
        return new TreatmentResponse(
                id,
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                type,
                "Treatment performed by the specialist.");
    }
}
