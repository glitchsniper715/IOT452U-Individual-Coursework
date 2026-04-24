package com.digitalid.service.management;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.UnauthorisedActionException;
import com.digitalid.exception.ValidationException;
import com.digitalid.infrastructure.InMemoryIdentityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IdentityManager — Day 15 coverage.
 *
 * <p>Each test is wired with real collaborators (InMemoryIdentityRepository
 * and AuthorisationService) rather than mocks, keeping the setup minimal
 * and the tests readable.</p>
 */
class IdentityManagerTest {

    private IdentityManager manager;
    private InMemoryIdentityRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIdentityRepository();
        manager    = new IdentityManager(repository, new AuthorisationService());
    }

    // ── create() — happy path ──────────────────────────────────────────────

    /**
     * Verifies that a successful create() call returns a non-null, non-blank
     * idNumber, and that the persisted DigitalID starts with ACTIVE status.
     */
    @Test
    void create_returnsNonNullIdNumber_whenAttributesAreValid() {
        String idNumber = manager.create(validAttributes(),
                OrganisationType.CENTRAL_AUTHORITY);

        assertNotNull(idNumber, "idNumber must not be null");
        assertFalse(idNumber.isBlank(), "idNumber must not be blank");
    }

    /**
     * Verifies that the newly created DigitalID has ACTIVE status immediately
     * after creation — as required by the brief.
     */
    @Test
    void create_newDigitalID_hasActiveStatusByDefault() {
        String idNumber = manager.create(validAttributes(),
                OrganisationType.CENTRAL_AUTHORITY);

        IDStatus status = repository.findById(idNumber).getStatus();
        assertEquals(IDStatus.ACTIVE, status,
                "Newly created DigitalID must start with ACTIVE status");
    }

    // ── create() — authorisation failure ──────────────────────────────────

    /**
     * Verifies that a BANK caller is rejected with UnauthorisedActionException
     * before any processing occurs — authorisation is the very first step.
     */
    @Test
    void create_throwsUnauthorisedActionException_whenCalledByBank() {
        assertThrows(UnauthorisedActionException.class,
                () -> manager.create(validAttributes(), OrganisationType.BANK),
                "BANK must not be permitted to call create()");
    }

    // ── create() — validation failures ────────────────────────────────────

    /**
     * Verifies that a missing fullName causes a ValidationException whose
     * message identifies the offending field.
     */
    @Test
    void create_throwsValidationException_whenFullNameIsMissing() {
        Map<String, Object> attrs = validAttributes();
        attrs.remove("fullName");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> manager.create(attrs, OrganisationType.CENTRAL_AUTHORITY));

        assertTrue(ex.getMessage().contains("fullName"),
                "Exception message must name the missing field");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Returns a fully-populated valid attribute map for convenience. */
    private Map<String, Object> validAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("fullName",     "Jane Smith");
        attrs.put("dateOfBirth",  LocalDate.of(1990, 5, 15));
        attrs.put("placeOfBirth", "London");
        attrs.put("nationality",  "British");
        return attrs;
    }
}
