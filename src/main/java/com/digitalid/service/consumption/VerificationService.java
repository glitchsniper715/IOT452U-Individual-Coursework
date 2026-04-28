package com.digitalid.service.consumption;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.AuditEntry;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.IDNotFoundException;
import com.digitalid.infrastructure.AuditRepository;
import com.digitalid.infrastructure.IdentityRepository;

import java.time.LocalDateTime;
import java.util.List;

public class VerificationService {
    private static final String ACTION_VERIFICATION = "VERIFICATION_REQUESTED";

    private static final String STATUS_CHANGE_ACTION = "STATUS_CHANGE";
    private static final String SUSPENDED_DETAIL     = "SUSPENDED";

    private final IdentityRepository   repository;
    private final AuthorisationService authService;
    private final AuditRepository      auditRepository;

    public VerificationService(IdentityRepository   repository,
                               AuthorisationService authService,
                               AuditRepository      auditRepository) {
        this.repository      = repository;
        this.authService     = authService;
        this.auditRepository = auditRepository;
    }
    /**
     * checks that the identity exists and is currently ACTIVE.
     * used by BankPortal and EmployerPortal.
     */
    public VerificationResult verifyBasic(String idNumber, OrganisationType callerType) {

        authService.authoriseConsumptionAction(callerType);
        DigitalID digitalID;
        try {
            digitalID = repository.findById(idNumber);
        } catch (IDNotFoundException e) {
            return new VerificationResult("NOT_FOUND", "Identity does not exist");
        }

        VerificationResult result;
        if (digitalID.getStatus() == IDStatus.ACTIVE) {
            result = new VerificationResult("VALID", "Identity is active");
        } else {
            result = new VerificationResult(
                    "INVALID",
                    "Identity status: " + digitalID.getStatus()
            );
        }

        auditRepository.log(idNumber, new AuditEntry(
                LocalDateTime.now(),
                ACTION_VERIFICATION,
                callerType.name(),
                "Basic verification result: " + result.status()
        ));
        return result;
    }
}
