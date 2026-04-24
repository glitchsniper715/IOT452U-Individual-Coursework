package com.digitalid.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IDStatusTest {

    @Test
    void enumContainsThreeValues() {
        assertEquals(3, IDStatus.values().length);
    }

    @Test
    void activeAndRevokedAreDistinct() {
        assertNotEquals(IDStatus.ACTIVE, IDStatus.REVOKED);
    }
}