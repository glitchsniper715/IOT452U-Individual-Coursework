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
    private static final String SUSPENDED_DETAIL = "SUSPENDED";
    private static final String CONDITION_NO_RESTRICTION = "NO_TEMPORARY_RESTRICTION";

    private final IdentityRepository repository;
    private final AuthorisationService authService;
    private final AuditRepository auditRepository;

    public VerificationService(IdentityRepository repository,
                               AuthorisationService authService,
                               AuditRepository auditRepository) {
        this.repository = repository;
        this.authService = authService;
        this.auditRepository = auditRepository;
    }
    /**
     * checks that the identity exists and is currently ACTIVE.
     * used by BankPortal and EmployerPortal.
     */
    public VerificationResult verifyBasic(String idNumber, OrganisationType callerType) {

        authService.authoriseConsumptionAction(callerType);

        DigitalID digitalID = fetchById(idNumber);
        if (digitalID == null) {
            return new VerificationResult("NOT_FOUND", "Identity does not exist");
        }

        VerificationResult result = digitalID.getStatus() == IDStatus.ACTIVE
                ? new VerificationResult("VALID", "Identity is active")
                : new VerificationResult("INVALID", "Identity status: " + digitalID.getStatus());

        auditRepository.log(idNumber, new AuditEntry(
                LocalDateTime.now(),
                ACTION_VERIFICATION,
                callerType.name(),
                "Basic verification result: " + result.status()
        ));
        return result;
    }

    /**
     * checks that the identity is currently ACTIVE
     * and was not suspended at any point during the specified date range.

     * Used by TaxAuthorityPortal
     */
    public VerificationResult verifyWithHistory(String idNumber,
                                                OrganisationType callerType,
                                                LocalDateTime from,
                                                LocalDateTime to) {

        authService.authoriseConsumptionAction(callerType);

        DigitalID digitalID = fetchById(idNumber);
        if (digitalID == null) {
            return new VerificationResult("NOT_FOUND", "Identity does not exist");
        }

        if (digitalID.getStatus() != IDStatus.ACTIVE) {
            return new VerificationResult("INVALID", "Identity status: " + digitalID.getStatus());
        }

        boolean wasSuspendedDuringPeriod = auditRepository
                .findByIdNumberAndDateRange(idNumber, from, to)
                .stream()
                .anyMatch(e -> e.action().equals(STATUS_CHANGE_ACTION)
                        && e.details().contains(SUSPENDED_DETAIL));

        VerificationResult result = wasSuspendedDuringPeriod
                ? new VerificationResult("INVALID", "Identity was suspended during the reporting period")
                : new VerificationResult("VALID", "Identity was active throughout the reporting period");

        auditRepository.log(idNumber, new AuditEntry(
                LocalDateTime.now(),
                ACTION_VERIFICATION,
                callerType.name(),
                "Period verification result: " + result.status()
        ));
        return result;
    }

    public VerificationResult verifyWithEligibility(String idNumber,
                                                    OrganisationType callerType,
                                                    List<String> requiredConditions) {
        authService.authoriseConsumptionAction(callerType);

        DigitalID digitalID = fetchById(idNumber);
        if (digitalID == null) {
            return new VerificationResult("NOT_FOUND", "Identity does not exist");
        }

        if (digitalID.getStatus() != IDStatus.ACTIVE) {
            return new VerificationResult("INVALID", "Identity status: " + digitalID.getStatus());
        }

        if (requiredConditions.contains(CONDITION_NO_RESTRICTION)
                && digitalID.isTemporaryRestriction()) {
            return new VerificationResult("INELIGIBLE", "Identity has a temporary restriction");
        }

        auditRepository.log(idNumber, new AuditEntry(
                LocalDateTime.now(),
                ACTION_VERIFICATION,
                callerType.name(),
                "Eligibility verification result: VALID"
        ));
        return new VerificationResult("VALID", "Identity is eligible");
    }

    private DigitalID fetchById(String idNumber) {
        try {
            return repository.findById(idNumber);
        } catch (IDNotFoundException e) {
            return null;
        }
    }

}
