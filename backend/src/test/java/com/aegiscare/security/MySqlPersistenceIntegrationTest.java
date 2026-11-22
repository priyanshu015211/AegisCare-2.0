package com.aegiscare.security;

import com.aegiscare.entity.Hospital;
import com.aegiscare.entity.Patient;
import com.aegiscare.repository.HospitalRepository;
import com.aegiscare.repository.PatientRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the real MySQL 8 migrations and JPA mappings when Docker is available. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("mysql-it")
class MySqlPersistenceIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("aegiscare")
            .withUsername("aegiscare_test")
            .withPassword("AegisCare-MySQL-Test-Password-2026!");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySqlPersistenceIntegrationTest::jdbcUrlWithUtc);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Flyway flyway;
    @Autowired HospitalRepository hospitalRepository;
    @Autowired PatientRepository patientRepository;

    @Test
    void appliesMysqlMigrationsAndUsesMysqlEight() {
        String productVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
        assertTrue(productVersion != null && productVersion.startsWith("8."),
                "Expected MySQL 8 but received: " + productVersion);

        Set<String> appliedVersions = Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().getVersion())
                .collect(Collectors.toSet());
        assertTrue(appliedVersions.containsAll(Set.of("1", "2")),
                "Expected V1 and V2 migrations to be applied; found " + appliedVersions);

        Integer innodbTables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = 'patients' AND engine = 'InnoDB'",
                Integer.class);
        assertEquals(1, innodbTables);
    }

    @Test
    void persistsUtcInstantsAndHospitalForeignKeys() {
        Hospital hospital = new Hospital();
        hospital.setName("MySQL Integration Hospital");
        hospital.setCity("Bengaluru");
        hospital.setTotalBeds(100);
        hospital.setCurrentOccupancy(40);
        hospital = hospitalRepository.saveAndFlush(hospital);

        Instant expected = Instant.parse("2026-11-21T09:15:30.123456Z");
        Patient patient = new Patient();
        patient.setPatientCode("MYSQL-UTC-TEST");
        patient.setFullName("Synthetic MySQL Test Patient");
        patient.setHospitalId(hospital.getId());
        patient.setCreatedAt(expected);
        patientRepository.saveAndFlush(patient);

        Patient reloaded = patientRepository.findByPatientCode("MYSQL-UTC-TEST").orElseThrow();
        assertEquals(expected, reloaded.getCreatedAt());
        assertEquals(hospital.getId(), reloaded.getHospitalId());
    }

    private static String jdbcUrlWithUtc() {
        String url = MYSQL.getJdbcUrl();
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";
    }
}
