package com.digitalid.infrastructure;

import com.digitalid.domain.AuditEntry;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditRepository {

    void log(String idNumber, AuditEntry entry);

    List<AuditEntry> findByIdNumber(String idNumber);

    List<AuditEntry> findByIdNumberAndDateRange(String idNumber, LocalDateTime from, LocalDateTime to);
}