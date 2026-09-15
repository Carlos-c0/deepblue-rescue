package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    @Test
    void shouldRegisterTreatmentWhenAllRulesPass() {
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

        // 👇 ESTA línea faltaba — enlaza animal ↔ rescueCase
        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );

        LocalDateTime performedAt =
                LocalDateTime.of(2026, 8, 19, 9, 0);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                performedAt,
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );

        TreatmentResponse expectedResponse = new TreatmentResponse(
                1L,
                "AN-001",
                "SPEC-001",
                performedAt,
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        when(treatmentRepository.save(any(Treatment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(mapper.toResponse(any(Treatment.class)))
                .thenReturn(expectedResponse);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(expectedResponse);

        verify(animalRepository).findByAnimalCode("AN-001");
        verify(specialistRepository).findByProfessionalCode("SPEC-001");
        verify(treatmentRepository).save(any(Treatment.class));
        verify(mapper).toResponse(any(Treatment.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenSpecialistIsInactive() {
        Animal animal = new Animal(
                "AN-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );
        specialist.setActive(false);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SPEC-001");

        verify(animalRepository).findByAnimalCode("AN-001");
        verify(specialistRepository).findByProfessionalCode("SPEC-001");
        verify(treatmentRepository, never()).save(any(Treatment.class));
        verify(mapper, never()).toResponse(any(Treatment.class));
    }

    @Test
    void shouldThrowBusinessRuleExceptionWhenCaseIsReleased() {
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

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.OBSERVATION,
                "General observation"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RELEASED");

        verify(animalRepository).findByAnimalCode("AN-001");
        verify(specialistRepository).findByProfessionalCode("SPEC-001");
        verify(treatmentRepository, never()).save(any(Treatment.class));
        verify(mapper, never()).toResponse(any(Treatment.class));
    }
    @Test
    void shouldThrowBusinessRuleExceptionWhenTreatmentDateIsBeforeRescueDate() {

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 20),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        rescueCase.assignAnimal(animal);

        Specialist specialist = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 15, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        when(animalRepository.findByAnimalCode("AN-2026-100"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2026-08-15")
                .hasMessageContaining("2026-08-20");

        verify(treatmentRepository, never()).save(any(Treatment.class));
        verify(mapper, never()).toResponse(any(Treatment.class));
    }

}