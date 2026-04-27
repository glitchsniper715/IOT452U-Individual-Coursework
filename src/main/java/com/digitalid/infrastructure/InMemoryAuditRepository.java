package com.digitalid.infrastructure;

import com.digitalid.domain.AuditEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryAuditRepository implements AuditRepository {

    private final Map<String, List<AuditEntry>> entries = new HashMap<>();

    @Override
    public void log(String idNumber, AuditEntry entry) {
        entries.computeIfAbsent(idNumber, key -> new ArrayList<>()).add(entry);
    }

    @Override
    public List<AuditEntry> findByIdNumber(String idNumber) {
        return Collections.unmodifiableList(
                entries.getOrDefault(idNumber, Collections.emptyList())
        );
    }

    @Override
    public List<AuditEntry> findByIdNumberAndDateRange(String idNumber,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        return entries.getOrDefault(idNumber, Collections.emptyList())
                .stream()
                .filter(entry -> isWithinRange(entry.timestamp(), from, to))
                .collect(Collectors.toUnmodifiableList());
    }

    private boolean isWithinRange(LocalDateTime timestamp,
                                  LocalDateTime from,
                                  LocalDateTime to) {
        return !timestamp.isBefore(from) && !timestamp.isAfter(to);
    }
}