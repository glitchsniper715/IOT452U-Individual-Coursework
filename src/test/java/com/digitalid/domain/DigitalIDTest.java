package com.digitalid.domain;

import com.digitalid.exception.InvalidTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DigitalIDTest {

    private static final String   TEST_ID_NUMBER      = "DIG-TEST-001";
    private static final String   TEST_FULL_NAME      = "Test Person";
    private static final LocalDate TEST_DOB            = LocalDate.of(1990, 5, 15);
    private static final String   TEST_PLACE_OF_BIRTH = "London";
    private static final String   TEST_ADDRESS        = "10 Test Street, London";
    private static final String   TEST_NATIONALITY    = "British";
    private static final String   PERFORMED_BY        = "CENTRAL_AUTHORITY";

    private DigitalID digitalID;
    @BeforeEach
    void setUp() {
        digitalID = new DigitalID(
                TEST_ID_NUMBER,
                TEST_FULL_NAME,
                TEST_DOB,
                TEST_PLACE_OF_BIRTH,
                TEST_ADDRESS,
                TEST_NATIONALITY
        );
    }

    /** The idNumber passed in must be stored and returned unchanged */
    @Test
    void constructor_storesIdNumber_correctly() {
        assertEquals(TEST_ID_NUMBER, digitalID.getIdNumber());
    }

    /** fullName is a mutable field but must be stored correctly at creation */
    @Test
    void constructor_storesFullName_correctly() {
        assertEquals(TEST_FULL_NAME, digitalID.getFullName());
    }

    /** dateOfBirth is an immutable field - it must be stored exactly as passed */
    @Test
    void constructor_storesDateOfBirth_correctly() {
        assertEquals(TEST_DOB, digitalID.getDateOfBirth());
    }

    /** placeOfBirth is an immutable field - it must be stored exactly as passed */
    @Test
    void constructor_storesPlaceOfBirth_correctly() {
        assertEquals(TEST_PLACE_OF_BIRTH, digitalID.getPlaceOfBirth());
    }

    /** address is a mutable field but must be stored correctly at creation */
    @Test
    void constructor_storesAddress_correctly() {
        assertEquals(TEST_ADDRESS, digitalID.getAddress());
    }

    /** nationality is a mutable field but must be stored correctly at creation */
    @Test
    void constructor_storesNationality_correctly() {
        assertEquals(TEST_NATIONALITY, digitalID.getNationality());
    }

    /**
     * Every new Digital ID must start as ACTIVE.
     */
    @Test
    void constructor_setsStatus_toActiveByDefault() {
        assertEquals(IDStatus.ACTIVE, digitalID.getStatus());
    }

    /**
     * No temporary restriction should be applied on creation.
     */
    @Test
    void constructor_setsTemporaryRestriction_toFalseByDefault() {
        assertFalse(digitalID.isTemporaryRestriction());
    }

    /** A brand new Digital ID has an empty audit log */
    @Test
    void constructor_createsEmptyAuditLog() {
        assertTrue(digitalID.getAuditLog().isEmpty());
    }

    /** getAuditLog() returns an unmodifiable list. Callers must not be able to add entries directly and bypass the audit trail */
    @Test
    void getAuditLog_returnsUnmodifiableList_soCallerCannotAddEntries() {
        assertThrows(UnsupportedOperationException.class, () ->
                digitalID.getAuditLog().add(
                        new AuditEntry(LocalDateTime.now(), "FAKE", "ATTACKER", "Should fail")
                )
        );
    }

    /** ACTIVE to SUSPENDED is a valid transition */
    @Test
    void transitionStatus_activeToSuspended_succeeds() {
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY));
        assertEquals(IDStatus.SUSPENDED, digitalID.getStatus());
    }

    /** ACTIVE to REVOKED is a valid transition */
    @Test
    void transitionStatus_activeToRevoked_succeeds() {
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY));
        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    /** SUSPENDED to ACTIVE is a valid transition */
    @Test
    void transitionStatus_suspendedToActive_succeeds() {
        digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY);
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY));
        assertEquals(IDStatus.ACTIVE, digitalID.getStatus());
    }

    /** SUSPENDED to REVOKED is a valid transition */
    @Test
    void transitionStatus_suspendedToRevoked_succeeds() {
        digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY);
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY));
        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    /** REVOKED to ACTIVE must be rejected */
    @Test
    void transitionStatus_revokedToActive_throwsInvalidTransitionException() {
        digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY);

        assertThrows(InvalidTransitionException.class,
                () -> digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY));

        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    /** REVOKED to SUSPENDED must be rejected */
    @Test
    void transitionStatus_revokedToSuspended_throwsInvalidTransitionException() {
        digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY);

        assertThrows(InvalidTransitionException.class,
                () -> digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY));

        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    /** Transitioning to the same status does nothing */
    @Test
    void transitionStatus_sameStatus_isIdempotentAndCreatesNoAuditEntry() {
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY));

        assertEquals(IDStatus.ACTIVE, digitalID.getStatus());
        assertTrue(digitalID.getAuditLog().isEmpty(),
                "No audit entry should be created when status does not change");
    }

    /** A valid transition must append exactly one AuditEntry to the log */
    @Test
    void transitionStatus_validTransition_appendsOneAuditEntry() {
        digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY);

        assertEquals(1, digitalID.getAuditLog().size());

        AuditEntry entry = digitalID.getAuditLog().get(0);
        assertEquals("STATUS_CHANGE", entry.action());
        assertEquals(PERFORMED_BY, entry.performedBy());
        assertNotNull(entry.timestamp());
        assertNotNull(entry.details());
    }

    /** Multiple transitions must each create their own audit entry */
    @Test
    void transitionStatus_multipleTransitions_buildsAuditLogInOrder() {
        digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY);
        digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY);
        digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY);

        assertEquals(3, digitalID.getAuditLog().size());

        digitalID.getAuditLog().forEach(entry ->
                assertEquals("STATUS_CHANGE", entry.action())
        );
    }

    /** An invalid transition must not change the status or add an audit entry */
    @Test
    void transitionStatus_invalidTransition_doesNotChangeStatusOrAuditLog() {
        digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY);

        try {
            digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY);
        } catch (InvalidTransitionException ignored) {
        }

        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
        assertEquals(1, digitalID.getAuditLog().size());
    }


    /** Tests all 6 possible transitions between the three statuses in one method */
    @ParameterizedTest(name = "{0} to {1} – should throw: {2}")
    @CsvSource({
            "ACTIVE,    SUSPENDED, false",
            "ACTIVE,    REVOKED,   false",
            "SUSPENDED, ACTIVE,    false",
            "SUSPENDED, REVOKED,   false",
            "REVOKED,   ACTIVE,    true",
            "REVOKED,   SUSPENDED, true"
    })
    void statusTransition_followsStateMachineRules(
            String fromStatusName,
            String toStatusName,
            boolean shouldThrow) {

        IDStatus fromStatus = IDStatus.valueOf(fromStatusName.trim());
        IDStatus toStatus   = IDStatus.valueOf(toStatusName.trim());

        DigitalID id = buildDigitalIDAtStatus(fromStatus);

        if (shouldThrow) {
            assertThrows(
                    InvalidTransitionException.class,
                    () -> id.transitionStatus(toStatus, PERFORMED_BY),
                    "Expected InvalidTransitionException for " + fromStatus + " -> " + toStatus
            );
        } else {
            assertDoesNotThrow(
                    () -> id.transitionStatus(toStatus, PERFORMED_BY),
                    "Did not expect exception for " + fromStatus + " -> " + toStatus
            );
            assertEquals(toStatus, id.getStatus(),
                    "Status should be " + toStatus + " after valid transition");
        }
    }

    /** Creates a DigitalID and moves it to the given status so that parameterised tests can start from any state without duplicating setup code */
    private DigitalID buildDigitalIDAtStatus(IDStatus target) {
        DigitalID id = new DigitalID(
                "DIG-PARAM-001",
                "Test Example",
                LocalDate.of(1985, 3, 20),
                "London",
                "123 Test Road, London",
                "British"
        );

        if (target == IDStatus.SUSPENDED) {
            id.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY);
        } else if (target == IDStatus.REVOKED) {
            id.transitionStatus(IDStatus.REVOKED, PERFORMED_BY);
        }
        return id;
    }
}
