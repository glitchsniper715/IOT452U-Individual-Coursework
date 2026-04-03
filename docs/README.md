# IOT452U-Individual-Coursework

## Section 1: Project Overview 
2 paragraphs describing the system and scenario

## Section 2: System Architecture 
copy your folder structure, briefly explain each package
Layered architecture
Each package has a single responsibility

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

## Section 3: Design Patterns Used 
list each pattern with a one-sentence justification

## Section 4: How to Run 
step-by-step commands (python -m pytest, python main.py)

## Section 5: GitHub Repository Link
https://github.com/sm325om/IOT452U-Individual-Coursework

## Section 6: References 
APA format, at least 3 sources