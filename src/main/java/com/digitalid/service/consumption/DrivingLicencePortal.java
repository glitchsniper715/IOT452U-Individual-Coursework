package com.digitalid.service.consumption;

import com.digitalid.authorisation.OrganisationType;

import java.util.List;

/** Portal for driving licence authority organisations. */
public class DrivingLicencePortal extends OrganisationPortal {

    /** the identity must not have a temporary restriction applied. */
    private static final List<String> REQUIRED_CONDITIONS =
            List.of("NO_TEMPORARY_RESTRICTION");

    public DrivingLicencePortal(VerificationService verificationService) {
        super(verificationService, OrganisationType.DRIVING_AUTHORITY);
    }

    /**
     * Verifies whether a Digital ID is eligible for a driving licence.

     * Returns VALID if the identity exists, is ACTIVE, and has no temporary restriction.
     * Returns INELIGIBLE if the identity is ACTIVE but has a temporary restriction.
     * Returns INVALID if the identity is not ACTIVE.
     * Returns NOT_FOUND if no identity with the given ID number exists.
     */
    public VerificationResult verifyForLicence(String idNumber) {
        return verificationService.verifyWithEligibility(
                idNumber, organisationType, REQUIRED_CONDITIONS
        );
    }
}
