package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);

    List<Animal> findByCommonNameContainingIgnoreCase(String text);

    // Animal -> rescueCase -> status
    List<Animal> findByRescueCaseStatus(RescueStatus status);

    // Animal -> rescueCase -> rescueCenter -> code
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);

    // PARTE XIII - reto sin guia: animales en rehabilitacion tratados por
    // especialistas con determinada experiencia. Se eligio @Query + JPQL
    // porque combina un filtro simple (status) con la navegacion de una
    // coleccion (treatments -> specialist -> expertiseAreas), lo cual ya
    // no puede expresarse de forma clara solo con el nombre del metodo.
    @Query("""
            select distinct a
            from Animal a
            join a.treatments t
            join t.specialist s
            join s.expertiseAreas e
            where a.rescueCase.status = :status
            and lower(e.name) = lower(:expertiseName)
            """)
    List<Animal> findInRehabilitationTreatedBySpecialistWithExpertise(
            @Param("status") RescueStatus status,
            @Param("expertiseName") String expertiseName);
}
