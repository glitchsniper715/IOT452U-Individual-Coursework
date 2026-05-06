package com.digitalid.presentation;

import com.digitalid.authorisation.AuthorisationService;
import com.digitalid.authorisation.OrganisationType;
import com.digitalid.domain.DigitalID;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;

/**
 * Entry point for the Digital ID System demonstration.
 * Runs nine scenarios covering every system capability. Each scenario clearly labels whether an operation is
 * expected to SUCCEED or be REJECTED, demonstrating that the system behaves correctly in both cases.
 */
public class Main {

    private static final String TOP     = "═══════════════════════════════════════════════════════════════════════════════";
    private static final String MID     = " -----------------------------------------------------------------------------";
    private static final String BOT     = "═══════════════════════════════════════════════════════════════════════════════";
    private static final String DIVIDER = "─────────────────────────────────────────────────────────────────────────────";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static IdentityRepository   identityRepository;
    private static IdentityManager      manager;
    private static VerificationService  verificationService;
    private static BankPortal           bankPortal;
    private static EmployerPortal       employerPortal;
    private static TaxAuthorityPortal   taxPortal;
    private static DrivingLicencePortal drivingPortal;

    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        bootstrap();
        systemBanner();
        sessionLoop();
        System.out.println("\nSystem closed. Goodbye.");
        scanner.close();
    }

    private static void bootstrap() {
        identityRepository = new InMemoryIdentityRepository();
        AuditRepository auditRepository = new InMemoryAuditRepository();
        AuthorisationService authService = new AuthorisationService();

        manager = new IdentityManager(identityRepository, authService, auditRepository);
        verificationService = new VerificationService(identityRepository, authService, auditRepository);

        bankPortal = new BankPortal(verificationService);
        employerPortal = new EmployerPortal(verificationService);
        taxPortal = new TaxAuthorityPortal(verificationService);
        drivingPortal = new DrivingLicencePortal(verificationService);
    }

    /**
     * Outer loop: user picks which organisation they represent each session.
     * They can switch organisations or exit from here
     * */
    private static void sessionLoop() {
        while (true) {
            OrganisationType org = pickOrganisation();
            if (org == null) return; // user chose Exit
            mainMenu(org);
        }
    }

    private static OrganisationType pickOrganisation() {
        while (true) {
            printHeader("Digital ID System — Select Organisation ");
            System.out.println("  Who are you logging in as?" );
            System.out.println(MID);
            System.out.println("  1. Central Authority  (full management access)");
            System.out.println("  2. Bank               (basic identity verification)");
            System.out.println("  3. Tax Authority      (period-based verification)");
            System.out.println("  4. Driving Authority  (eligibility verification)");
            System.out.println("  5. Employer           (basic identity verification)");
            System.out.println(MID);
            System.out.println("  6. Run Demo Scenarios (automated walkthrough)");
            System.out.println("  0. Exit");
            System.out.println(BOT);

            String choice = prompt("Enter choice");
            switch (choice) {
                case "1" -> { return OrganisationType.CENTRAL_AUTHORITY; }
                case "2" -> { return OrganisationType.BANK; }
                case "3" -> { return OrganisationType.TAX_SERVICE; }
                case "4" -> { return OrganisationType.DRIVING_AUTHORITY; }
                case "5" -> { return OrganisationType.EMPLOYER; }
                case "6" -> runDemoScenarios();
                case "0" -> { return null; }
                default  -> error("Invalid choice. Please enter a number from the menu.");
            }
        }
    }

    private static void mainMenu(OrganisationType org) {
        while (true) {
            printHeader("Digital ID System — " + friendlyOrgName(org));
            System.out.println("  Main Menu");
            System.out.println(MID);
            System.out.println("  1. View all Digital IDs");
            System.out.println("  2. Verify a Digital ID");

            if (org == OrganisationType.CENTRAL_AUTHORITY) {
                System.out.println("  3. Create a new Digital ID");
                System.out.println("  4. Update a Digital ID");
                System.out.println("  5. Change status of a Digital ID");
                System.out.println("  6. Set / clear temporary restriction");
                System.out.println("  7. View audit log for a Digital ID");
            }

            System.out.println(MID);
            System.out.println("  0. Back (switch organisation / exit)");
            System.out.println(BOT);

            String choice = prompt("Enter choice");
            switch (choice) {
                case "1" -> viewAllIds();
                case "2" -> verifyMenu(org);
                case "3" -> {
                    if (org == OrganisationType.CENTRAL_AUTHORITY) createId(org);
                    else accessDenied("create a Digital ID");
                }
                case "4" -> {
                    if (org == OrganisationType.CENTRAL_AUTHORITY) updateId(org);
                    else accessDenied("update a Digital ID");
                }
                case "5" -> {
                    if (org == OrganisationType.CENTRAL_AUTHORITY) changeStatus(org);
                    else accessDenied("change status");
                }
                case "6" -> {
                    if (org == OrganisationType.CENTRAL_AUTHORITY) setRestriction(org);
                    else accessDenied("set restrictions");
                }
                case "7" -> {
                    if (org == OrganisationType.CENTRAL_AUTHORITY) viewAuditLog();
                    else accessDenied("view audit log");
                }
                case "0" -> { return; }
                default  -> error("Invalid choice.");
            }
        }
    }

    private static void viewAllIds() {
        printHeader("All Digital IDs in the System");
        Collection<DigitalID> all = identityRepository.findAll();

        if (all.isEmpty()) {
            System.out.println("  (No Digital IDs have been created yet)");
        } else {
            System.out.printf("  %-12s %-22s %-12s %-13s %s%n",
                    "Short ID", "Full Name", "Status", "DOB", "Restriction");
            System.out.println("  " + DIVIDER);
            for (DigitalID id : all) {
                System.out.printf("  %-12s %-22s %-12s %-13s %s%n",
                        shortId(id.getIdNumber()),
                        id.getFullName(),
                        id.getStatus(),
                        id.getDateOfBirth().format(DATE_FMT),
                        id.isTemporaryRestriction() ? "RESTRICTED" : "None");
            }
        }
        System.out.println();
        pause();
    }

    private static void verifyMenu(OrganisationType org) {
        printHeader("Verify a Digital ID — " + friendlyOrgName(org));

        switch (org) {

            case CENTRAL_AUTHORITY, BANK, EMPLOYER -> {
                System.out.println("  Basic verification: checks the ID exists and is currently ACTIVE.\n");
                String idNumber = pickIdInteractively();
                if (idNumber == null) return;

                VerificationResult result = switch (org) {
                    case BANK -> bankPortal.verifyIdentity(idNumber);
                    case EMPLOYER -> employerPortal.verifyIdentity(idNumber);
                    default -> verificationService.verifyBasic(idNumber, org);
                };
                printVerificationResult(result);
            }

            case TAX_SERVICE -> {
                System.out.println("  Period verification: checks the ID was not suspended at any");
                System.out.println("  point during a given date range (even if later reactivated).\n");
                String idNumber = pickIdInteractively();
                if (idNumber == null) return;

                LocalDateTime from = promptDateTime("Period start date (dd/MM/yyyy)");
                LocalDateTime to = promptDateTime("Period end date (dd/MM/yyyy)");
                if (from == null || to == null) return;

                VerificationResult result = taxPortal.verifyForPeriod(idNumber, from, to);
                printVerificationResult(result);
            }

            case DRIVING_AUTHORITY -> {
                System.out.println("  Eligibility verification: checks the ID is ACTIVE and has");
                System.out.println("  no temporary restriction applied.\n");
                String idNumber = pickIdInteractively();
                if (idNumber == null) return;

                VerificationResult result = drivingPortal.verifyForLicence(idNumber);
                printVerificationResult(result);
            }
        }
        pause();
    }

    private static void createId(OrganisationType org) {
        printHeader("Create a New Digital ID");
        System.out.println("  Required: Full Name, Date of Birth, Place of Birth");
        System.out.println("  Optional: Address, Nationality\n");

        String fullName = prompt("  Full name");
        if (fullName.isBlank()) { error("Full name cannot be blank."); return; }

        LocalDate dob = promptDate("  Date of birth (dd/MM/yyyy)");
        if (dob == null) return;

        String placeOfBirth = prompt("  Place of birth");
        if (placeOfBirth.isBlank()) { error("Place of birth cannot be blank."); return; }

        String address = prompt("  Address (press Enter to skip)");
        String nationality = prompt("  Nationality (press Enter to skip)");

        try {
            String newId = manager.create(Map.of(
                    "fullName", fullName,
                    "dateOfBirth", dob,
                    "placeOfBirth", placeOfBirth,
                    "address", address,
                    "nationality", nationality
            ), org);
            success("Digital ID created successfully.");
            System.out.println("  Short ID : " + shortId(newId));
            System.out.println("  Full UUID: " + newId);
        } catch (UnauthorisedActionException e) {
            accessDeniedMsg(e.getMessage());
        } catch (ValidationException e) {
            error("Validation failed: " + e.getMessage());
        }
        pause();
    }

    private static void updateId(OrganisationType org) {
        printHeader("Update a Digital ID");
        System.out.println("  Mutable: fullName, address, nationality");
        System.out.println("  Immutable: idNumber, dateOfBirth, placeOfBirth (cannot be changed)\n");

        String idNumber = pickIdInteractively();
        if (idNumber == null) return;

        printIdSummary(idNumber);

        System.out.println("\n  Which field do you want to update?");
        System.out.println("  1. Full name");
        System.out.println("  2. Address");
        System.out.println("  3. Nationality");
        System.out.println("  0. Cancel");
        String fieldChoice = prompt("  Choice");

        String fieldKey = switch (fieldChoice) {
            case "1" -> "fullName";
            case "2" -> "address";
            case "3" -> "nationality";
            case "0" -> null;
            default  -> { error("Invalid choice."); yield null; }
        };
        if (fieldKey == null) return;

        String newValue = prompt("  New value for " + fieldKey);
        if (newValue.isBlank()) { error("Value cannot be blank."); return; }

        try {
            manager.updateAttributes(idNumber, Map.of(fieldKey, newValue), org);
            success("Field '" + fieldKey + "' updated successfully.");
        } catch (UnauthorisedActionException e) {
            accessDeniedMsg(e.getMessage());
        } catch (ImmutableFieldException e) {
            error("Cannot update immutable field: " + e.getMessage());
        } catch (ValidationException e) {
            error("Update rejected: " + e.getMessage());
        } catch (IDNotFoundException e) {
            error("Digital ID not found: " + e.getMessage());
        }
        pause();
    }

    private static void changeStatus(OrganisationType org) {
        printHeader("Change Status of a Digital ID");
        System.out.println("  Valid transitions: ACTIVE <-> SUSPENDED, ACTIVE/SUSPENDED -> REVOKED");
        System.out.println("  REVOKED is terminal and cannot be reversed.\n");

        String idNumber = pickIdInteractively();
        if (idNumber == null) return;

        printIdSummary(idNumber);

        System.out.println("\n  Select new status:");
        System.out.println("  1. ACTIVE");
        System.out.println("  2. SUSPENDED");
        System.out.println("  3. REVOKED  (WARNING: this cannot be undone)");
        System.out.println("  0. Cancel");
        String choice = prompt("  Choice");

        IDStatus newStatus = switch (choice) {
            case "1" -> IDStatus.ACTIVE;
            case "2" -> IDStatus.SUSPENDED;
            case "3" -> IDStatus.REVOKED;
            case "0" -> null;
            default  -> { error("Invalid choice."); yield null; }
        };
        if (newStatus == null) return;

        if (newStatus == IDStatus.REVOKED) {
            String confirm = prompt("  Type CONFIRM to proceed with REVOKE (this is permanent)");
            if (!confirm.equals("CONFIRM")) {
                System.out.println("  Cancelled.");
                return;
            }
        }

        try {
            manager.changeStatus(idNumber, newStatus, org);
            success("Status changed to " + newStatus + ".");
        } catch (UnauthorisedActionException e) {
            accessDeniedMsg(e.getMessage());
        } catch (InvalidTransitionException e) {
            error("Invalid transition: " + e.getMessage());
        } catch (ValidationException e) {
            error("Status change rejected: " + e.getMessage());
        } catch (IDNotFoundException e) {
            error("Digital ID not found: " + e.getMessage());
        }
        pause();
    }

    private static void setRestriction(OrganisationType org) {
        printHeader("Set / Clear Temporary Restriction");
        System.out.println("  A restriction prevents a Driving Licence from being issued.\n");

        String idNumber = pickIdInteractively();
        if (idNumber == null) return;

        printIdSummary(idNumber);

        System.out.println("\n  1. Apply restriction");
        System.out.println("  2. Remove restriction");
        System.out.println("  0. Cancel");
        String choice = prompt("  Choice");

        if (choice.equals("0")) return;

        boolean restrict = switch (choice) {
            case "1" -> true;
            case "2" -> false;
            default  -> { error("Invalid choice."); yield false; }
        };

        if (!choice.equals("1") && !choice.equals("2")) return;

        try {
            manager.setRestriction(idNumber, restrict, org);
            success("Restriction " + (restrict ? "applied." : "removed."));
        } catch (UnauthorisedActionException e) {
            accessDeniedMsg(e.getMessage());
        } catch (IDNotFoundException e) {
            error("Digital ID not found: " + e.getMessage());
        }
        pause();
    }

    private static void viewAuditLog() {
        printHeader("Audit Log");
        String idNumber = pickIdInteractively();
        if (idNumber == null) return;

        try {
            DigitalID id = identityRepository.findById(idNumber);
            System.out.println("\n  Audit log for: " + id.getFullName() + " (" + shortId(idNumber) + ")");
            System.out.println("  " + DIVIDER);

            if (id.getAuditLog().isEmpty()) {
                System.out.println("  (No audit entries)");
            } else {
                for (var entry : id.getAuditLog()) {
                    System.out.printf("  [%s] %s by %s -> %s%n",
                            entry.timestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                            entry.action(),
                            entry.performedBy(),
                            entry.details());
                }
            }
        } catch (IDNotFoundException e) {
            error("Digital ID not found: " + e.getMessage());
        }
        System.out.println();
        pause();
    }

    private static String pickIdInteractively() {
        Collection<DigitalID> all = identityRepository.findAll();

        if (all.isEmpty()) {
            System.out.println("\n  (No Digital IDs exist yet. Create one first.)\n");
            return null;
        }

        System.out.println("  Available Digital IDs:");
        System.out.printf("  %-12s %-22s %-12s%n", "Short ID", "Full Name", "Status");
        System.out.println("  " + DIVIDER);
        for (DigitalID id : all) {
            System.out.printf("  %-12s %-22s %-12s%n",
                    shortId(id.getIdNumber()),
                    id.getFullName(),
                    id.getStatus());
        }
        System.out.println();

        String input = prompt("  Enter Short ID (e.g. DIG-XXXXX) or full UUID (0 to cancel)");
        if (input.equals("0")) return null;

        // Match on short ID prefix (DIG-XXXXX — 9 chars total)
        if (input.toUpperCase().startsWith("DIG-") && input.length() == 9) {
            String prefix = input.substring(4).toLowerCase();
            for (DigitalID id : all) {
                if (id.getIdNumber().startsWith(prefix)) {
                    return id.getIdNumber();
                }
            }
            error("No Digital ID found matching short ID: " + input);
            return null;
        }

        // Fall back to full UUID
        if (identityRepository.exists(input)) {
            return input;
        }
        error("No Digital ID found with that identifier.");
        return null;
    }

    private static void printIdSummary(String idNumber) {
        try {
            DigitalID id = identityRepository.findById(idNumber);
            System.out.println("\n  Selected    : " + id.getFullName() + " (" + shortId(idNumber) + ")");
            System.out.println("  Status      : " + id.getStatus()
                    + (id.isTemporaryRestriction() ? "  [RESTRICTED]" : ""));
            System.out.println("  DOB         : " + id.getDateOfBirth().format(DATE_FMT));
            System.out.println("  Address     : " + (id.getAddress().isBlank() ? "—" : id.getAddress()));
            System.out.println("  Nationality : " + (id.getNationality().isBlank() ? "—" : id.getNationality()));
        } catch (IDNotFoundException ignored) {
            // Caller already validated the ID via pickIdInteractively
        }
    }

    private static void printVerificationResult(VerificationResult result) {
        String label = switch (result.status()) {
            case "VALID"      -> "[ VALID ]      ";
            case "INVALID"    -> "[ INVALID ]    ";
            case "INELIGIBLE" -> "[ INELIGIBLE ] ";
            case "NOT_FOUND"  -> "[ NOT FOUND ]  ";
            default           -> "[ " + result.status() + " ] ";
        };
        System.out.println("\n  " + label + result.reason());
    }

    private static String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private static LocalDate promptDate(String label) {
        String input = prompt(label);
        try {
            return LocalDate.parse(input, DATE_FMT);
        } catch (DateTimeParseException e) {
            error("Invalid date format. Expected dd/MM/yyyy (e.g. 15/05/1990).");
            return null;
        }
    }

    private static LocalDateTime promptDateTime(String label) {
        LocalDate date = promptDate(label);
        return date == null ? null : date.atStartOfDay();
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println(TOP);
        System.out.println(title);
        System.out.println(BOT);
    }

    private static void success(String msg) {
        System.out.println("\n  [OK] " + msg);
    }

    private static void error(String msg) {
        System.out.println("\n  [ERROR] " + msg);
    }

    private static void accessDenied(String action) {
        System.out.println("\n  [ACCESS DENIED] Your organisation cannot " + action + ".");
        System.out.println("  Only CENTRAL_AUTHORITY can perform management operations.");
        pause();
    }

    private static void accessDeniedMsg(String serviceMessage) {
        System.out.println("\n  [ACCESS DENIED] " + serviceMessage);
    }

    private static void pause() {
        System.out.print("  Press Enter to continue...");
        scanner.nextLine();
    }

    private static String friendlyOrgName(OrganisationType org) {
        return switch (org) {
            case CENTRAL_AUTHORITY -> "Central Authority";
            case BANK -> "Bank";
            case TAX_SERVICE -> "Tax Authority";
            case DRIVING_AUTHORITY -> "Driving Licence Authority";
            case EMPLOYER -> "Employer";
        };
    }

    private static String shortId(String uuid) {
        return "DIG-" + uuid.substring(0, 5).toUpperCase();
    }

    private static void systemBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║          DIGITAL IDENTITY MANAGEMENT SYSTEM                  ║");
        System.out.println("║                   IOT452U Coursework                         ║");
        System.out.println("║                                                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Role-based access control is enforced at all times.         ║");
        System.out.println("║  Only Central Authority can create or modify identities.     ║");
        System.out.println("║  All other organisations are limited to verification only.   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void runDemoScenarios() {
        System.out.println("\n  Running demo — this uses separate data and won't affect your");
        System.out.println("  live session.\n");

        // Fresh isolated services so demo output doesn't pollute the live store
        IdentityRepository   demoRepo    = new InMemoryIdentityRepository();
        AuditRepository      demoAudit   = new InMemoryAuditRepository();
        AuthorisationService demoAuth    = new AuthorisationService();
        IdentityManager      demoMgr     = new IdentityManager(demoRepo, demoAuth, demoAudit);
        VerificationService  demoVerify  = new VerificationService(demoRepo, demoAuth, demoAudit);
        BankPortal           demoBankP   = new BankPortal(demoVerify);
        EmployerPortal       demoEmpP    = new EmployerPortal(demoVerify);
        TaxAuthorityPortal   demoTaxP    = new TaxAuthorityPortal(demoVerify);
        DrivingLicencePortal demoDriveP  = new DrivingLicencePortal(demoVerify);

        // SCENARIO 1
        System.out.println(TOP);
        System.out.println("  SCENARIO 1: Creating Digital IDs");
        System.out.println("  Only CENTRAL_AUTHORITY may create identities");
        System.out.println(MID);

        String idJane = demoMgr.create(Map.of(
                "fullName", "Jane Smith", "dateOfBirth", LocalDate.of(1990, 5, 15),
                "placeOfBirth", "London", "address", "12 Baker Street, London", "nationality", "British"
        ), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Central Authority creates Jane Smith  → ID: " + shortId(idJane));

        String idJohn = demoMgr.create(Map.of(
                "fullName", "John Doe", "dateOfBirth", LocalDate.of(1985, 11, 20),
                "placeOfBirth", "Manchester", "address", "7 Oak Avenue, Manchester", "nationality", "British"
        ), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Central Authority creates John Doe    → ID: " + shortId(idJohn));

        String idAli = demoMgr.create(Map.of(
                "fullName", "Ali Hassan", "dateOfBirth", LocalDate.of(1995, 3, 8),
                "placeOfBirth", "Birmingham", "address", "3 Elm Road, Birmingham", "nationality", "British"
        ), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Central Authority creates Ali Hassan  → ID: " + shortId(idAli));

        // SCENARIO 2
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 2: Unauthorised Creation Attempts");
        System.out.println("  Banks, employers, and other orgs CANNOT create identities ");
        System.out.println(MID);

        for (OrganisationType badOrg : new OrganisationType[]{
                OrganisationType.BANK, OrganisationType.EMPLOYER, OrganisationType.TAX_SERVICE}) {
            try {
                demoMgr.create(Map.of("fullName", "Fake Person",
                        "dateOfBirth", LocalDate.of(2000, 1, 1), "placeOfBirth", "London"), badOrg);
                System.out.println("  [BUG] " + badOrg + " created an identity — this should never happen!");
            } catch (UnauthorisedActionException e) {
                System.out.println("  [REJECTED] " + badOrg + " attempts to create → \n             " + e.getMessage());
            }
        }

        // SCENARIO 3
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 3: Creation With Missing Required Fields");
        System.out.println("  fullName, dateOfBirth, placeOfBirth are all required");
        System.out.println(MID);

        try {
            demoMgr.create(Map.of("dateOfBirth", LocalDate.of(2000, 1, 1), "placeOfBirth", "London"),
                    OrganisationType.CENTRAL_AUTHORITY);
        } catch (ValidationException e) {
            System.out.println("  [REJECTED] Missing fullName → " + e.getMessage());
        }
        try {
            demoMgr.create(Map.of("fullName", "No DOB Person", "placeOfBirth", "London"),
                    OrganisationType.CENTRAL_AUTHORITY);
        } catch (ValidationException e) {
            System.out.println("  [REJECTED] Missing dateOfBirth → " + e.getMessage());
        }

        // SCENARIO 4
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 4: Status Transitions");
        System.out.println("  Valid: ACTIVE<->SUSPENDED, ACTIVE/SUSPENDED->REVOKED");
        System.out.println("  Invalid: anything FROM REVOKED (terminal)");
        System.out.println(MID);

        demoMgr.changeStatus(idJane, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Jane: ACTIVE → SUSPENDED");
        demoMgr.changeStatus(idJane, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Jane: SUSPENDED → ACTIVE");
        demoMgr.changeStatus(idJohn, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] John: ACTIVE → SUSPENDED");
        demoMgr.changeStatus(idJohn, IDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] John: SUSPENDED → REVOKED");
        demoMgr.changeStatus(idAli, IDStatus.REVOKED, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Ali:  ACTIVE → REVOKED");

        try {
            demoMgr.changeStatus(idJohn, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        } catch (InvalidTransitionException e) {
            System.out.println("  [REJECTED] John REVOKED → ACTIVE (terminal) → \n             " + e.getMessage());
        }
        try {
            demoMgr.changeStatus(idAli, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        } catch (InvalidTransitionException e) {
            System.out.println("  [REJECTED] Ali  REVOKED → SUSPENDED (terminal) → \n             " + e.getMessage());
        }
        try {
            demoMgr.changeStatus(idJane, IDStatus.SUSPENDED, OrganisationType.BANK);
        } catch (UnauthorisedActionException e) {
            System.out.println("  [REJECTED] Bank attempts to change Jane's status → \n             " + e.getMessage());
        }

        // SCENARIO 5
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 5: Updating Identity Attributes");
        System.out.println("  Mutable: fullName, address, nationality");
        System.out.println("  Immutable: idNumber, dateOfBirth, placeOfBirth");
        System.out.println(MID);

        demoMgr.updateAttributes(idJane, Map.of("address", "99 New Road, London"), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Jane's address updated (mutable field)");
        demoMgr.updateAttributes(idJane, Map.of("fullName", "Jane Smith-Jones"), OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Jane's fullName updated (mutable field)");
        try {
            demoMgr.updateAttributes(idJane, Map.of("dateOfBirth", LocalDate.of(2000, 1, 1)),
                    OrganisationType.CENTRAL_AUTHORITY);
        } catch (ImmutableFieldException e) {
            System.out.println("  [REJECTED] Attempt to change dateOfBirth → \n             " + e.getMessage());
        }
        try {
            demoMgr.updateAttributes(idJane, Map.of("placeOfBirth", "Paris"),
                    OrganisationType.CENTRAL_AUTHORITY);
        } catch (ImmutableFieldException e) {
            System.out.println("  [REJECTED] Attempt to change placeOfBirth → \n             " + e.getMessage());
        }
        try {
            demoMgr.updateAttributes(idJohn, Map.of("address", "New Address"),
                    OrganisationType.CENTRAL_AUTHORITY);
        } catch (ValidationException e) {
            System.out.println("  [REJECTED] Update on REVOKED identity (John) → \n             " + e.getMessage());
        }
        try {
            demoMgr.updateAttributes(idJane, Map.of("address", "Hacked Address"),
                    OrganisationType.TAX_SERVICE);
        } catch (UnauthorisedActionException e) {
            System.out.println("  [REJECTED] Tax service attempts to update Jane → \n             " + e.getMessage());
        }

        // SCENARIO 6
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 6: Bank & Employer Portal Verification");
        System.out.println("  Returns VALID, INVALID, or NOT_FOUND");
        System.out.println(MID);

        printDemoResult("Bank verifies Jane (ACTIVE)",        demoBankP.verifyIdentity(idJane));
        printDemoResult("Bank verifies John (REVOKED)",       demoBankP.verifyIdentity(idJohn));
        printDemoResult("Employer verifies Ali (REVOKED)",    demoEmpP.verifyIdentity(idAli));
        printDemoResult("Bank verifies unknown ID",           demoBankP.verifyIdentity("UNKNOWN-ID-999"));

        demoMgr.changeStatus(idJane, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        printDemoResult("Bank verifies Jane (now SUSPENDED)", demoBankP.verifyIdentity(idJane));
        demoMgr.changeStatus(idJane, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        System.out.println("  [ACCEPTED] Jane reactivated for remaining scenarios");

        // SCENARIO 7
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 7: Tax Authority Period-Based Verification");
        System.out.println("  INVALID if suspended at any point during the period");
        System.out.println(MID);

        LocalDateTime pStart = LocalDateTime.now().minusHours(2);
        LocalDateTime pEnd   = LocalDateTime.now().plusHours(1);

        printDemoResult("Tax — Jane (always active in period)",  demoTaxP.verifyForPeriod(idJane, pStart, pEnd));

        String idTaxTest = demoMgr.create(Map.of(
                "fullName", "Tax Test Person",
                "dateOfBirth", LocalDate.of(1980, 6, 1),
                "placeOfBirth", "Cardiff"
        ), OrganisationType.CENTRAL_AUTHORITY);
        demoMgr.changeStatus(idTaxTest, IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        demoMgr.changeStatus(idTaxTest, IDStatus.ACTIVE, OrganisationType.CENTRAL_AUTHORITY);
        printDemoResult("Tax — suspended+reactivated in period", demoTaxP.verifyForPeriod(idTaxTest, pStart, pEnd));
        printDemoResult("Tax — unknown ID", demoTaxP.verifyForPeriod("UNKNOWN-999", pStart, pEnd));

        // SCENARIO 8
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 8: Driving Licence Eligibility Verification");
        System.out.println("  VALID only if ACTIVE and no temporary restriction");
        System.out.println(MID);

        printDemoResult("Driving — Jane (no restriction)", demoDriveP.verifyForLicence(idJane));
        demoMgr.setRestriction(idJane, true, OrganisationType.CENTRAL_AUTHORITY);
        printDemoResult("Driving — Jane (WITH restriction)", demoDriveP.verifyForLicence(idJane));
        demoMgr.setRestriction(idJane, false, OrganisationType.CENTRAL_AUTHORITY);
        printDemoResult("Driving — Jane (restriction removed)", demoDriveP.verifyForLicence(idJane));
        printDemoResult("Driving — Ali (REVOKED)", demoDriveP.verifyForLicence(idAli));
        printDemoResult("Driving — unknown ID", demoDriveP.verifyForLicence("UNKNOWN-999"));

        // SCENARIO 9
        System.out.println();
        System.out.println(TOP);
        System.out.println("  SCENARIO 9: Management Operations on Non-Existent IDs");
        System.out.println("  Status changes and updates on unknown IDs throw");
        System.out.println("  IDNotFoundException");
        System.out.println(MID);

        try {
            demoMgr.changeStatus("DOES-NOT-EXIST", IDStatus.SUSPENDED, OrganisationType.CENTRAL_AUTHORITY);
        } catch (IDNotFoundException e) {
            System.out.println("  [REJECTED] Status change on unknown ID → \n             " + e.getMessage());
        }
        try {
            demoMgr.updateAttributes("DOES-NOT-EXIST", Map.of("address", "Somewhere"),
                    OrganisationType.CENTRAL_AUTHORITY);
        } catch (IDNotFoundException e) {
            System.out.println("  [REJECTED] Update on unknown ID → \n             " + e.getMessage());
        }

        System.out.println();
        System.out.println("  Demo complete — all 9 scenarios finished.");
        pause();
    }

    private static void printDemoResult(String label, VerificationResult result) {
        System.out.printf("  [ACCEPTED] %-42s → %-11s — %s%n",
                label, result.status(), result.reason());
    }
}