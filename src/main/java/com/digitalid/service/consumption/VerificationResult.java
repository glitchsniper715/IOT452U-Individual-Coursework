package com.digitalid.service.consumption;

/**
 * Using a Java record ensures this object cannot be modified after creation.
 * Possible status values: VALID, INVALID, INELIGIBLE, NOT_FOUND
 */
public record VerificationResult(String status, String reason) {
}
