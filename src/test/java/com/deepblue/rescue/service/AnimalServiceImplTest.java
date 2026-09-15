package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldFindAnimalByCode() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);

        AnimalResponse expectedResponse = new AnimalResponse(
                1L,
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                "RES-001",
                RescueStatus.IN_REHABILITATION
        );

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(mapper.toResponse(animal))
                .thenReturn(expectedResponse);


        AnimalResponse result = service.findByCode("AN-001");

        assertThat(result).isEqualTo(expectedResponse);

        verify(repository).findByAnimalCode("AN-001");
        verify(mapper).toResponse(animal);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenAnimalNotFound() {

        when(repository.findByAnimalCode("AN-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");

        verify(repository).findByAnimalCode("AN-999");
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldReturnAnimalsInRehabilitation() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);

        AnimalResponse response = new AnimalResponse(
                1L,
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE,
                "RES-001",
                RescueStatus.IN_REHABILITATION
        );

        when(repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(animal));

        when(mapper.toResponse(animal))
                .thenReturn(response);


        List<AnimalResponse> result = service.findAnimalsInRehabilitation();

        assertThat(result)
                .hasSize(1)
                .containsExactly(response);

        verify(repository).findByRescueCaseStatus(RescueStatus.IN_REHABILITATION);
        verify(mapper).toResponse(animal);
    }

    @Test
    void canReceiveTreatment_returnsTrueForUnderEvaluation() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.UNDER_EVALUATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isTrue();

        verify(repository).findByAnimalCode("AN-001");
    }

    @Test
    void canReceiveTreatment_returnsTrueForInRehabilitation() {
        // Arrange
        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isTrue();
    }

    @Test
    void canReceiveTreatment_returnsFalseForReleased() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.RELEASED
        );

        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );
        rescueCase.assignAnimal(animal);

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isFalse();
    }

    @Test
    void canReceiveTreatment_throwsWhenAnimalNotFound() {

        when(repository.findByAnimalCode("AN-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canReceiveTreatment("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");
    }
}