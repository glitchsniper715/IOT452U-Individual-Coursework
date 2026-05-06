package com.digitalid.presentation;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.IDStatus;
import com.digitalid.exception.IDNotFoundException;
import com.digitalid.exception.ImmutableFieldException;
import com.digitalid.exception.InvalidTransitionException;
import com.digitalid.exception.UnauthorisedActionException;
import com.digitalid.exception.ValidationException;
import com.digitalid.infrastructure.AuditRepository;
import com.digitalid.infrastructure.InMemoryAuditRepository;
import com.digitalid.infrastructure.InMemoryIdentityRepository;
import com.digitalid.infrastructure.IdentityRepository;
import com.digitalid.service.consumption.*;
import com.digitalid.service.management.IdentityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entry point for the Digital ID System demonstration.
 * Runs nine scenarios covering every system capability. Each scenario clearly labels whether an operation is
 * expected to SUCCEED or be REJECTED, demonstrating that the system behaves correctly in both cases.
 */
public class Main {

    public static void main(String[] args) {

        IdentityRepository identityRepository = new InMemoryIdentityRepository();
        AuditRepository auditRepository = new InMemoryAuditRepository();
        AuthorisationService authService = new AuthorisationService();

        IdentityManager manager = new IdentityManager(identityRepository, authService, auditRepository);
        VerificationService verificationService = new VerificationService(identityRepository, authService, auditRepository);

        BankPortal bankPortal = new BankPortal(verificationService);
        EmployerPortal employerPortal = new EmployerPortal(verificationService);
        TaxAuthorityPortal taxPortal = new TaxAuthorityPortal(verificationService);
        DrivingLicencePortal drivingPortal = new DrivingLicencePortal(verificationService);

        // SCENARIO 1: Creating Digital IDs (ACCEPTED operations)
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 1: Creating Digital IDs                            ║");
        System.out.println("║  Only CENTRAL_AUTHORITY may create identities                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        String idJane = manager.create(Map.of(
                "fullName", "Jane Smith",
                "dateOfBirth", LocalDate.of(1990, 5, 15),
                "placeOfBirth","London",
                "address", "12 Baker Street, London",
                "nationality", "British"
        ), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Central Authority creates Jane Smith → ID: " + shortId(idJane));

        String idJohn = manager.create(Map.of(
                "fullName","John Doe",
                "dateOfBirth", LocalDate.of(1985, 11, 20),
                "placeOfBirth","Manchester",
                "address","7 Oak Avenue, Manchester",
                "nationality", "British"
        ), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Central Authority creates John Doe → ID: " + shortId(idJohn));

        String idAli = manager.create(Map.of(
                "fullName", "Ali Hassan",
                "dateOfBirth", LocalDate.of(1995, 3, 8),
                "placeOfBirth","Birmingham",
                "address","3 Elm Road, Birmingham",
                "nationality","British"
        ), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Central Authority creates Ali Hassan → ID: " + shortId(idAli));

        // SCENARIO 2: Unauthorised Creation Attempts (REJECTED operations)
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 2: Unauthorised Creation Attempts                  ║");
        System.out.println("║  Banks, employers, and other orgs CANNOT create identities   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        try {
            manager.create(Map.of(
                    "fullName", "Fake Person",
                    "dateOfBirth", LocalDate.of(2000, 1, 1),
                    "placeOfBirth","London"
            ), OrganisationType.BANK);
            System.out.println(" [BUG] Bank created an identity — this should never happen!");
        } catch (UnauthorisedActionException e) {
            System.out.println("[REJECTED] Bank attempts to create an ID → " + e.getMessage());
        }

        try {
            manager.create(Map.of(
                    "fullName", "Fake Person",
                    "dateOfBirth", LocalDate.of(2000, 1, 1),
                    "placeOfBirth","London"
            ), OrganisationType.EMPLOYER);
            System.out.println("[BUG] Employer created an identity — this should never happen!");
        } catch (UnauthorisedActionException e) {
            System.out.println("[REJECTED] Employer attempts to create an ID → " + e.getMessage());
        }

        try {
            manager.create(Map.of(
                    "fullName", "Fake Person",
                    "dateOfBirth", LocalDate.of(2000, 1, 1),
                    "placeOfBirth","London"
            ), OrganisationType.TAX_SERVICE);
            System.out.println("[BUG] Tax service created an identity — this should never happen!");
        } catch (UnauthorisedActionException e) {
            System.out.println("[REJECTED] Tax service attempts to create an ID → " + e.getMessage());
        }

        // SCENARIO 3: Validation — Missing Required Fields (REJECTED operations)
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 3: Creation With Missing Required Fields           ║");
        System.out.println("║  fullName, dateOfBirth, placeOfBirth are all required        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        try {
            manager.create(Map.of(
                    "dateOfBirth", LocalDate.of(2000, 1, 1),
                    "placeOfBirth","London"
            ), OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Created with missing fullName — this should never happen!");
        } catch (ValidationException e) {
            System.out.println("[REJECTED] Missing fullName → " + e.getMessage());
        }

        try {
            manager.create(Map.of(
                    "fullName", "No DOB Person",
                    "placeOfBirth","London"
            ), OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Created with missing dateOfBirth — this should never happen!");
        } catch (ValidationException e) {
            System.out.println("[REJECTED] Missing dateOfBirth → " + e.getMessage());
        }

        // SCENARIO 4: Status Transitions (ACCEPTED and REJECTED)
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 4: Status Transitions                              ║");
        System.out.println("║  Valid: ACTIVE↔SUSPENDED, ACTIVE/SUSPENDED→REVOKED           ║");
        System.out.println("║  Invalid: anything FROM REVOKED (it is terminal)             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        manager.changeStatus(idJane, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Jane: ACTIVE → SUSPENDED");

        manager.changeStatus(idJane, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Jane: SUSPENDED → ACTIVE");

        manager.changeStatus(idJohn, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] John: ACTIVE → SUSPENDED");

        manager.changeStatus(idJohn, IDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] John: SUSPENDED → REVOKED");

        manager.changeStatus(idAli, IDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Ali: ACTIVE → REVOKED");

        try {
            manager.changeStatus(idJohn, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Revoked ID was reactivated — this should never happen!");
        } catch (InvalidTransitionException e) {
            System.out.println("[REJECTED] John REVOKED → ACTIVE (terminal) → " + e.getMessage());
        }

        try {
            manager.changeStatus(idAli, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Revoked ID was suspended — this should never happen!");
        } catch (InvalidTransitionException e) {
            System.out.println("[REJECTED] Ali REVOKED → SUSPENDED (terminal) → " + e.getMessage());
        }

        try {
            manager.changeStatus(idJane, IDStatus.SUSPENDED, OrganisationType.BANK);
            System.out.println("[BUG] Bank changed a status — this should never happen!");
        } catch (UnauthorisedActionException e) {
            System.out.println("[REJECTED] Bank attempts to change Jane's status → " + e.getMessage());
        }

        // SCENARIO 5: Updating Identity Attributes (ACCEPTED and REJECTED)
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 5: Updating Identity Attributes                    ║");
        System.out.println("║  Mutable: fullName, address, nationality                     ║");
        System.out.println("║  Immutable: idNumber, dateOfBirth, placeOfBirth              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        manager.updateAttributes(idJane, Map.of("address", "99 New Road, London"), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Jane's address updated (mutable field)");

        manager.updateAttributes(idJane, Map.of("fullName", "Jane Smith-Jones"), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Jane's fullName updated (mutable field)");

        try {
            manager.updateAttributes(idJane, Map.of("dateOfBirth", LocalDate.of(2000, 1, 1)), OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Immutable field was changed — this should never happen!");
        } catch (ImmutableFieldException e) {
            System.out.println("[REJECTED] Attempt to change dateOfBirth → " + e.getMessage());
        }

        try {
            manager.updateAttributes(idJane, Map.of("placeOfBirth", "Paris"), OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Immutable field was changed — this should never happen!");
        } catch (ImmutableFieldException e) {
            System.out.println("[REJECTED] Attempt to change placeOfBirth → " + e.getMessage());
        }

        try {
            manager.updateAttributes(idJohn, Map.of("address", "New Address"), OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Revoked ID was updated — this should never happen!");
        } catch (ValidationException e) {
            System.out.println("[REJECTED] Update on REVOKED identity (John) → " + e.getMessage());
        }

        try {
            manager.updateAttributes(idJane, Map.of("address", "Hacked Address"), OrganisationType.TAX_SERVICE);
            System.out.println("[BUG] Tax service updated an identity — this should never happen!");
        } catch (UnauthorisedActionException e) {
            System.out.println("[REJECTED] Tax service attempts to update Jane → " + e.getMessage());
        }

        // SCENARIO 6: Bank & Employer Portal Verification (ACCEPTED operations)
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 6: Bank & Employer Portal Verification             ║");
        System.out.println("║  Returns VALID (active), INVALID (suspended/revoked),        ║");
        System.out.println("║  or NOT_FOUND (unknown ID)                                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        VerificationResult bankJane = bankPortal.verifyIdentity(idJane);
        System.out.println("[ACCEPTED] Bank verifies Jane (ACTIVE) → " + bankJane.status() + " — " + bankJane.reason());

        VerificationResult bankJohn = bankPortal.verifyIdentity(idJohn);
        System.out.println("[ACCEPTED] Bank verifies John (REVOKED) → " + bankJohn.status() + " — " + bankJohn.reason());

        VerificationResult empAli = employerPortal.verifyIdentity(idAli);
        System.out.println("[ACCEPTED] Employer verifies Ali (REVOKED) → " + empAli.status() + " — " + empAli.reason());

        VerificationResult bankUnknown = bankPortal.verifyIdentity("UNKNOWN-ID-999");
        System.out.println("[ACCEPTED] Bank verifies unknown ID → " + bankUnknown.status() + " — " + bankUnknown.reason());

        manager.changeStatus(idJane, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        VerificationResult bankJaneSuspended = bankPortal.verifyIdentity(idJane);
        System.out.println("[ACCEPTED] Bank verifies Jane (now SUSPENDED) → " + bankJaneSuspended.status() + " — " + bankJaneSuspended.reason());

        manager.changeStatus(idJane, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("[ACCEPTED] Jane reactivated for remaining scenarios");

        // SCENARIO 7: Tax Authority — Period-Based Verification
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 7: Tax Authority Period-Based Verification         ║");
        System.out.println("║  INVALID if suspended at any point during the period,        ║");
        System.out.println("║  even if reactivated afterwards                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        LocalDateTime periodStart = LocalDateTime.now().minusHours(2);
        LocalDateTime periodEnd = LocalDateTime.now().plusHours(1);

        VerificationResult taxJane = taxPortal.verifyForPeriod(idJane, periodStart, periodEnd);
        System.out.println("[ACCEPTED] Tax — Jane (always active in period) → " + taxJane.status() + " — " + taxJane.reason());

        String idTaxTest = manager.create(Map.of(
                "fullName", "Tax Test Person",
                "dateOfBirth", LocalDate.of(1980, 6, 1),
                "placeOfBirth","Cardiff"
        ), OrganisationType.CENTRAL_AUTHORITY);
        manager.changeStatus(idTaxTest, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        manager.changeStatus(idTaxTest, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);

        VerificationResult taxSuspendedInPeriod = taxPortal.verifyForPeriod(idTaxTest, periodStart, periodEnd);
        System.out.println("[ACCEPTED] Tax — suspended+reactivated in period → " + taxSuspendedInPeriod.status() + " — " + taxSuspendedInPeriod.reason());

        VerificationResult taxUnknown = taxPortal.verifyForPeriod("UNKNOWN-999", periodStart, periodEnd);
        System.out.println("[ACCEPTED] Tax — unknown ID → " + taxUnknown.status() + " — " + taxUnknown.reason());

        // SCENARIO 8: Driving Licence — Eligibility Verification
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 8: Driving Licence Eligibility Verification        ║");
        System.out.println("║  VALID only if ACTIVE and no temporary restriction           ║");
        System.out.println("║  INELIGIBLE if active but restriction is set                 ║");
        System.out.println("║  INVALID if not active at all                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        VerificationResult driveJaneClean = drivingPortal.verifyForLicence(idJane);
        System.out.println("[ACCEPTED] Driving — Jane (active, no restriction) → " + driveJaneClean.status() + " — " + driveJaneClean.reason());

        manager.setRestriction(idJane, true, OrganisationType.CENTRAL_AUTHORITY);
        VerificationResult driveJaneRestricted = drivingPortal.verifyForLicence(idJane);
        System.out.println("[ACCEPTED] Driving — Jane (active, WITH restriction) → " + driveJaneRestricted.status() + " — " + driveJaneRestricted.reason());

        manager.setRestriction(idJane, false, OrganisationType.CENTRAL_AUTHORITY);
        VerificationResult driveJaneRestored = drivingPortal.verifyForLicence(idJane);
        System.out.println("[ACCEPTED] Driving — Jane (restriction removed) → " + driveJaneRestored.status() + " — " + driveJaneRestored.reason());

        VerificationResult driveAli = drivingPortal.verifyForLicence(idAli);
        System.out.println("[ACCEPTED] Driving — Ali (REVOKED) → " + driveAli.status() + " — " + driveAli.reason());

        VerificationResult driveUnknown = drivingPortal.verifyForLicence("UNKNOWN-999");
        System.out.println("[ACCEPTED] Driving — unknown ID → " + driveUnknown.status() + " — " + driveUnknown.reason());

        // SCENARIO 9: Management Operations on Non-Existent IDs (REJECTED)
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 9: Management Operations on Non-Existent IDs       ║");
        System.out.println("║  Status changes and updates on unknown IDs throw             ║");
        System.out.println("║  IDNotFoundException                                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        try {
            manager.changeStatus("DOES-NOT-EXIST", IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Status changed on non-existent ID — this should never happen!");
        } catch (IDNotFoundException e) {
            System.out.println("[REJECTED] Status change on unknown ID → " + e.getMessage());
        }

        try {
            manager.updateAttributes("DOES-NOT-EXIST", Map.of("address", "Somewhere"), OrganisationType.CENTRAL_AUTHORITY);
            System.out.println("[BUG] Update on non-existent ID — this should never happen!");
        } catch (IDNotFoundException e) {
            System.out.println("[REJECTED] Update on unknown ID → " + e.getMessage());
        }
    }

    private static String shortId(String uuid) {
        return "DIG-" + uuid.substring(0, 5).toUpperCase();
    }
}