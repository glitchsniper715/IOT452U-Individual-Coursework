package com.digitalid.service.consumption;

import com.digitalid.authorisation.OrganisationType;

import java.time.LocalDateTime;

/** Portal for tax authority organisations. */
public class TaxAuthorityPortal extends OrganisationPortal {

    public TaxAuthorityPortal(VerificationService verificationService) {
        super(verificationService, OrganisationType.TAX_SERVICE);
    }

    /**
     * Verifies that a Digital ID was continuously valid throughout a reporting period.

     * Returns VALID if the identity currently exists, is ACTIVE, and was not
     * suspended at any point between [from, to].
     * Returns INVALID if the identity was suspended during the period (even if subsequently reactivated).
     * Returns NOT_FOUND if no identity with the given ID number exists.
     */
    public VerificationResult verifyForPeriod(String idNumber,
                                              LocalDateTime from,
                                              LocalDateTime to) {
        if (from == null || to == null) {
            return new VerificationResult("INVALID", "Date range must not be null");
        }

        return verificationService.verifyWithHistory(idNumber, organisationType, from, to);
    }
}
