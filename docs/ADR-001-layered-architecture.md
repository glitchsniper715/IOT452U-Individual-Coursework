# ADR-001 — Layered Architecture

## Context
The brief requires the system to have two separate capabilities: one for managing Digital IDs (writing) and one for consuming them (reading). I needed a structure that would enforce this cleanly rather than just relying on runtime checks, and that would also make each part of the system independently testable.

## Why I used it
I split the code into four layers: presentation, service, domain, and infrastructure. The service layer is further divided into `management/` and `consumption/` sub-packages, and neither is allowed to import the other. The dependency rule means each layer only depends on the one below it. So `IdentityManager` depends on the `IdentityRepository` interface, not on `InMemoryIdentityRepository` directly.

```
presentation/   →  Main.java (wires everything together)
service/
  management/   →  IdentityManager
  consumption/  →  VerificationService + portals
domain/         →  DigitalID, IDStatus, AuditEntry
infrastructure/ →  repository interfaces + in-memory implementations
authorisation/  →  AuthorisationService, OrganisationType
exception/      →  typed exception classes
```

## Consequences
Each layer can be tested without the others. IdentityManagerTest passes an InMemoryIdentityRepository directly and doesn't touch any consumption code. The package split means that if a consumption class accidentally tried to import IdentityManager, it would be immediately visible. 

The main trade-off is more packages and files than a flat structure would have, but this felt worth it since the brief 
explicitly requires the two capabilities to be separate.
