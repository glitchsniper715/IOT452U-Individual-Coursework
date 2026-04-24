package com.digitalid.service.management;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.AuditEntry;
import com.digitalid.domain.DigitalID;
import com.digitalid.exception.ValidationException;
import com.digitalid.infrastructure.IdentityRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


public class IdentityManager {

    private static final String ACTION_CREATE       = "IDENTITY_CREATED";

    private static final String ATTR_FULL_NAME      = "fullName";
    private static final String ATTR_DATE_OF_BIRTH  = "dateOfBirth";
    private static final String ATTR_PLACE_OF_BIRTH = "placeOfBirth";

    private static final String ATTR_ADDRESS        = "address";
    private static final String ATTR_NATIONALITY    = "nationality";

    private final IdentityRepository   repository;
    private final AuthorisationService authService;


    public IdentityManager(IdentityRepository repository,
                           AuthorisationService authService) {
        this.repository  = repository;
        this.authService = authService;
    }

    /**
     *   <li><b>Authorise</b>  — only CENTRAL_AUTHORITY may create identities.</li>
     *   <li><b>Validate</b>   — fullName, dateOfBirth, and placeOfBirth must all be present.</li>
     *   <li><b>Generate</b>   — a UUID is assigned as the immutable idNumber.</li>
     *   <li><b>Persist</b>    — the new DigitalID is saved to the repository.</li>
     *   <li><b>Audit</b>      — an IDENTITY_CREATED entry is appended to the audit log.</li>
     */
    public String create(Map<String, Object> attributes,
                         OrganisationType callerType) {

        authService.authoriseManagementAction(callerType);

        validateRequiredField(attributes, ATTR_FULL_NAME);
        validateRequiredField(attributes, ATTR_DATE_OF_BIRTH);
        validateRequiredField(attributes, ATTR_PLACE_OF_BIRTH);

        String idNumber = UUID.randomUUID().toString();
        String fullName = (String) attributes.get(ATTR_FULL_NAME);
        LocalDate dateOfBirth = (LocalDate) attributes.get(ATTR_DATE_OF_BIRTH);
        String placeOfBirth = (String) attributes.get(ATTR_PLACE_OF_BIRTH);

        String address = attributes.containsKey(ATTR_ADDRESS) ? (String) attributes.get(ATTR_ADDRESS) : "";
        String nationality = attributes.containsKey(ATTR_NATIONALITY) ? (String) attributes.get(ATTR_NATIONALITY) : "";

        DigitalID newID = new DigitalID(
                idNumber,
                fullName,
                dateOfBirth,
                placeOfBirth,
                address,
                nationality
        );

        repository.save(newID);

        newID.addAuditEntry(new AuditEntry(
                LocalDateTime.now(),
                ACTION_CREATE,
                callerType.name(),
                "Identity created for: " + fullName
        ));
        return idNumber;
    }

    private void validateRequiredField(Map<String, Object> attributes, String fieldName) {
        Object value = attributes.get(fieldName);
        if (value == null || value.toString().isBlank()) {
            throw new ValidationException("Required field is missing or blank: " + fieldName);
        }
    }
}