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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TaxAuthorityPortalTest {

    private static final OrganisationType ORG_AUTH = OrganisationType.CENTRAL_AUTHORITY;

    private IdentityManager manager;
    private TaxAuthorityPortal taxPortal;

    @BeforeEach
    void setUp() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
        AuthorisationService authService = new AuthorisationService();

        manager = new IdentityManager(repository, authService, auditRepository);
        VerificationService verificationService =
                new VerificationService(repository, authService, auditRepository);
        taxPortal = new TaxAuthorityPortal(verificationService);
    }

    @Test
    void verifyForPeriod_returnsVALID_whenIDWasAlwaysActiveDuringPeriod() {
        String id = createTestID();

        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now().plusHours(1);

        VerificationResult result = taxPortal.verifyForPeriod(id, from, to);
        assertEquals("VALID", result.status());
    }

    @Test
    void verifyForPeriod_returnsINVALID_whenIDWasSuspendedDuringPeriod() {
        String id = createTestID();

        manager.changeStatus(id, IDStatus.SUSPENDED, ORG_AUTH);
        manager.changeStatus(id, IDStatus.ACTIVE, ORG_AUTH);

        LocalDateTime from = LocalDateTime.now().minusHours(2);
        LocalDateTime to = LocalDateTime.now().plusHours(1);

        VerificationResult result = taxPortal.verifyForPeriod(id, from, to);
        assertEquals("INVALID", result.status());
        assertTrue(result.reason().toLowerCase().contains("suspended"),
                "Reason should mention suspension");
    }

    @Test
    void verifyForPeriod_returnsNOT_FOUND_whenIDDoesNotExist() {
        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now().plusHours(1);

        VerificationResult result = taxPortal.verifyForPeriod("unknown-id-999", from, to);
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
