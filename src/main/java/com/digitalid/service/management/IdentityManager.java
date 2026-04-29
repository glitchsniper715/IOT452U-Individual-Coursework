package com.digitalid.service.management;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.AuditEntry;
import com.digitalid.domain.DigitalID;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.ImmutableFieldException;
import com.digitalid.exception.ValidationException;
import com.digitalid.infrastructure.AuditRepository;
import com.digitalid.infrastructure.IdentityRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class IdentityManager {

    private static final String ACTION_CREATE = "IDENTITY_CREATED";
    private static final String ACTION_UPDATE = "IDENTITY_UPDATED";
    private static final String ACTION_STATUS_CHANGE = "STATUS_CHANGE";

    private static final String ATTR_FULL_NAME = "fullName";
    private static final String ATTR_DATE_OF_BIRTH = "dateOfBirth";
    private static final String ATTR_PLACE_OF_BIRTH = "placeOfBirth";

    private static final String ATTR_ADDRESS = "address";
    private static final String ATTR_NATIONALITY = "nationality";

    private static final Set<String> IMMUTABLE_FIELDS =
            Set.of("idNumber", "dateOfBirth", "placeOfBirth");

    private final IdentityRepository repository;
    private final AuthorisationService authService;
    private final AuditRepository auditRepository;


    public IdentityManager(IdentityRepository repository,
                           AuthorisationService authService,
                           AuditRepository auditRepository) {
        this.repository = repository;
        this.authService = authService;
        this.auditRepository = auditRepository;
    }

    /**
     *  Authorise — only CENTRAL_AUTHORITY may create identities.
     *  Validate — fullName, dateOfBirth, and placeOfBirth must all be present.
     *  Generate — a UUID is assigned as the immutable idNumber.
     *  Persist — the new DigitalID is saved to the repository.
     *  Audit — an IDENTITY_CREATED entry is appended to the audit log.
     */
    public String create(Map<String, Object> attributes, OrganisationType callerType) {

        authService.authoriseManagementAction(callerType);

        validateRequiredField(attributes, ATTR_FULL_NAME);
        validateRequiredField(attributes, ATTR_DATE_OF_BIRTH);
        validateRequiredField(attributes, ATTR_PLACE_OF_BIRTH);

        String idNumber = UUID.randomUUID().toString();
        String fullName = (String) attributes.get(ATTR_FULL_NAME);
        LocalDate dateOfBirth = (LocalDate) attributes.get(ATTR_DATE_OF_BIRTH);
        String placeOfBirth = (String) attributes.get(ATTR_PLACE_OF_BIRTH);

        String address = attributes.containsKey(ATTR_ADDRESS)
                ? (String) attributes.get(ATTR_ADDRESS) : "";
        String nationality = attributes.containsKey(ATTR_NATIONALITY)
                ? (String) attributes.get(ATTR_NATIONALITY) : "";

        DigitalID newID = new DigitalID(
                idNumber,
                fullName,
                dateOfBirth,
                placeOfBirth,
                address,
                nationality
        );

        repository.save(newID);

        AuditEntry createEntry = new AuditEntry(
                LocalDateTime.now(),
                ACTION_CREATE,
                callerType.name(),
                "Identity created for: " + fullName
        );
        newID.addAuditEntry(createEntry);
        auditRepository.log(idNumber, createEntry);

        return idNumber;
    }

    public void updateAttributes(String idNumber, Map<String, Object> updates, OrganisationType callerType) {

        authService.authoriseManagementAction(callerType);
        DigitalID digitalID = repository.findById(idNumber);

        if (digitalID.getStatus() == IDStatus.REVOKED) {
            throw new ValidationException(
                    "Cannot update a REVOKED identity: " + idNumber);
        }

        for (String key : updates.keySet()) {
            if (IMMUTABLE_FIELDS.contains(key)) {
                throw new ImmutableFieldException(
                        "Field cannot be changed after creation: " + key);
            }
        }
        applyMutableUpdates(digitalID, updates);
        repository.save(digitalID);

        AuditEntry updateEntry = new AuditEntry(
                LocalDateTime.now(), ACTION_UPDATE, callerType.name(),
                "Attributes updated: " + updates.keySet()
        );

        digitalID.addAuditEntry(updateEntry);
        auditRepository.log(idNumber, updateEntry);
    }

    public void changeStatus(String idNumber, IDStatus newStatus, OrganisationType callerType) {

        authService.authoriseManagementAction(callerType);
        DigitalID digitalID = repository.findById(idNumber);

        digitalID.transitionStatus(newStatus, callerType.name());

        repository.save(digitalID);

        AuditEntry statusEntry = new AuditEntry(
                LocalDateTime.now(), ACTION_STATUS_CHANGE, callerType.name(),
                "Status changed to: " + newStatus
        );

        digitalID.addAuditEntry(statusEntry);
        auditRepository.log(idNumber, statusEntry);
    }

    private void applyMutableUpdates(DigitalID digitalID, Map<String, Object> updates) {
        if (updates.containsKey(ATTR_FULL_NAME))
            digitalID.setFullName((String) updates.get(ATTR_FULL_NAME));
        if (updates.containsKey(ATTR_ADDRESS))
            digitalID.setAddress((String) updates.get(ATTR_ADDRESS));
        if (updates.containsKey(ATTR_NATIONALITY))
            digitalID.setNationality((String) updates.get(ATTR_NATIONALITY));
        if (updates.containsKey("temporaryRestriction"))
            digitalID.setTemporaryRestriction((boolean) updates.get("temporaryRestriction"));
    }

    private void validateRequiredField(Map<String, Object> attributes, String fieldName) {
        Object value = attributes.get(fieldName);
        if (value == null || value.toString().isBlank()) {
            throw new ValidationException("Required field is missing or blank: " + fieldName);
        }
    }

    public void setRestriction(String idNumber, boolean restricted, OrganisationType callerType) {
        authService.authoriseManagementAction(callerType);
        DigitalID digitalID = repository.findById(idNumber);

        digitalID.setTemporaryRestriction(restricted);
        repository.save(digitalID);

        AuditEntry entry = new AuditEntry(
                LocalDateTime.now(),
                "RESTRICTION_UPDATED",
                callerType.name(),
                "Temporary restriction set to: " + restricted
        );
        digitalID.addAuditEntry(entry);
        auditRepository.log(idNumber, entry);
    }
}