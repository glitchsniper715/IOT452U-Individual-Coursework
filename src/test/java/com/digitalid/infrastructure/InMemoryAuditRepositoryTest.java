package com.digitalid.infrastructure;

import com.digitalid.domain.AuditEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAuditRepositoryTest {

    private static final String ID_NUMBER = "DIG-TEST-001";

    private InMemoryAuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        auditRepository = new InMemoryAuditRepository();
    }

    @Test
    void log_andFindByIdNumber_returnsStoredEntry() {

        AuditEntry entry = new AuditEntry(
                LocalDateTime.now(), "IDENTITY_CREATED", "CENTRAL_AUTHORITY",
                "Identity created for: Jane Smith"
        );

        auditRepository.log(ID_NUMBER, entry);
        List<AuditEntry> results = auditRepository.findByIdNumber(ID_NUMBER);

        assertEquals(1, results.size());
        assertEquals("IDENTITY_CREATED", results.get(0).action());
    }

    @Test
    void findByIdNumber_returnsEmptyList_forUnknownIdNumber() {
        List<AuditEntry> results = auditRepository.findByIdNumber("UNKNOWN-999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findByIdNumberAndDateRange_returnsOnlyEntriesWithinRange() {
        LocalDateTime now = LocalDateTime.now();

        AuditEntry before = new AuditEntry(now.minusHours(5), "STATUS_CHANGED",
                "CENTRAL_AUTHORITY", "Status changed to: SUSPENDED");
        AuditEntry inside = new AuditEntry(now, "STATUS_CHANGED",
                "CENTRAL_AUTHORITY", "Status changed to: ACTIVE");
        AuditEntry after  = new AuditEntry(now.plusHours(5), "STATUS_CHANGED",
                "CENTRAL_AUTHORITY", "Status changed to: SUSPENDED");

        auditRepository.log(ID_NUMBER, before);
        auditRepository.log(ID_NUMBER, inside);
        auditRepository.log(ID_NUMBER, after);

        LocalDateTime from = now.minusHours(1);
        LocalDateTime to = now.plusHours(1);

        List<AuditEntry> results =
                auditRepository.findByIdNumberAndDateRange(ID_NUMBER, from, to);

        assertEquals(1, results.size(),
                "Only the entry timestamped within [from, to] should be returned");
        assertEquals("Status changed to: ACTIVE", results.get(0).details());
    }

    @Test
    void findByIdNumberAndDateRange_returnsEmpty_whenNoEntriesInRange() {
        AuditEntry entry = new AuditEntry(
                LocalDateTime.now().minusDays(10), "IDENTITY_CREATED",
                "CENTRAL_AUTHORITY", "Identity created"
        );
        auditRepository.log(ID_NUMBER, entry);

        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now().plusHours(1);

        List<AuditEntry> results =
                auditRepository.findByIdNumberAndDateRange(ID_NUMBER, from, to);

        assertTrue(results.isEmpty(),
                "No entries should match when the only entry is outside the range");
    }
}