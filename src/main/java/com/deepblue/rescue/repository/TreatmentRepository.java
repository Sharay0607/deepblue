package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    // Tratamientos de un animal ordenados cronologicamente.
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    // Tratamientos realizados entre dos fechas.
    @Query("""
            select t
            from Treatment t
            where t.performedAt between :start and :end
            order by t.performedAt asc
            """)
    List<Treatment> findByPerformedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // JPQL navegando tres entidades: Treatment -> Animal -> RescueCase -> RescueCenter
    @Query("""
            select t
            from Treatment t
            join t.animal a
            join a.rescueCase rc
            join rc.rescueCenter rcenter
            where rcenter.code = :centerCode
            order by t.performedAt asc
            """)
    List<Treatment> findByAnimalRescueCaseRescueCenterCode(@Param("centerCode") String centerCode);

    // JPQL con N:M: Treatment -> Specialist -> Expertise
    @Query("""
            select distinct t
            from Treatment t
            join t.specialist s
            join s.expertiseAreas e
            where lower(e.name) = lower(:expertiseName)
            order by t.performedAt asc
            """)
    List<Treatment> findBySpecialistExpertise(@Param("expertiseName") String expertiseName);
}
