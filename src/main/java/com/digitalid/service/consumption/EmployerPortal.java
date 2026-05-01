package com.digitalid.service.consumption;

import com.digitalid.authorisation.OrganisationType;

/** Portal for employer organisations. */
public class EmployerPortal extends OrganisationPortal {

    public EmployerPortal(VerificationService verificationService) {
        super(verificationService, OrganisationType.EMPLOYER);
    }

    /**
     * Verifies whether a Digital ID is currently valid.

     * Returns VALID if the identity exists and is ACTIVE at the time of the request.
     * Returns INVALID if the identity exists but is not ACTIVE.
     * Returns NOT_FOUND if no identity with the given ID number exists.
     */
    public VerificationResult verifyIdentity(String idNumber) {
        return verificationService.verifyBasic(idNumber, organisationType);
    }
}
