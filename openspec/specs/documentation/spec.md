# documentation Specification

## Purpose
TBD - created by archiving change openspec-initialization-with-english-locale. Update Purpose after archive.
## Requirements
### Requirement: OpenSpec Documentation Language

All OpenSpec documentation within the `openspec/` directory SHALL be written in English.

The system SHALL enforce this requirement through:
1. Documentation templates in English
2. AGENTS.md instructions stating the language requirement
3. Validation checks that encourage English documentation

Rationale: English documentation ensures international collaboration, AI assistant compatibility, and consistency across the specification system.

#### Scenario: Creating a new change proposal

- **GIVEN** a developer wants to create an OpenSpec change proposal
- **WHEN** they create files under `openspec/changes/<change-id>/`
- **THEN** all content (proposal.md, tasks.md, design.md, spec deltas) MUST be in English
- **AND** technical terminology may remain in original form (e.g., "PMX model format")

#### Scenario: Adding examples in specifications

- **GIVEN** a developer is writing specification scenarios
- **WHEN** they include code examples or documentation samples
- **THEN** the descriptive text and explanations MUST be in English
- **AND** code comments within examples may follow project conventions

#### Scenario: Reviewing existing proposals

- **GIVEN** a team member reviews an OpenSpec proposal
- **WHEN** they read any file under `openspec/`
- **THEN** the content SHALL be readable in English
- **AND** non-English content SHALL be considered a documentation violation

### Requirement: Language Requirement Scope

The English language requirement SHALL apply to all OpenSpec-related documentation, with clear boundaries for what is in-scope and out-of-scope.

#### Scenario: In-scope documentation

- **GIVEN** a file is located within the `openspec/` directory
- **WHEN** the file is one of: proposal.md, tasks.md, design.md, spec.md
- **THEN** the English language requirement applies
- **AND** all content MUST be in English

#### Scenario: Out-of-scope content

- **GIVEN** a developer writes content outside the `openspec/` directory
- **WHEN** the content is: source code comments, README files, user guides, commit messages
- **THEN** the English language requirement does NOT apply
- **AND** developers may use the project's natural language

#### Scenario: Mixed-language scenarios

- **GIVEN** a proposal discusses code that uses non-English comments
- **WHEN** the proposal references or quotes code
- **THEN** the proposal text MUST be in English
- **AND** code quotes may preserve original language for accuracy

