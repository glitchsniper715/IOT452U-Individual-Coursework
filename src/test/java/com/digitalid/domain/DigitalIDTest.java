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

    @Test
    void constructor_storesIdNumber_correctly() {
        assertEquals(TEST_ID_NUMBER, digitalID.getIdNumber());
    }

    @Test
    void constructor_storesFullName_correctly() {
        assertEquals(TEST_FULL_NAME, digitalID.getFullName());
    }

    @Test
    void constructor_storesDateOfBirth_correctly() {
        assertEquals(TEST_DOB, digitalID.getDateOfBirth());
    }

    @Test
    void constructor_storesPlaceOfBirth_correctly() {
        assertEquals(TEST_PLACE_OF_BIRTH, digitalID.getPlaceOfBirth());
    }

    @Test
    void constructor_storesAddress_correctly() {
        assertEquals(TEST_ADDRESS, digitalID.getAddress());
    }

    @Test
    void constructor_storesNationality_correctly() {
        assertEquals(TEST_NATIONALITY, digitalID.getNationality());
    }

    @Test
    void constructor_setsStatus_toActiveByDefault() {
        assertEquals(IDStatus.ACTIVE, digitalID.getStatus());
    }


    @Test
    void constructor_setsTemporaryRestriction_toFalseByDefault() {
        assertFalse(digitalID.isTemporaryRestriction());
    }

    @Test
    void constructor_createsEmptyAuditLog() {
        assertTrue(digitalID.getAuditLog().isEmpty());
    }

    @Test
    void getAuditLog_returnsUnmodifiableList_soCallerCannotAddEntries() {
        assertThrows(UnsupportedOperationException.class, () ->
                digitalID.getAuditLog().add(
                        new AuditEntry(LocalDateTime.now(), "FAKE", "ATTACKER", "Should fail")
                )
        );
    }

    @Test
    void transitionStatus_activeToSuspended_succeeds() {
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY));
        assertEquals(IDStatus.SUSPENDED, digitalID.getStatus());
    }

    @Test
    void transitionStatus_activeToRevoked_succeeds() {
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY));
        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    @Test
    void transitionStatus_suspendedToActive_succeeds() {
        digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY);
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY));
        assertEquals(IDStatus.ACTIVE, digitalID.getStatus());
    }

    @Test
    void transitionStatus_suspendedToRevoked_succeeds() {
        digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY);
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY));
        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    @Test
    void transitionStatus_revokedToActive_throwsInvalidTransitionException() {
        digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY);

        assertThrows(InvalidTransitionException.class,
                () -> digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY));

        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    @Test
    void transitionStatus_revokedToSuspended_throwsInvalidTransitionException() {
        digitalID.transitionStatus(IDStatus.REVOKED, PERFORMED_BY);

        assertThrows(InvalidTransitionException.class,
                () -> digitalID.transitionStatus(IDStatus.SUSPENDED, PERFORMED_BY));

        assertEquals(IDStatus.REVOKED, digitalID.getStatus());
    }

    @Test
    void transitionStatus_sameStatus_isIdempotentAndCreatesNoAuditEntry() {
        assertDoesNotThrow(() -> digitalID.transitionStatus(IDStatus.ACTIVE, PERFORMED_BY));

        assertEquals(IDStatus.ACTIVE, digitalID.getStatus());
        assertTrue(digitalID.getAuditLog().isEmpty(),
                "No audit entry should be created when status does not change");
    }

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
