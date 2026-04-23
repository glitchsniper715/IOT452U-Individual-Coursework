package com.digitalid.authorisation;

/**
 * This enum is used by AuthorisationService to decide whether a request is permitted.

 * Only CENTRAL_AUTHORITY can perform management operations (create, update, change status)
 * All other types can only perform consumption operations (verify, look up)
 */
public enum OrganisationType {

    /** The home or interior ministry. Can write or modify Digital IDs. */
    CENTRAL_AUTHORITY,

    /** A bank or financial institution. Can perform basic identity verification only. */
    BANK,

    /** A tax authority. Can check identity validity over a date range (period-based check). */
    TAX_SERVICE,

    /** A driving licence authority. Can check eligibility conditions before issuing a licence. */
    DRIVING_AUTHORITY,

    /** An employer. Can perform basic identity verification only, same as a bank. */
    EMPLOYER
}