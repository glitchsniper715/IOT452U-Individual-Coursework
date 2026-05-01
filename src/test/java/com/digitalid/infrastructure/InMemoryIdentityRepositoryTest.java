package com.digitalid.infrastructure;

import com.digitalid.domain.DigitalID;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.IDNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryIdentityRepositoryTest {

    // Declared as the INTERFACE type to test the contract, not the implementation
    private IdentityRepository repository;
    private DigitalID testDigitalID;
    private static final String TEST_ID_NUMBER = "DIG-REPO-001";

    @BeforeEach
    void setUp() {
        // Fresh repository before every test so tests do not interfere with each other
        repository = new InMemoryIdentityRepository();

        testDigitalID = new DigitalID(
                TEST_ID_NUMBER,
                "Fake Person",
                LocalDate.of(1990, 5, 15),
                "London",
                "10 Test Street, London",
                "British"
        );
    }

    @Test
    void save_thenFindById_returnsTheSameDigitalID() {
        repository.save(testDigitalID);

        DigitalID retrieved = repository.findById(TEST_ID_NUMBER);

        // Same object reference - the HashMap returns exactly what was stored
        assertSame(testDigitalID, retrieved);
    }

    @Test
    void save_withSameIdNumber_overwritesPreviousRecord() {
        repository.save(testDigitalID);

        DigitalID updatedID = new DigitalID(
                TEST_ID_NUMBER,
                "Fake Person-Updated",
                LocalDate.of(1990, 5, 15),
                "London",
                "20 New Street, London",
                "British"
        );

        repository.save(updatedID);

        DigitalID retrieved = repository.findById(TEST_ID_NUMBER);
        assertEquals("Fake Person-Updated", retrieved.getFullName());
    }

    @Test
    void findById_throwsIDNotFoundException_forUnknownIdNumber() {
        // Repository is empty - nothing has been saved
        assertThrows(IDNotFoundException.class,
                () -> repository.findById("UNKNOWN-999"));
    }

    @Test
    void findById_exceptionMessage_containsTheMissingIdNumber() {
        String missingId = "DIG-MISSING-123";

        IDNotFoundException thrown = assertThrows(IDNotFoundException.class,
                () -> repository.findById(missingId));

        assertTrue(thrown.getMessage().contains(missingId),
                "Exception message should identify the missing ID number");
    }

    @Test
    void exists_returnsTrue_afterSaving() {
        repository.save(testDigitalID);

        assertTrue(repository.exists(TEST_ID_NUMBER));
    }

    @Test
    void exists_returnsFalse_forUnknownIdNumber() {
        // Nothing saved yet
        assertFalse(repository.exists("DIG-NEVER-SAVED"));
    }

    @Test
    void exists_returnsFalse_onFreshRepository() {
        assertFalse(repository.exists(TEST_ID_NUMBER));
    }
}
