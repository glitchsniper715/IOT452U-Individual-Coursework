package com.digitalid.service.management;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.AuditEntry;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.UnauthorisedActionException;
import com.digitalid.exception.ImmutableFieldException;
import com.digitalid.exception.ValidationException;
import com.digitalid.infrastructure.InMemoryAuditRepository;
import com.digitalid.infrastructure.InMemoryIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IdentityManagerTest {
    private static final OrganisationType ORG_AUTH = OrganisationType.CENTRAL_AUTHORITY;

    private IdentityManager manager;
    private InMemoryIdentityRepository repository;
    private InMemoryAuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        repository      = new InMemoryIdentityRepository();
        auditRepository = new InMemoryAuditRepository();
        manager         = new IdentityManager(repository, new AuthorisationService(), auditRepository);

    }

    @Test
    void create_returnsNonNullIdNumber_whenAttributesAreValid() {
        String idNumber = manager.create(validAttributes(), ORG_AUTH);

        assertNotNull(idNumber, "idNumber must not be null");
        assertFalse(idNumber.isBlank(), "idNumber must not be blank");
    }

    @Test
    void create_newDigitalID_hasActiveStatusByDefault() {
        String idNumber = manager.create(validAttributes(), ORG_AUTH);

        IDStatus status = repository.findById(idNumber).getStatus();
        assertEquals(IDStatus.ACTIVE, status,
                "Newly created DigitalID must start with ACTIVE status");
    }

    @Test
    void create_throwsUnauthorisedActionException_whenCalledByBank() {
        assertThrows(UnauthorisedActionException.class,
                () -> manager.create(validAttributes(), OrganisationType.BANK),
                "BANK must not be permitted to call create()");
    }

    @Test
    void create_throwsValidationException_whenFullNameIsMissing() {
        Map<String, Object> attrs = validAttributes();
        attrs.remove("fullName");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> manager.create(attrs, ORG_AUTH));

        assertTrue(ex.getMessage().contains("fullName"),
                "Exception message must name the missing field");
    }

    private Map<String, Object> validAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("fullName",     "Jane Smith");
        attrs.put("dateOfBirth",  LocalDate.of(1990, 5, 15));
        attrs.put("placeOfBirth", "London");
        attrs.put("nationality",  "British");
        return attrs;
    }

    @Test
    void updateAttributes_succeedsAndPersistsChange_whenIDIsActive() {
        String idNumber = createTestID();

        manager.updateAttributes(idNumber, Map.of("fullName", "Jane Smith-Updated"), ORG_AUTH);

        String savedName = repository.findById(idNumber).getFullName();
        assertEquals("Jane Smith-Updated", savedName);
    }

    @Test
    void updateAttributes_throwsValidationException_forRevokedID() {
        String idNumber = createTestID();
        manager.changeStatus(idNumber, IDStatus.REVOKED, ORG_AUTH);

        assertThrows(ValidationException.class,
                () -> manager.updateAttributes(idNumber,
                        Map.of("fullName", "New Name"), ORG_AUTH),
                "Updating a REVOKED identity must throw ValidationException");
    }

    @Test
    void updateAttributes_throwsImmutableFieldException_whenImmutableFieldTargeted() {
        String idNumber = createTestID();

        assertThrows(ImmutableFieldException.class,
                () -> manager.updateAttributes(idNumber,
                        Map.of("placeOfBirth", "Paris"), ORG_AUTH),
                "Updating an immutable field must throw ImmutableFieldException");
    }

    @Test
    void changeStatus_fromActiveToSuspended_succeedsAndPersistsNewStatus() {
        String idNumber = createTestID();

        manager.changeStatus(idNumber, IDStatus.SUSPENDED, ORG_AUTH);

        IDStatus newStatus = repository.findById(idNumber).getStatus();
        assertEquals(IDStatus.SUSPENDED, newStatus);
    }

    @Test
    void changeStatus_addAuditEntry_afterSuccessfulTransition() {
        String idNumber = createTestID();

        manager.changeStatus(idNumber, IDStatus.SUSPENDED, ORG_AUTH);

        boolean hasStatusEntry = repository.findById(idNumber)
                .getAuditLog()
                .stream()
                .anyMatch(e -> e.action().contains("STATUS_CHANGE"));

        assertTrue(hasStatusEntry,
                "An audit entry recording the status change must be present");
    }

    @Test
    void create_doesNotThrow_whenAllAttributesPresent() {
        assertDoesNotThrow(() -> manager.create(validAttributes(), ORG_AUTH));
    }

    @Test
    void changeStatus_writesEntryToAuditRepository_afterSuccessfulTransition() {
        String idNumber = createTestID();

        manager.changeStatus(idNumber, IDStatus.SUSPENDED, ORG_AUTH);

        List<AuditEntry> entries = auditRepository.findByIdNumber(idNumber);
        boolean hasStatusEntry = entries.stream()
                .anyMatch(e -> e.action().equals("STATUS_CHANGE"));

        assertTrue(hasStatusEntry,
                "AuditRepository must contain a STATUS_CHANGE entry after changeStatus()");
    }

    private String createTestID() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("fullName",     "Jane Smith");
        attrs.put("dateOfBirth",  LocalDate.of(1990, 5, 15));
        attrs.put("placeOfBirth", "London");
        return manager.create(attrs, ORG_AUTH);
    }
}
