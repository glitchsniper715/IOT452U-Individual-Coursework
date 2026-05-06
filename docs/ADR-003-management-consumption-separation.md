# ADR-003 — Strict Separation of Management and Consumption Capabilities

## Context
The brief explicitly states that managing Digital IDs (creating, updating, changing status) and verifying them are two distinct capabilities, and that consuming organisations must never be able to call management operations. I wanted this to be enforced by the structure of the code, not just by the authorisation checks at runtime.

## Why I used it
The service layer is split into two sub-packages `service/management/` and `service/consumption/` with a strict rule that neither imports the other. `Main.java` is the only place where both sides are wired together using the same shared repositories.

Within the consumption sub-package I also used the Facade pattern: each organisation type gets its own portal class (`BankPortal`, `TaxAuthorityPortal`, etc.) that only exposes the specific verification method that organisation needs. So a `BankPortal` can't accidentally call a tax history query because that method simply doesn't exist on `BankPortal`.

```
service/
├── management/    ← IdentityManager only
└── consumption/   ← VerificationService + one portal per org type
```

## Consequences
The separation is structurally verifiable, checking for cross-package imports is enough to confirm it holds. Each sub-package also tests independently: 
`IdentityManagerTest` has no consumption imports and `VerificationServiceTest` has no management imports. 

The main trade-off is that `VerificationService` needs to receive the same repository instances as `IdentityManager`, which means `Main.java` has to create them first and pass them to both, but that's a deliberate design choice rather than a problem.