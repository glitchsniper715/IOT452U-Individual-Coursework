package com.digitalid.authorisation;

import com.digitalid.exception.UnauthorisedActionException;

import java.util.Set;

/** Checks whether an organisation is permitted to perform a requested action. */
public class AuthorisationService {

    private static final Set<OrganisationType> MANAGEMENT_ROLES =
            Set.of(OrganisationType.CENTRAL_AUTHORITY);

    /**
     * Verifies that the given organisation type is permitted to perform management
     * operations (create, update, change status)
     */
    public void authoriseManagementAction(OrganisationType orgType) {
        if (!MANAGEMENT_ROLES.contains(orgType)) {
            throw new UnauthorisedActionException(
                    orgType + " is not authorised to perform management operations. "
                            + "Only CENTRAL_AUTHORITY may create, update, or change the status of a Digital ID."
            );
        }
    }

    public void authoriseConsumptionAction(OrganisationType orgType) {
    }
}
