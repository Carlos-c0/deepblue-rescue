package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    @Test
    void shouldFindRescueCaseByCode() {

        RescueCase rescueCase = new RescueCase(
                "RES-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        RescueCaseResponse expectedResponse = new RescueCaseResponse(
                1L,
                "RES-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION,
                "DB-CAR",
                null
        );

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        when(mapper.toResponse(rescueCase))
                .thenReturn(expectedResponse);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(expectedResponse);

        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenNotFound() {

        when(repository.findByCaseCode("RES-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RES-999");

        verify(repository).findByCaseCode("RES-999");
        verify(mapper, never()).toResponse(any());
    }
}