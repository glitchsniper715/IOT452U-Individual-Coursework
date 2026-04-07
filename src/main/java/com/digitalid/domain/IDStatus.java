package com.digitalid.domain;
/** Lifecycle status of a DigitalID.
 * REVOKED is terminal - no transitions permitted from here */
public enum IDStatus {
    ACTIVE,
    SUSPENDED,
    REVOKED,
}
