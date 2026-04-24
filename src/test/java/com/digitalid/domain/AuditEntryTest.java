package com.digitalid.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditEntryTest {
    @Test
    void allFieldsStoredCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        String action = "CREATE";
        String performedBy = "AUTHORITY";
        String details = "Created ID-001";

        AuditEntry e = new AuditEntry(now, action, performedBy, details);

        assertEquals(now, e.timestamp());
        assertEquals(action, e.action());
        assertEquals(performedBy, e.performedBy());
        assertEquals(details, e.details());
    }

    @Test
    void identicalEntriesAreEqual() {
        LocalDateTime now = LocalDateTime.now();

        AuditEntry e1 = new AuditEntry(now, "CREATE", "AUTHORITY", "ID-001");
        AuditEntry e2 = new AuditEntry(now, "CREATE", "AUTHORITY", "ID-001");

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}