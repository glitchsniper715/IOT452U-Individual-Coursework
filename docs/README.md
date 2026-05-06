# IOT452U-Individual-Coursework

## Section 1: Project Overview 
Describing the system and scenario

## Section 2: System Architecture
Layered architecture: Each package has a single responsibility

Packages:
- Domain
  - It contains the data and its rules
  - Ensures the business logic remains independent of external concerns
- Service (business logic layer)
  - Where the system actually does things
  - management and consumption
  - It enforces system rules
- Authorisation
  - Contains permission checks, validations, and access controls
  - Ensures only certain accounts can perform specific operations
- Exception
  - Custom exceptions to improve error clarity and allows the system to handle invalid operations
- Infrastructure
  - Where data storage and external things live
  - Hash-map stored here
  - in-memory storage
  - It helps to isolate data persistence from business logic (allows the system to be modified without impacting core functionality)
- Presentation (interface layer)
  - Console input/output
  - Handles user interactions and delegates operations to the service layer
  - Ensures separation of concern
  
### Class Diagram
![Class Diagram](class_diagram.png)

## Organisation Portals

The system provides organisation-specific portals that implement the consumption capability. Each portal exposes only the verification functionality appropriate to its role.

| Portal               | Organisation Type | Verification Type                          |
|----------------------|-------------------|--------------------------------------------|
| BankPortal           | BANK              | Basic (ACTIVE/INVALID)                     |
| EmployerPortal       | EMPLOYER          | Basic (ACTIVE/INVALID)                     |
| TaxAuthorityPortal   | TAX_SERVICE       | Historical (period-based suspension check) |
| DrivingLicencePortal | DRIVING_AUTHORITY | Eligibility (ACTIVE + no restrictions)     |

## Section 3: Design Patterns Used 
### Repository Pattern
IdentityManager depends on the IdentityRepository and AuditRepository interfaces, not on the concrete InMemory implementations. This separates storage from business logic and means the backing store can be swapped without changing any service code.

### Dependency Injection
IdentityManager receives all three of its dependencies through its constructor rather than creating them internally. This makes the class independently testable and is what allows the Repository Pattern to work in practice.

### Domain Model
DigitalID is not a passive data holder. It owns and enforces its own business rules: the status transition state machine and the internal audit log both live inside the domain object itself, keeping business logic out of the service layer.

## Section 4: How to Run 
### Requirements
- Java 17 or later
- Maven 3.8 or later

Check your versions before starting:
```
java -version
mvn -version
```

### Step 1 — Clone the repository
```
git clone https://github.com/glitchsniper715/IOT452U-Individual-Coursework.git
cd IOT452U-Individual-Coursework
```

### Step 2 — Run all tests
```
mvn test
```
All tests should pass, and you will see a `BUILD SUCCESS` message. JaCoCo will also generate a coverage report at 
`target/site/jacoco/index.html` — open that file in a browser to see line-by-line coverage.

### Step 3 — Run the demonstration
```
mvn exec:java -Dexec.mainClass=com.digitalid.presentation.Main
```
This runs `Main.java`, which executes nine scenarios covering every system capability. Each line of output is labelled `[ACCEPTED]` or `[REJECTED]` so you can see the system behaving correctly in both the happy path and the error cases.

### Expected output (first few lines)
```
╔══════════════════════════════════════════════════════════════╗
║  SCENARIO 1: Creating Digital IDs                            ║
║  Only CENTRAL_AUTHORITY may create identities                ║
╚══════════════════════════════════════════════════════════════╝
[ACCEPTED] Central Authority creates Jane Smith → ID: DIG-XXXXX
[ACCEPTED] Central Authority creates John Doe  → ID: DIG-XXXXX
[ACCEPTED] Central Authority creates Ali Hassan → ID: DIG-XXXXX
```
The UUID suffix in each ID will differ on every run — that is expected behaviour.


## Section 5: GitHub Repository Link
https://github.com/glitchsniper715/IOT452U-Individual-Coursework