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

class DrivingLicencePortalTest {

    private static final OrganisationType ORG_AUTH = OrganisationType.CENTRAL_AUTHORITY;

    private IdentityManager manager;
    private DrivingLicencePortal drivingPortal;

    @BeforeEach
    void setUp() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
        AuthorisationService authService = new AuthorisationService();

        manager = new IdentityManager(repository, authService, auditRepository);
        VerificationService verificationService =
                new VerificationService(repository, authService, auditRepository);
        drivingPortal = new DrivingLicencePortal(verificationService);
    }

    @Test
    void verifyForLicence_returnsVALID_whenIDIsActiveWithNoRestriction() {
        String id = createTestID();

        VerificationResult result = drivingPortal.verifyForLicence(id);
        assertEquals("VALID", result.status());
    }

    @Test
    void verifyForLicence_returnsINELIGIBLE_whenTemporaryRestrictionIsSet() {
        String id = createTestID();
        manager.setRestriction(id, true, ORG_AUTH);

        VerificationResult result = drivingPortal.verifyForLicence(id);
        assertEquals("INELIGIBLE", result.status());
        assertTrue(result.reason().contains("restriction"));
    }

    @Test
    void verifyForLicence_returnsINVALID_whenIDIsSuspended() {
        String id = createTestID();
        manager.changeStatus(id, IDStatus.SUSPENDED, ORG_AUTH);

        VerificationResult result = drivingPortal.verifyForLicence(id);
        assertEquals("INVALID", result.status());
    }

    @Test
    void verifyForLicence_returnsNOT_FOUND_whenIDDoesNotExist() {
        VerificationResult result = drivingPortal.verifyForLicence("NONEXISTENT-ID");
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