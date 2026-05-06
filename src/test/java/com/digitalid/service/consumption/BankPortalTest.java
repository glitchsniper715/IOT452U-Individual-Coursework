package com.digitalid.service.consumption;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.*;
import com.digitalid.infrastructure.InMemoryAuditRepository;
import com.digitalid.infrastructure.InMemoryIdentityRepository;
import com.digitalid.service.management.IdentityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BankPortalTest {

    private static final OrganisationType ORG_AUTH = OrganisationType.CENTRAL_AUTHORITY;

    private IdentityManager manager;
    private BankPortal bankPortal;

    @BeforeEach
    void setUp() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
        AuthorisationService authService = new AuthorisationService();

        manager = new IdentityManager(repository, authService, auditRepository);
        VerificationService verificationService =
                new VerificationService(repository, authService, auditRepository);
        bankPortal = new BankPortal(verificationService);
    }

    @Test
    void verifyIdentity_returnsVALID_whenIDIsActive() {
        String id = createTestID();

        VerificationResult result = bankPortal.verifyIdentity(id);
        assertEquals("VALID", result.status());
    }

    @Test
    void verifyIdentity_returnsINVALID_whenIDIsRevoked() {
        String id = createTestID();
        manager.changeStatus(id, IDStatus.REVOKED, ORG_AUTH);

        VerificationResult result = bankPortal.verifyIdentity(id);

        assertEquals("INVALID", result.status());
    }

    @Test
    void verifyIdentity_returnsINVALID_whenIdIsEmpty() {
        VerificationResult result = bankPortal.verifyIdentity("");

        assertEquals("INVALID", result.status());
        assertTrue(result.reason().toLowerCase().contains("id"),
                "Reason should mention invalid ID");
    }

    @Test
    void verifyIdentity_returnsINVALID_whenIdIsNull() {
        VerificationResult result = bankPortal.verifyIdentity(null);

        assertEquals("INVALID", result.status());
        assertTrue(result.reason().toLowerCase().contains("id"),
                "Reason should mention invalid ID");
    }

    @Test
    void verifyIdentity_returnsNOT_FOUND_whenIdDoesNotExist() {
        VerificationResult result = bankPortal.verifyIdentity("non-existent-id");

        assertEquals("NOT_FOUND", result.status());
    }

    private String createTestID() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("fullName", "Fake Name");
        attrs.put("dateOfBirth", LocalDate.of(1990, 5, 15));
        attrs.put("placeOfBirth", "London");
        return manager.create(attrs, ORG_AUTH);
    }
}
