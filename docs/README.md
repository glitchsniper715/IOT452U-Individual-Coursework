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

## Section 3: Design Patterns Used 
### Repository Pattern
IdentityManager depends on the IdentityRepository and AuditRepository interfaces, not on the concrete InMemory implementations. This separates storage from business logic and means the backing store can be swapped without changing any service code.

### Dependency Injection
IdentityManager receives all three of its dependencies through its constructor rather than creating them internally. This makes the class independently testable and is what allows the Repository Pattern to work in practice.

### Domain Model
DigitalID is not a passive data holder. It owns and enforces its own business rules: the status transition state machine and the internal audit log both live inside the domain object itself, keeping business logic out of the service layer.

## Section 4: How to Run 
step-by-step commands

## Section 5: GitHub Repository Link
https://github.com/glitchsniper715/IOT452U-Individual-Coursework