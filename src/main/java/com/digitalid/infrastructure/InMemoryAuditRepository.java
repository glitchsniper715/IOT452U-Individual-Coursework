package com.digitalid.infrastructure;

import com.digitalid.domain.AuditEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** In-memory implementation of AuditRepository

 * Stores all audit entries in a HashMap where the key is the Digital ID number
 * */
public class InMemoryAuditRepository implements AuditRepository {

    /** Using ArrayList preserves insertion order, which equals chronological order*/
    private final Map<String, List<AuditEntry>> entries = new HashMap<>();

    /**
     * If this is the first entry for the given ID number, a new list is
     * created automatically. Entries are appended in the order they arrive,
     * giving natural chronological order.
     */
    @Override
    public void log(String idNumber, AuditEntry entry) {
        entries.computeIfAbsent(idNumber, key -> new ArrayList<>()).add(entry);
    }

    /** Returns an unmodifiable view so callers cannot alter the internal list. */
    @Override
    public List<AuditEntry> findByIdNumber(String idNumber) {
        return Collections.unmodifiableList(
                entries.getOrDefault(idNumber, Collections.emptyList())
        );
    }

    /** Filters entries using Java Streams. */
    @Override
    public List<AuditEntry> findByIdNumberAndDateRange(String idNumber,
                                                       LocalDateTime from,
                                                       LocalDateTime to) {
        return entries.getOrDefault(idNumber, Collections.emptyList())
                .stream()
                .filter(entry -> isWithinRange(entry.timestamp(), from, to))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns true when {@code timestamp} falls in the inclusive range [from, to].

     * Using {@code !isBefore(from)} is equivalent to {@code >= from},
     * and {@code !isAfter(to)} is equivalent to {@code <= to}.
     */
    private boolean isWithinRange(LocalDateTime timestamp,
                                  LocalDateTime from,
                                  LocalDateTime to) {
        return !timestamp.isBefore(from) && !timestamp.isAfter(to);
    }
}