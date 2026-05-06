# ADR-002 — Repository Pattern for Identity Storage

## Context
`IdentityManager` needs to save and retrieve `DigitalID` objects, and `VerificationService` needs to query the audit log. I could have created the storage objects directly inside those classes, but that would have coupled the business logic tightly to a specific storage implementation and made testing much harder.

## Why I used it
I defined `IdentityRepository` and `AuditRepository` as interfaces in the `infrastructure/` package. `IdentityManager` and `VerificationService` both depend on these interfaces, not on the concrete implementations. The actual `InMemoryIdentityRepository` and `InMemoryAuditRepository` are only created in `Main.java` and passed in via constructors.

``` java
// Only Main.java names the concrete classes
IdentityRepository repo = new InMemoryIdentityRepository();
AuditRepository auditRepo = new InMemoryAuditRepository();
IdentityManager manager = new IdentityManager(repo, authService, auditRepo);
```

## Consequences
Tests can instantiate `InMemoryIdentityRepository` directly. The in-memory implementations are lightweight enough to work as test fakes on their own. If a real database were needed in future, I'd write a new implementation class and change one line in `Main.java`; `IdentityManager` wouldn't need to change at all. The only real downside is two files per stored entity (interface + implementation), which adds a bit of boilerplate for a project of this size.