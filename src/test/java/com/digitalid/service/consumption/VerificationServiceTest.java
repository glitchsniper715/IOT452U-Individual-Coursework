package com.digitalid.service.consumption;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.IDStatus;
import com.digitalid.infrastructure.InMemoryAuditRepository;
import com.digitalid.infrastructure.InMemoryIdentityRepository;
import com.digitalid.service.management.IdentityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VerificationServiceTest {

    private static final OrganisationType ORG_AUTH = OrganisationType.CENTRAL_AUTHORITY;
    private static final OrganisationType ORG_BANK = OrganisationType.BANK;

    private IdentityManager manager;
    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
        AuthorisationService authService = new AuthorisationService();

        manager = new IdentityManager(repository, authService, auditRepository);
        verificationService = new VerificationService(repository, authService, auditRepository);
    }

    @Test
    void verifyBasic_returnsVALID_whenIDIsActive() {
        String id = createTestID();
        VerificationResult result = verificationService.verifyBasic(id, ORG_BANK);
        assertEquals("VALID", result.status());
    }

    @Test
    void verifyBasic_returnsINVALID_whenIDIsSuspended() {
        String id = createTestID();
        manager.changeStatus(id, IDStatus.SUSPENDED, ORG_AUTH);

        VerificationResult result = verificationService.verifyBasic(id, ORG_BANK);
        assertEquals("INVALID", result.status());
        assertTrue(result.reason().contains("SUSPENDED"));
    }

    @Test
    void verifyBasic_returnsINVALID_whenIDIsRevoked() {
        String id = createTestID();
        manager.changeStatus(id, IDStatus.REVOKED, ORG_AUTH);

        VerificationResult result = verificationService.verifyBasic(id, ORG_BANK);
        assertEquals("INVALID", result.status());
        assertTrue(result.reason().contains("REVOKED"));
    }

    @Test
    void verifyBasic_returnsNOT_FOUND_whenIDDoesNotExist() {
        VerificationResult result = verificationService.verifyBasic("unknown-id-999", ORG_BANK);
        assertEquals("NOT_FOUND", result.status());
    }

    private String createTestID() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("fullName", "Fake Person");
        attrs.put("dateOfBirth", LocalDate.of(1990, 5, 15));
        attrs.put("placeOfBirth", "London");
        return manager.create(attrs, ORG_AUTH);
    }
}
