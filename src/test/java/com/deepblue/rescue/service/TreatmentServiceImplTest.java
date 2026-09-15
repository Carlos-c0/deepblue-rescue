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
import static org.mockito.ArgumentMatchers.any;
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
                .thenReturn(null);

        when(mapper.toResponse(any(Treatment.class)))
                .thenReturn(expectedResponse);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(expectedResponse);

        verify(animalRepository).findByAnimalCode("AN-001");
        verify(specialistRepository).findByProfessionalCode("SPEC-001");
        verify(treatmentRepository).save(any(Treatment.class));
        verify(mapper).toResponse(any(Treatment.class));
    }
}