package com.deepblue.rescue;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flyway_migration_history_contains_V1_and_V2() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                String.class
        );

        assertThat(versions)
                .contains("1")
                .contains("2");
    }

    @Test
    void inherited_methods_work_for_RescueCenter() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();

        assertThat(rescueCenterRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(RescueCenter::getCode)
                .isEqualTo("DB-CAR");

        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();

        assertThat(rescueCenterRepository.count()).isEqualTo(1);
    }

    @Test
    void rescue_center_has_many_rescue_cases() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCase case1 = new RescueCase(
                "RES-2026-001",
                LocalDate.of(2026, 8, 10),
                "Bahía Concha",
                RescueStatus.ADMITTED
        );

        RescueCase case2 = new RescueCase(
                "RES-2026-002",
                LocalDate.of(2026, 8, 12),
                "Taganga",
                RescueStatus.IN_REHABILITATION
        );

        center.addCase(case1);
        center.addCase(case2);

        rescueCenterRepository.save(center);

        List<RescueCase> cases =
                rescueCaseRepository.findByRescueCenterCode("DB-CAR");

        assertThat(cases)
                .hasSize(2)
                .extracting(RescueCase::getCaseCode)
                .containsExactlyInAnyOrder("RES-2026-001", "RES-2026-002");

        assertThat(cases)
                .allSatisfy(c -> assertThat(c.getRescueCenter().getCode())
                        .isEqualTo("DB-CAR"));
    }

    @Test
    void rescue_case_has_one_animal() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-001",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);

        rescueCenterRepository.save(center);

        RescueCase foundCase =
                rescueCaseRepository.findByCaseCode("RES-2026-001")
                        .orElseThrow();

        assertThat(foundCase.getAnimal()).isNotNull();
        assertThat(foundCase.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(foundCase.getAnimal().getCommonName()).isEqualTo("Green Sea Turtle");
        assertThat(foundCase.getAnimal().getScientificName()).isEqualTo("Chelonia mydas");
        assertThat(foundCase.getAnimal().getSex()).isEqualTo(AnimalSex.FEMALE);

        Animal foundAnimal =
                animalRepository.findByAnimalCode("AN-2026-001")
                        .orElseThrow();

        assertThat(foundAnimal.getRescueCase()).isNotNull();
        assertThat(foundAnimal.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    @Test
    void animal_has_one_medical_record_with_cascade() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-002",
                LocalDate.of(2026, 8, 20),
                "Taganga",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-002",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"),
                "STABLE",
                "Left front flipper injury",
                null
        );

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        animal.assignMedicalRecord(record);

        rescueCenterRepository.save(center);

        assertThat(animal.getId()).isNotNull();
        assertThat(record.getId()).isNotNull();

        Animal foundAnimal =
                animalRepository.findByAnimalCode("AN-2026-002")
                        .orElseThrow();

        assertThat(foundAnimal.getMedicalRecord()).isNotNull();
        assertThat(foundAnimal.getMedicalRecord().getInitialWeight())
                .isEqualByComparingTo("28.40");
        assertThat(foundAnimal.getMedicalRecord().getInitialCondition())
                .isEqualTo("STABLE");
        assertThat(foundAnimal.getMedicalRecord().getInjuries())
                .isEqualTo("Left front flipper injury");
    }

    @Test
    void specialist_has_many_expertise_areas() {
        Expertise trauma = expertiseRepository
                .findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehabilitation = expertiseRepository
                .findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );

        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist saved = specialistRepository.save(elena);

        assertThat(saved.getId()).isNotNull();

        Specialist found = specialistRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(found.getExpertiseAreas())
                .hasSize(2)
                .extracting(Expertise::getName)
                .containsExactlyInAnyOrder("Trauma", "Rehabilitation");
    }

    @Test
    void find_cases_by_status_returns_only_matching() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCase case1 = new RescueCase(
                "RES-2026-001",
                LocalDate.of(2026, 8, 10),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        RescueCase case2 = new RescueCase(
                "RES-2026-002",
                LocalDate.of(2026, 8, 12),
                "Taganga",
                RescueStatus.READY_FOR_RELEASE
        );

        RescueCase case3 = new RescueCase(
                "RES-2026-003",
                LocalDate.of(2026, 8, 15),
                "Playa Blanca",
                RescueStatus.IN_REHABILITATION
        );

        center.addCase(case1);
        center.addCase(case2);
        center.addCase(case3);

        rescueCenterRepository.save(center);

        List<RescueCase> inRehab =
                rescueCaseRepository.findByStatusOrderByRescueDateAsc(
                        RescueStatus.IN_REHABILITATION
                );

        assertThat(inRehab)
                .hasSize(2)
                .extracting(RescueCase::getCaseCode)
                .containsExactly("RES-2026-001", "RES-2026-003");
    }

    @Test
    void find_animals_by_rescue_center_code() {
        RescueCenter caribbean = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCenter pacific = new RescueCenter(
                "DB-PAC",
                "DeepBlue Pacific Center",
                "Buenaventura"
        );

        RescueCase caseCaribbean = new RescueCase(
                "RES-CAR-001",
                LocalDate.of(2026, 8, 10),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animalCaribbean = new Animal(
                "AN-CAR-001",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        RescueCase casePacific = new RescueCase(
                "RES-PAC-001",
                LocalDate.of(2026, 8, 12),
                "Malpelo",
                RescueStatus.IN_REHABILITATION
        );

        Animal animalPacific = new Animal(
                "AN-PAC-001",
                "Humpback Whale",
                "Megaptera novaeangliae",
                AnimalSex.MALE
        );

        caribbean.addCase(caseCaribbean);
        caseCaribbean.assignAnimal(animalCaribbean);

        pacific.addCase(casePacific);
        casePacific.assignAnimal(animalPacific);

        rescueCenterRepository.save(caribbean);
        rescueCenterRepository.save(pacific);

        List<Animal> caribbeanAnimals =
                animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(caribbeanAnimals)
                .hasSize(1)
                .extracting(Animal::getAnimalCode)
                .containsExactly("AN-CAR-001");

        List<Animal> pacificAnimals =
                animalRepository.findByRescueCaseRescueCenterCode("DB-PAC");

        assertThat(pacificAnimals)
                .hasSize(1)
                .extracting(Animal::getAnimalCode)
                .containsExactly("AN-PAC-001");
    }

    @Test
    void find_active_specialists_by_expertise_trauma() {
        Expertise trauma = expertiseRepository
                .findByNameIgnoreCase("Trauma")
                .orElseThrow();

        Expertise rehabilitation = expertiseRepository
                .findByNameIgnoreCase("Rehabilitation")
                .orElseThrow();

        Expertise marineMammals = expertiseRepository
                .findByNameIgnoreCase("Marine Mammals")
                .orElseThrow();

        Expertise marineBirds = expertiseRepository
                .findByNameIgnoreCase("Marine Birds")
                .orElseThrow();

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist mateo = new Specialist(
                "SPEC-002",
                "Mateo",
                "Rojas",
                "mateo@deepblue.org"
        );
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);

        Specialist sofia = new Specialist(
                "SPEC-003",
                "Sofia",
                "Lozano",
                "sofia@deepblue.org"
        );
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);

        specialistRepository.saveAll(List.of(elena, mateo, sofia));

        List<Specialist> traumaSpecialists =
                specialistRepository.findActiveByExpertise("Trauma");

        assertThat(traumaSpecialists)
                .hasSize(2)
                .extracting(Specialist::getProfessionalCode)
                .containsExactlyInAnyOrder("SPEC-001", "SPEC-003");
    }

    @Test
    void create_treatments_for_animal() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.save(center);

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );

        Specialist mateo = new Specialist(
                "SPEC-002",
                "Mateo",
                "Rojas",
                "mateo@deepblue.org"
        );

        specialistRepository.saveAll(List.of(elena, mateo));

        Treatment t1 = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 10, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );

        Treatment t2 = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 15, 0),
                TreatmentType.HYDRATION,
                "Subcutaneous fluid therapy"
        );

        Treatment t3 = new Treatment(
                animal,
                mateo,
                LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.OBSERVATION,
                "General observation"
        );

        treatmentRepository.saveAll(List.of(t1, t2, t3));

        assertThat(t1.getId()).isNotNull();
        assertThat(t2.getId()).isNotNull();
        assertThat(t3.getId()).isNotNull();

        assertThat(treatmentRepository.count()).isEqualTo(3);
    }

    @Test
    void find_treatments_by_animal_ordered_chronologically() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 18),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.save(center);

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );

        Specialist mateo = new Specialist(
                "SPEC-002",
                "Mateo",
                "Rojas",
                "mateo@deepblue.org"
        );

        specialistRepository.saveAll(List.of(elena, mateo));

        Treatment t1 = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 10, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper"
        );

        Treatment t2 = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 18, 15, 0),
                TreatmentType.HYDRATION,
                "Subcutaneous fluid therapy"
        );

        Treatment t3 = new Treatment(
                animal,
                mateo,
                LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.OBSERVATION,
                "General observation"
        );

        treatmentRepository.saveAll(List.of(t1, t2, t3));

        List<Treatment> treatments =
                treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(
                        animal.getId()
                );

        assertThat(treatments)
                .hasSize(3)
                .extracting(Treatment::getType)
                .containsExactly(
                        TreatmentType.WOUND_CARE,
                        TreatmentType.HYDRATION,
                        TreatmentType.OBSERVATION
                );
    }

    @Test
    void find_treatments_between_dates_returns_only_in_range() {
        RescueCenter center = new RescueCenter(
                "DB-CAR",
                "DeepBlue Caribbean Center",
                "Santa Marta"
        );

        RescueCase rescueCase = new RescueCase(
                "RES-2026-100",
                LocalDate.of(2026, 8, 1),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION
        );

        Animal animal = new Animal(
                "AN-2026-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        center.addCase(rescueCase);
        rescueCase.assignAnimal(animal);
        rescueCenterRepository.save(center);

        Specialist elena = new Specialist(
                "SPEC-001",
                "Elena",
                "Vargas",
                "elena@deepblue.org"
        );

        specialistRepository.save(elena);

        Treatment before = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 1, 10, 0),
                TreatmentType.OBSERVATION,
                "Before range"
        );

        Treatment inside = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 10, 10, 0),
                TreatmentType.WOUND_CARE,
                "Inside range"
        );

        Treatment after = new Treatment(
                animal,
                elena,
                LocalDateTime.of(2026, 8, 20, 10, 0),
                TreatmentType.HYDRATION,
                "After range"
        );

        treatmentRepository.saveAll(List.of(before, inside, after));

        LocalDateTime start = LocalDateTime.of(2026, 8, 5, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 15, 23, 59, 59);

        List<Treatment> result =
                treatmentRepository.findBetweenDates(start, end);

        assertThat(result)
                .hasSize(1)
                .extracting(Treatment::getDescription)
                .containsExactly("Inside range");
    }

    @Test
    void unique_animal_code_violation_throws_DataIntegrityViolationException() {
        RescueCenter center1 = new RescueCenter(
                "DB-CAR-1",
                "DeepBlue Caribbean Center 1",
                "Santa Marta"
        );

        RescueCase rescueCase1 = new RescueCase(
                "RES-2026-901",
                LocalDate.of(2026, 8, 10),
                "Bahía Concha",
                RescueStatus.ADMITTED
        );

        Animal animal1 = new Animal(
                "AN-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.FEMALE
        );

        center1.addCase(rescueCase1);
        rescueCase1.assignAnimal(animal1);
        rescueCenterRepository.saveAndFlush(center1);

        RescueCenter center2 = new RescueCenter(
                "DB-CAR-2",
                "DeepBlue Caribbean Center 2",
                "Taganga"
        );

        RescueCase rescueCase2 = new RescueCase(
                "RES-2026-902",
                LocalDate.of(2026, 8, 12),
                "Taganga",
                RescueStatus.ADMITTED
        );

        Animal animal2 = new Animal(
                "AN-100",
                "Green Sea Turtle",
                "Chelonia mydas",
                AnimalSex.MALE
        );

        center2.addCase(rescueCase2);
        rescueCase2.assignAnimal(animal2);

        assertThatThrownBy(() -> rescueCenterRepository.saveAndFlush(center2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

}