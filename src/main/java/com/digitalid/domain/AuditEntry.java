package com.digitalid.domain;

import java.time.LocalDateTime;


/** immutable record of a single system action. Using a Java Record ensures entries cannot be modified after creation */

public record AuditEntry(LocalDateTime timestamp,
                         String action,
                         String performedBy,
                         String details) {
}
