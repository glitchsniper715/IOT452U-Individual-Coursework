package com.digitalid.domain;

import com.digitalid.exception.InvalidTransitionException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a citizen's Digital ID within the system.
 * Immutable fields (idNumber, dateOfBirth, placeOfBirth) cannot be changed after creation
 * Mutable fields (fullName, address, nationality, temporaryRestriction) can only be
 * modified within the domain package via package-private setters
 * The class also maintains an audit log of all actions performed on the identity
 */
public class DigitalID {

    private final String idNumber;
    private final LocalDate dateOfBirth;
    private final String placeOfBirth;

    private String fullName;
    private String address;
    private String nationality;
    private boolean temporaryRestriction;

    private IDStatus status;

    private final List<AuditEntry> auditLog = new ArrayList<>();

    /**
     * Constructs a new DigitalID with required attributes.
     * Status is initialised to ACTIVE by default.
     * idNumber Unique identifier for the digital ID
     * fullName Full name of the individual
     * dateOfBirth Date of birth (immutable)
     * placeOfBirth Place of birth (immutable)
     * address Current address
     * nationality Nationality of the individual
     */
    public DigitalID(String idNumber,
                     String fullName,
                     LocalDate dateOfBirth,
                     String placeOfBirth,
                     String address,
                     String nationality) {

        this.idNumber = idNumber;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.placeOfBirth = placeOfBirth;
        this.address = address;
        this.nationality = nationality;
        this.status = IDStatus.ACTIVE;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public void setTemporaryRestriction(boolean restricted) {
        this.temporaryRestriction = restricted;
    }

    public void addAuditEntry(AuditEntry entry) {
        auditLog.add(entry);
    }

    public String getIdNumber() {
        return idNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAddress() {
        return address;
    }

    public String getNationality() {
        return nationality;
    }

    public boolean isTemporaryRestriction() {
        return temporaryRestriction;
    }

    public IDStatus getStatus() {
        return status;
    }

    /** Returns an unmodifiable view of the audit log to prevent external modification (read-only list) */
    public List<AuditEntry> getAuditLog() {
        return Collections.unmodifiableList(auditLog);
    }


    public void transitionStatus(IDStatus newStatus, String performedBy) {
        if (newStatus == this.status) {
            return;
        }

        if (!isValidTransition(this.status, newStatus)) {
            throw new InvalidTransitionException(
                    "Cannot transition from " + this.status + " to " + newStatus
            );
        }
        IDStatus previousStatus = this.status;
        this.status = newStatus;

        auditLog.add(new AuditEntry(
                LocalDateTime.now(),
                "STATUS_CHANGE",
                performedBy,
                "Status changed from " + previousStatus + " to " + newStatus
        ));
    }

    private boolean isValidTransition(IDStatus from, IDStatus to) {
        return switch (from) {
            case ACTIVE    -> to == IDStatus.SUSPENDED || to == IDStatus.REVOKED;
            case SUSPENDED -> to == IDStatus.ACTIVE    || to == IDStatus.REVOKED;
            case REVOKED   -> false;
        };
    }
}