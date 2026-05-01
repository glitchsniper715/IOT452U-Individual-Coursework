package com.digitalid.service.consumption;

import com.digitalid.authorisation.OrganisationType;

/**
 * Abstract base class for all organisation portals.
 * Each portal exposes only the verification methods appropriate for its role.

 * This is the Facade pattern where each portal is a facade over the full VerificationService,
 * surfacing only what that organisation type needs and nothing more.
 */
public abstract class OrganisationPortal {
    protected final VerificationService verificationService;
    protected final OrganisationType organisationType;

    /**
     * Constructs an OrganisationPortal with its required dependencies.

     * verificationService = the service that performs the actual verification logic
     * organisationType = the organisation type this portal represents
     */
    protected OrganisationPortal(VerificationService verificationService,
                                 OrganisationType    organisationType) {
        this.verificationService = verificationService;
        this.organisationType   = organisationType;
    }
}
