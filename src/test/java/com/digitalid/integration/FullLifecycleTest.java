package com.digitalid.integration;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.UnauthorisedActionException;
import com.digitalid.infrastructure.InMemoryAuditRepository;
import com.digitalid.infrastructure.InMemoryIdentityRepository;
import com.digitalid.service.consumption.*;
import com.digitalid.service.management.IdentityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Full end-to-end integration tests. */
class FullLifecycleTest {

    private static final OrganisationType ORG_AUTH = OrganisationType.CENTRAL_AUTHORITY;

    private IdentityManager manager;

    private BankPortal bankPortal;
    private EmployerPortal employerPortal;
    private TaxAuthorityPortal taxPortal;
    private DrivingLicencePortal drivingPortal;

    @BeforeEach
    void setUp() {
        InMemoryIdentityRepository repository = new InMemoryIdentityRepository();
        InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
        AuthorisationService authService = new AuthorisationService();

        manager = new IdentityManager(repository, authService, auditRepository);
        VerificationService verificationService =
                new VerificationService(repository, authService, auditRepository);

        bankPortal = new BankPortal(verificationService);
        employerPortal = new EmployerPortal(verificationService);
        taxPortal = new TaxAuthorityPortal(verificationService);
        drivingPortal = new DrivingLicencePortal(verificationService);
    }

    /**
     *  create → suspend → bank verification returns INVALID → reactivate → bank verification returns VALID.
     * Verifies that BankPortal correctly reflects status changes made by IdentityManager.
     */
    @Test
    void fullLifecycle_suspendedID_returnsINVALIDFromBankPortal_thenVALIDAfterReactivation() {
        String id = manager.create(testAttributes(), ORG_AUTH);
        assertEquals("VALID", bankPortal.verifyIdentity(id).status());

        manager.changeStatus(id, IDStatus.SUSPENDED, ORG_AUTH);
        VerificationResult suspended = bankPortal.verifyIdentity(id);
        assertEquals("INVALID", suspended.status());

        manager.changeStatus(id, IDStatus.ACTIVE, ORG_AUTH);
        assertEquals("VALID", bankPortal.verifyIdentity(id).status());
    }

    /**
     * ID active → period check VALID → suspend → period check covering suspension → INVALID.
     * Demonstrates that TaxAuthorityPortal detects a suspension that occurred
     * during the reporting period even if the identity is currently ACTIVE again.
     */
    @Test
    void fullLifecycle_taxPeriodCheck_returnsINVALID_whenIDWasSuspendedDuringPeriod() {
        String id = manager.create(testAttributes(), ORG_AUTH);

        LocalDateTime beforeSuspension = LocalDateTime.now().minusMinutes(1);
        LocalDateTime afterCheck = LocalDateTime.now().plusHours(1);

        VerificationResult beforeResult =
                taxPortal.verifyForPeriod(id, beforeSuspension, afterCheck);
        assertEquals("VALID", beforeResult.status());

        manager.changeStatus(id, IDStatus.SUSPENDED, ORG_AUTH);
        manager.changeStatus(id, IDStatus.ACTIVE,    ORG_AUTH);

        LocalDateTime periodStart = LocalDateTime.now().minusHours(2);
        LocalDateTime periodEnd = LocalDateTime.now().plusHours(1);

        VerificationResult afterResult =
                taxPortal.verifyForPeriod(id, periodStart, periodEnd);
        assertEquals("INVALID", afterResult.status());
        assertTrue(afterResult.reason().toLowerCase().contains("suspended"));
    }

    /**
     * Authorisation boundary: a BANK caller must never be able to invoke
     * management operations, even if it tries to call IdentityManager directly.

     * Verifies the strict separation between management and consumption capabilities required by the brief.
     */
    @Test
    void crossCapability_bankCannotCallManagementCreate() {
        assertThrows(UnauthorisedActionException.class, () ->
                        manager.create(testAttributes(), OrganisationType.BANK),
                "BANK must never be permitted to call IdentityManager.create()");
    }

    /**
     * Driving licence eligibility: active ID with restriction → INELIGIBLE → restriction cleared → VALID.
     * Confirms that DrivingLicencePortal reflects restriction changes made by IdentityManager.
     */
    @Test
    void fullLifecycle_drivingLicence_returnsINELIGIBLE_thenVALIDAfterRestrictionCleared() {
        String id = manager.create(testAttributes(), ORG_AUTH);
        assertEquals("VALID", drivingPortal.verifyForLicence(id).status());

        manager.setRestriction(id, true, ORG_AUTH);
        assertEquals("INELIGIBLE", drivingPortal.verifyForLicence(id).status());

        manager.setRestriction(id, false, ORG_AUTH);
        assertEquals("VALID", drivingPortal.verifyForLicence(id).status());
    }

    /**
     * Employer portal basic verification mirrors bank portal behaviour. Active/inactive check only.
     */
    @Test
    void fullLifecycle_employerPortal_returnsINVALID_whenIDIsRevoked() {
        String id = manager.create(testAttributes(), ORG_AUTH);

        manager.changeStatus(id, IDStatus.REVOKED, ORG_AUTH);

        VerificationResult result = employerPortal.verifyIdentity(id);
        assertEquals("INVALID", result.status());
    }

    private Map<String, Object> testAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("fullName", "Fake Name");
        attrs.put("dateOfBirth", LocalDate.of(1990, 5, 15));
        attrs.put("placeOfBirth", "London");
        attrs.put("nationality", "British");
        return attrs;
    }
}
