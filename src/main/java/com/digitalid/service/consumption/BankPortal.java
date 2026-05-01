package com.digitalid.service.consumption;

import com.digitalid.authorisation.OrganisationType;

/** Portal for bank organisations. */
public class BankPortal extends OrganisationPortal {

    public BankPortal(VerificationService verificationService) {
        super(verificationService, OrganisationType.BANK);
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