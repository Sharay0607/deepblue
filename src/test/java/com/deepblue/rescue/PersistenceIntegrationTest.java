package com.deepblue.rescue;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.ExpertiseRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.repository.RescueCenterRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("deepblue_test")
                    .withUsername("deepblue")
                    .withPassword("deepblue");

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    // NOTA: el paso 47 (test de flyway_schema_history) queda fuera de este
    // laboratorio a pedido explícito, por lo que no se implementa aquí.

    // ---------------------------------------------------------------
    // Paso 48 - metodos heredados
    // ---------------------------------------------------------------
    @Test
    void inheritedMethodsWorkOnRescueCenter() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(rescueCenterRepository.findById(saved.getId())).isPresent();
        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isGreaterThanOrEqualTo(1);
    }

    // ---------------------------------------------------------------
    // Paso 49 - relacion 1:N
    // ---------------------------------------------------------------
    @Test
    void oneCenterCanHaveManyCases() {
        RescueCenter center = new RescueCenter("DB-1N", "DeepBlue One To Many", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase("RES-1N-001", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-1N-002", LocalDate.now(), "Taganga", RescueStatus.ADMITTED);
        center.addCase(case1);
        center.addCase(case2);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-1N");

        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(c -> c.getRescueCenter().getCode().equals("DB-1N"));
    }

    // ---------------------------------------------------------------
    // Paso 50 - relacion 1:1 RescueCase <-> Animal
    // ---------------------------------------------------------------
    @Test
    void rescueCaseAndAnimalAreLinkedBothWays() {
        RescueCenter center = new RescueCenter("DB-11A", "DeepBlue 1:1 Animal", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        assertThat(rescueCase.getAnimal()).isEqualTo(animal);
        assertThat(animal.getRescueCase()).isEqualTo(rescueCase);
    }

    // ---------------------------------------------------------------
    // Paso 51 - relacion 1:1 Animal <-> MedicalRecord (cascade)
    // ---------------------------------------------------------------
    @Test
    void animalAndMedicalRecordArePersistedTogetherViaCascade() {
        Animal animal = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"), "STABLE", "Left front flipper injury", null);
        animal.assignMedicalRecord(record);

        Animal saved = animalRepository.save(animal);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMedicalRecord().getId()).isNotNull();
    }

    // ---------------------------------------------------------------
    // Paso 52 - relacion N:M Specialist <-> Expertise
    // ---------------------------------------------------------------
    @Test
    void specialistCanHaveMultipleExpertiseAreas() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-NM-001", "Elena", "Vargas", "elena.nm@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist saved = specialistRepository.save(elena);

        assertThat(saved.getExpertiseAreas()).hasSize(2);
    }

    // ---------------------------------------------------------------
    // Paso 53 - Query Method simple por status
    // ---------------------------------------------------------------
    @Test
    void findByStatusReturnsOnlyMatchingCases() {
        RescueCenter center = new RescueCenter("DB-QMS", "DeepBlue Query Method Status", "Santa Marta");
        rescueCenterRepository.save(center);

        center.addCase(new RescueCase("RES-001", LocalDate.now(), "Loc A", RescueStatus.IN_REHABILITATION));
        center.addCase(new RescueCase("RES-002", LocalDate.now(), "Loc B", RescueStatus.READY_FOR_RELEASE));
        center.addCase(new RescueCase("RES-003", LocalDate.now(), "Loc C", RescueStatus.IN_REHABILITATION));
        center.getCases().forEach(rescueCaseRepository::save);

        List<RescueCase> inRehab = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(inRehab).hasSize(2);
    }

    // ---------------------------------------------------------------
    // Paso 54 - Query Method navegando relaciones (Animal -> centro)
    // ---------------------------------------------------------------
    @Test
    void findAnimalsByCenterCodeNavigatesRelations() {
        RescueCenter caribbean = new RescueCenter("DB-CAR-54", "DeepBlue Caribbean", "Santa Marta");
        RescueCenter pacific = new RescueCenter("DB-PAC-54", "DeepBlue Pacific", "Buenaventura");
        rescueCenterRepository.save(caribbean);
        rescueCenterRepository.save(pacific);

        RescueCase caseCar = new RescueCase("RES-CAR-54", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        caribbean.addCase(caseCar);
        rescueCaseRepository.save(caseCar);
        Animal animalCar = new Animal("AN-CAR-54", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        caseCar.assignAnimal(animalCar);
        animalRepository.save(animalCar);

        RescueCase casePac = new RescueCase("RES-PAC-54", LocalDate.now(), "Malaga Bay", RescueStatus.ADMITTED);
        pacific.addCase(casePac);
        rescueCaseRepository.save(casePac);
        Animal animalPac = new Animal("AN-PAC-54", "Humpback Whale", "Megaptera novaeangliae", AnimalSex.MALE);
        casePac.assignAnimal(animalPac);
        animalRepository.save(animalPac);

        List<Animal> caribbeanAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR-54");

        assertThat(caribbeanAnimals).extracting(Animal::getAnimalCode).containsExactly("AN-CAR-54");
    }

    // ---------------------------------------------------------------
    // Paso 55 - JPQL de especialistas por expertise
    // ---------------------------------------------------------------
    @Test
    void findActiveSpecialistsByExpertiseUsesJpql() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SPEC-55-001", "Elena", "Vargas", "elena.55@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist("SPEC-55-002", "Mateo", "Rios", "mateo.55@deepblue.org", true);
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);
        specialistRepository.save(mateo);

        Specialist sofia = new Specialist("SPEC-55-003", "Sofia", "Blanco", "sofia.55@deepblue.org", true);
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);
        specialistRepository.save(sofia);

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("trauma");

        assertThat(traumaSpecialists).extracting(Specialist::getFirstName).containsExactlyInAnyOrder("Elena", "Sofia");
    }

    // ---------------------------------------------------------------
    // Paso 56/57 - tratamientos y Query Method cronologico
    // ---------------------------------------------------------------
    @Test
    void treatmentsForAnimalAreReturnedInChronologicalOrder() {
        Animal animal = animalRepository.save(new Animal("AN-56", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE));
        Specialist elena = specialistRepository.save(new Specialist("SPEC-56-001", "Elena", "Vargas", "elena.56@deepblue.org", true));
        Specialist mateo = specialistRepository.save(new Specialist("SPEC-56-002", "Mateo", "Rios", "mateo.56@deepblue.org", true));

        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 1, 9, 0), TreatmentType.WOUND_CARE, "Treatment 1"));
        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 2, 9, 0), TreatmentType.HYDRATION, "Treatment 2"));
        treatmentRepository.save(new Treatment(animal, mateo,
                LocalDateTime.of(2026, 8, 3, 9, 0), TreatmentType.OBSERVATION, "Treatment 3"));

        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertThat(treatments).extracting(Treatment::getDescription)
                .containsExactly("Treatment 1", "Treatment 2", "Treatment 3");
    }

    // ---------------------------------------------------------------
    // Paso 58 - JPQL por intervalo de fechas
    // ---------------------------------------------------------------
    @Test
    void treatmentsBetweenDatesUsesJpql() {
        Animal animal = animalRepository.save(new Animal("AN-58", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE));
        Specialist elena = specialistRepository.save(new Specialist("SPEC-58-001", "Elena", "Vargas", "elena.58@deepblue.org", true));

        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Early"));
        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Middle"));
        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Late"));

        List<Treatment> result = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0));

        assertThat(result).extracting(Treatment::getDescription).containsExactly("Middle");
    }

    // ---------------------------------------------------------------
    // Paso 59 - constraint UNIQUE real en PostgreSQL
    // ---------------------------------------------------------------
    @Test
    void duplicateAnimalCodeViolatesUniqueConstraint() {
        animalRepository.saveAndFlush(new Animal("AN-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE));

        assertThrows(DataIntegrityViolationException.class, () ->
                animalRepository.saveAndFlush(new Animal("AN-100", "Loggerhead Turtle", "Caretta caretta", AnimalSex.MALE)));
    }

    // ---------------------------------------------------------------
    // Paso 61 - constraint CHECK real en PostgreSQL
    // ---------------------------------------------------------------
    @Test
    void checkConstraintRejectsInvalidStatusAtDatabaseLevel() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CHK", "DeepBlue Check Constraint", "Santa Marta"));

        RescueCase invalidCase = new RescueCase("RES-CHK-001", LocalDate.now(), "Loc", RescueStatus.ADMITTED);
        center.addCase(invalidCase);
        RescueCase saved = rescueCaseRepository.saveAndFlush(invalidCase);

        // Se fuerza un valor invalido directamente en la fila persistida,
        // saltandose la validacion de Java, para comprobar que PostgreSQL
        // sigue protegiendo la integridad del dato mediante el CHECK.
        assertThrows(Exception.class, () -> {
            saved.setStatus(null);
            rescueCaseRepository.saveAndFlush(saved);
        });
    }

    // -----------------------------------------------------------------
    // PARTE XII - Reto integrador (pasos 65 y 66)
    // -----------------------------------------------------------------
    @Test
    void integratorChallengeScenario() {
        RescueCenter center = new RescueCenter("DB-CAR-INT", "DeepBlue Caribbean", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100", LocalDate.of(2026, 8, 18), "Bahia Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord medicalRecord = new MedicalRecord(
                new BigDecimal("27.80"), "STABLE", "Injury caused by fishing net", "Possible plastic ingestion");
        animal.assignMedicalRecord(medicalRecord);
        animalRepository.save(animal);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 18, 11, 0), TreatmentType.WOUND_CARE, "Cleaning of left front flipper"));
        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.of(2026, 8, 18, 13, 0), TreatmentType.HYDRATION, "Subcutaneous fluid therapy"));

        // Consulta 1: existe el caso RES-2026-100?
        assertTrue(rescueCaseRepository.existsByCaseCode("RES-2026-100"));

        // Consulta 2: casos IN_REHABILITATION
        assertThat(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .extracting(RescueCase::getCaseCode).contains("RES-2026-100");

        // Consulta 3: animales de DB-CAR-INT
        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR-INT"))
                .extracting(Animal::getAnimalCode).containsExactly("AN-2026-100");

        // Consulta 4: animales cuyo nombre comun contiene "turtle"
        assertThat(animalRepository.findByCommonNameContainingIgnoreCase("turtle"))
                .extracting(Animal::getAnimalCode).contains("AN-2026-100");

        // Consulta 5: especialistas con experiencia en Trauma
        assertThat(specialistRepository.findActiveByExpertise("Trauma"))
                .extracting(Specialist::getProfessionalCode).contains("SPEC-001");

        // Consulta 6: tratamientos de AN-2026-100 ordenados cronologicamente
        assertThat(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId()))
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");

        // Consulta 7: tratamientos por especialistas con experiencia en Rehabilitation
        assertThat(treatmentRepository.findBySpecialistExpertise("Rehabilitation")).hasSize(2);

        // Consulta 8: tratamientos entre dos fechas
        assertThat(treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 19, 0, 0))).hasSize(2);
    }

    // -----------------------------------------------------------------
    // PARTE XIII - Reto sin guia (paso 77)
    // -----------------------------------------------------------------
    @Test
    void animalsInRehabilitationTreatedByTraumaSpecialist() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-77", "DeepBlue Challenge", "Santa Marta"));

        RescueCase rescueCase = new RescueCase(
                "RES-77-001", LocalDate.now(), "Bahia Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        rescueCaseRepository.save(rescueCase);

        Animal animal = new Animal("AN-77-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        animalRepository.save(animal);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Specialist elena = specialistRepository.save(new Specialist("SPEC-77-001", "Elena", "Vargas", "elena.77@deepblue.org", true));
        elena.addExpertise(trauma);
        specialistRepository.save(elena);

        treatmentRepository.save(new Treatment(animal, elena,
                LocalDateTime.now(), TreatmentType.WOUND_CARE, "Trauma related care"));

        List<Animal> result = animalRepository.findInRehabilitationTreatedBySpecialistWithExpertise(
                RescueStatus.IN_REHABILITATION, "trauma");

        assertThat(result).extracting(Animal::getAnimalCode).contains("AN-77-001");
    }
}
