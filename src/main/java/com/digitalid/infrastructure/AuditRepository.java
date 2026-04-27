package com.digitalid.infrastructure;

import com.digitalid.domain.AuditEntry;

import java.time.LocalDateTime;
import java.util.List;

/** Defines the storage contract for audit entry records. */

public interface AuditRepository {

    /** Stores a single audit entry. */
    void log(String idNumber, AuditEntry entry);

    /** Returns all audit entries recorded for the given ID number. */
    List<AuditEntry> findByIdNumber(String idNumber);

    /** Returns audit entries for the given ID number whose timestamp falls within the inclusive range */
    List<AuditEntry> findByIdNumberAndDateRange(String idNumber, LocalDateTime from, LocalDateTime to);
}