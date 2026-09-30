# CodeCommit AI - Architecture

## 1. Architecture Overview

CodeCommit AI follows a modular client-server architecture.

```text
React Frontend
      |
      | HTTPS / REST
      v
Spring Boot Backend
      |
      +------ PostgreSQL + pgvector
      |
      +------ GitHub API
      |
      +------ LLM / Embedding Provider

The system consists of:

- React frontend
- Spring Boot backend
- PostgreSQL with pgvector
- GitHub API integration
- LLM provider
- Embedding provider

## 2. Architecture Style

CodeCommit AI uses a modular monolith architecture.

The backend runs as one Spring Boot application with clear internal module boundaries.

Initial modules:

- auth
- repository
- github
- indexing
- search
- ai
- analysis
- common


## 3. Architectural Principles

The project follows:

1. Separation of concerns
2. SOLID principles
3. Dependency inversion
4. Secure-by-default design
5. Testability
6. Maintainability
7. Incremental development
8. Minimal unnecessary abstraction

## 4. Technology Stack

### Backend

- Java 21
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Maven

### Database

- PostgreSQL
- pgvector

### Frontend

- React
- TypeScript
- Vite
- Tailwind CSS

### Infrastructure

- Docker
- Docker Compose

### External Systems

- GitHub API
- LLM provider
- Embedding provider


## 5. Backend Package Structure

The backend package root is:

com.codecommitai

The main application class is:

CodeCommitAiApplication

The initial backend structure is:

backend/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── codecommitai/
    │   │           ├── CodeCommitAiApplication.java
    │   │           ├── auth/
    │   │           ├── repository/
    │   │           ├── github/
    │   │           ├── indexing/
    │   │           ├── search/
    │   │           ├── ai/
    │   │           ├── analysis/
    │   │           └── common/
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/
    │
    └── test/

Controllers should remain thin.

Business logic belongs primarily in services.

Database access belongs in repositories.

External integrations belong behind dedicated clients or services.

## 6. Backend Request Flow

A typical API request follows:

Client
  |
  v
Controller
  |
  v
Validation
  |
  v
Service
  |
  +---- Repository
  |
  +---- External Client
  |
  v
Database / External System
  |
  v
DTO Response
  |
  v
Client

Controllers should not contain complex business logic.

Services contain application and business logic.

Repositories handle persistence.

DTOs define API contracts.



## 7. Authentication Architecture

Authentication uses Spring Security and JWT.

### Registration Flow

Client
  |
  v
Auth Controller
  |
  v
Auth Service
  |
  v
Password Hashing
  |
  v
User Repository
  |
  v
PostgreSQL

Passwords must never be stored in plain text.

BCrypt will be used for password hashing.

### Login Flow

Client
  |
  v
Login API
  |
  v
Validate Credentials
  |
  v
Generate JWT
  |
  v
Return JWT

### Protected Request

Client
  |
  | Authorization: Bearer JWT
  v
Spring Security Filter
  |
  v
JWT Validation
  |
  v
Authenticated Request
  |
  v
Controller

Authentication and authorization must be handled before protected business operations are executed.

## 8. Repository Architecture

A repository belongs to a user.

User
 |
 +--- Repository
        |
        +--- RepositoryVersion

A repository contains metadata such as:

- id
- userId
- name
- fullName
- githubUrl
- defaultBranch
- description
- language
- status
- createdAt
- updatedAt

Possible repository states:

- CONNECTED
- INDEXING
- READY
- FAILED

## 9. Repository Version Architecture

Every indexing operation is associated with a specific repository commit.

Repository
    |
    +--- RepositoryVersion
            |
            +--- commitSha
            +--- branchName
            +--- createdAt

The commit SHA identifies the exact source version that was indexed.

This prevents ambiguity about which version of a repository produced the indexed data.




## 10. GitHub Integration Architecture

GitHub communication is isolated from the core business logic.

The flow is:

Repository Service
       |
       v
GitHub Service
       |
       v
GitHub Client
       |
       v
GitHub API

The GitHub module is responsible for:

- Repository information
- Branch information
- Commit information
- Repository file retrieval
- GitHub authentication
- API error handling
- Rate-limit handling

Other modules should not directly call GitHub APIs.

## 11. Repository Indexing Architecture

Repository indexing is the core processing pipeline of CodeCommit AI.

The high-level pipeline is:

GitHub Repository
       |
       v
Repository Fetcher
       |
       v
File Scanner
       |
       v
File Filter
       |
       v
Language Detection
       |
       v
Code Parser
       |
       v
Symbol Extraction
       |
       v
Semantic Chunker
       |
       v
Embedding Generator
       |
       v
PostgreSQL + pgvector

Each stage should have a clearly defined responsibility.

## 12. Indexing Stages

### Stage 1 - Fetch

Retrieve the selected repository version from GitHub.

The system should identify:

- Repository
- Branch
- Commit SHA

### Stage 2 - Scan

Identify files contained in the repository.

### Stage 3 - Filter

Exclude files that should not be indexed.

Examples include:

- .git
- node_modules
- Build output
- Compiled binaries
- Images
- Videos
- Archives
- Temporary files
- Dependency caches
- Generated files

The filtering rules should be configurable.

### Stage 4 - Language Detection

Determine the programming language of each supported source file.

Java is the first-class language for the initial implementation.

Additional languages can be supported later.

### Stage 5 - Parse

Parse supported source files to understand their structure.

The parser should identify:

- Classes
- Interfaces
- Enums
- Methods
- Functions
- Constructors
- Fields
- Imports
- Packages

### Stage 6 - Symbol Extraction

Extract structured symbols from parsed source code.

Each symbol should retain:

- Name
- Symbol type
- Fully qualified name
- File
- Start line
- End line
- Parent symbol

### Stage 7 - Semantic Chunking

Source code should be divided into meaningful chunks.

The system should prefer structural boundaries such as:

- Classes
- Methods
- Interfaces
- Logical code sections

Arbitrary fixed-size splitting should not be the only strategy.

### Stage 8 - Embedding

Generate vector embeddings for code chunks.

The embedding provider should be configurable.

### Stage 9 - Storage

Store:

- Repository metadata
- Repository versions
- Files
- Symbols
- Chunks
- Embeddings
- Indexing job information

### Stage 10 - Completion

When all stages finish successfully, the repository version is marked as indexed and ready.


## 13. Indexing Job Architecture

Repository indexing can be a long-running operation.

The HTTP request should not remain open until indexing finishes.

The API should create an indexing job and start background processing.

The flow is:

POST /api/v1/repositories/{id}/index
              |
              v
       Create IndexingJob
              |
              v
       Start Processing
              |
              v
       Return Job Status
              |
              v
       Background Pipeline
              |
              v
       Update Job Progress

The initial implementation may use Spring asynchronous processing.

A dedicated queue or worker architecture can be introduced later if repository size and workload require it.

## 14. Indexing Job States

Possible states:

- PENDING
- CLONING
- SCANNING
- PARSING
- CHUNKING
- EMBEDDING
- COMPLETED
- FAILED

The indexing job tracks:

- id
- repositoryId
- repositoryVersionId
- status
- totalFiles
- processedFiles
- totalChunks
- processedChunks
- currentStage
- errorMessage
- startedAt
- completedAt

## 15. File Architecture

Each indexed file belongs to a repository version.

RepositoryVersion
       |
       +--- File

File metadata includes:

- id
- path
- name
- extension
- language
- size
- contentHash
- createdAt

The system should avoid unnecessary duplication of large source content.

## 16. Symbol Architecture

File
 |
 +--- CodeSymbol

A symbol contains:

- id
- fileId
- name
- symbolType
- fullyQualifiedName
- startLine
- endLine
- parentSymbolId

Supported symbol types initially include:

- Class
- Interface
- Enum
- Method
- Constructor
- Field
- Function

## 17. Code Chunk Architecture

File
 |
 +--- CodeChunk
        |
        +--- Embedding

Each code chunk should retain enough metadata to locate its original source.

Example:

File:
src/main/java/com/example/payment/PaymentService.java

Symbol:
processPayment()

Start line:
25

End line:
68

The chunk should also retain information about:

- Repository version
- File
- Symbol
- Chunk index
- Token count
- Source line range

## 18. Embedding Architecture

Embedding generation should be abstracted behind a service.

Conceptually:

EmbeddingService
       |
       +--- Embedding Provider

The rest of the application should not depend directly on one specific AI provider.

The provider and embedding model should be configurable.

## 19. Vector Storage

PostgreSQL is the primary database.

pgvector provides vector storage and similarity search.

The embedding vector dimension must match the selected embedding model.

The dimension should be configuration-driven rather than an arbitrary hard-coded value.

Conceptual relationship:

CodeChunk
    |
    v
Embedding
    |
    v
pgvector vector column



## 20. Search Architecture

CodeCommit AI will support multiple search strategies.

### Keyword Search

Keyword search handles exact or text-based searches such as:

- Class names
- Method names
- File names
- Variable names
- Code fragments
- Text

Example:

"PaymentService"

### Semantic Search

Semantic search allows users to search by meaning rather than exact words.

Example:

"Where is payment validation performed?"

The system should be able to find relevant code even when the exact phrase does not appear.

### Hybrid Search

Hybrid search combines:

- Keyword relevance
- Semantic similarity
- Structural relevance

Conceptual flow:

User Query
     |
     +---- Keyword Search
     |
     +---- Semantic Search
     |
     +---- Structural Signals
     |
     v
  Ranking
     |
     v
Final Search Results

Potential ranking signals include:

- Keyword relevance
- Vector similarity
- Symbol relevance
- File relevance
- Exact identifier matches

The ranking logic should be independently testable.

## 21. RAG Architecture

AI questions about a repository use Retrieval-Augmented Generation.

The flow is:

User Question
      |
      v
Query Processing
      |
      v
Retriever
      |
      v
Candidate Code Chunks
      |
      v
Ranking
      |
      v
Context Builder
      |
      v
Prompt Builder
      |
      v
LLM
      |
      v
Answer Generator
      |
      v
Source References

The LLM should receive relevant repository context instead of the entire repository.

## 22. RAG Retrieval Context

Each retrieved code chunk should contain metadata.

Example:

Repository:
CodeCommit-AI

File:
src/main/java/com/example/payment/PaymentService.java

Symbol:
processPayment()

Start Line:
25

End Line:
68

Code:
<retrieved source code>

The context builder should format retrieved chunks clearly so the LLM can distinguish different source locations.

The retrieval system should prefer relevant and diverse chunks instead of simply returning the nearest vectors.

The system should enforce reasonable context limits before sending information to the LLM.



## 23. AI Response Architecture

The AI response layer is responsible for converting retrieved repository context into a useful and grounded answer.

The conceptual flow is:

Retrieved Context
      |
      v
Prompt Construction
      |
      v
LLM Request
      |
      v
LLM Response
      |
      v
Response Validation
      |
      v
Source Mapping
      |
      v
API Response

The AI layer should not directly access the database.

Instead, it should receive structured context from the retrieval layer.

The AI response should contain:

- Natural language answer
- Relevant source references
- File paths
- Symbol names when available
- Line ranges when available
- Optional confidence or relevance metadata

The AI service should remain independent from the HTTP layer.

This allows the same AI functionality to be reused by:

- REST APIs
- Background jobs
- Future CLI tools
- Future integrations

## 24. Source Grounding Architecture

CodeCommit AI should prioritize grounded answers.

Every factual statement about repository code should be supported by retrieved repository context whenever possible.

The system should preserve the relationship between:

Repository
    |
    v
Repository Version
    |
    v
File
    |
    v
Symbol
    |
    v
Code Chunk
    |
    v
AI Answer

Source references should identify the original location of the information.

Example:

Source:
PaymentService.java

Lines:
25-68

Symbol:
processPayment()

The response layer should return structured source information rather than only plain text.

Example conceptual response:

{
  "answer": "...",
  "sources": [
    {
      "file": "src/main/java/.../PaymentService.java",
      "symbol": "processPayment",
      "startLine": 25,
      "endLine": 68
    }
  ]
}

The system should avoid presenting unsupported assumptions as repository facts.

If sufficient repository context cannot be retrieved, the AI layer should clearly indicate that the available context is insufficient.

Source grounding is a core reliability requirement of CodeCommit AI.



## 25. Code Explorer Architecture

The Code Explorer provides a structured way to navigate indexed repositories.

The explorer should allow users to browse:

- Repository versions
- Directories
- Files
- Classes
- Interfaces
- Methods
- Functions
- Other extracted symbols

Conceptual structure:

Repository
    |
    +-- Directory
    |     |
    |     +-- File
    |           |
    |           +-- Class
    |           |     |
    |           |     +-- Method
    |           |
    |           +-- Function
    |
    +-- Other Files

The Code Explorer should use indexed metadata rather than repeatedly scanning the GitHub repository.

When a user opens a file, the system should be able to provide:

- File metadata
- Source content
- Language
- Symbols
- Line information
- Related code chunks

## 26. Dependency Analysis Architecture

Dependency analysis identifies relationships between elements of a codebase.

Examples include:

- Class A depends on Class B
- Service A calls Service B
- Controller A uses Service B
- Module A imports Module B
- Method A calls Method B

The dependency system should build a graph representation.

Conceptual model:

Node
    |
    +-- Class
    +-- Interface
    +-- Method
    +-- Function
    +-- Module

Edges represent relationships such as:

- IMPORTS
- EXTENDS
- IMPLEMENTS
- CALLS
- USES
- DEPENDS_ON

The dependency graph should be derived from parsed source code and symbol relationships.

## 27. Impact Analysis Architecture

Impact analysis determines which parts of a repository may be affected by a change.

Example:

Developer changes:

PaymentService.processPayment()

The system may identify:

PaymentController
        |
        v
PaymentService.processPayment()
        |
        +---- PaymentRepository
        |
        +---- PaymentValidator

The analysis should consider:

- Direct dependencies
- Reverse dependencies
- Method calls
- Class relationships
- Imports
- Module relationships

Impact analysis should provide structured results such as:

- Directly affected symbols
- Indirectly affected symbols
- Related files
- Dependency paths

The analysis should clearly distinguish confirmed relationships from heuristic relationships.

## 28. Database Architecture

PostgreSQL is the primary persistent data store.

pgvector is used for vector similarity search.

The database is responsible for storing:

- User data
- Repository metadata
- Repository versions
- Files
- Symbols
- Code chunks
- Embeddings
- Indexing jobs
- Dependency relationships

Conceptual database structure:

PostgreSQL
    |
    +-- Users
    |
    +-- Repositories
    |
    +-- Repository Versions
    |
    +-- Files
    |
    +-- Symbols
    |
    +-- Code Chunks
    |
    +-- Embeddings
    |
    +-- Indexing Jobs
    |
    +-- Dependencies

Large source content should be handled carefully to avoid unnecessary duplication.

Database access should be isolated behind repository/data-access components.

Business logic should not contain raw database queries unless there is a justified performance requirement.

Database migrations should be version controlled and applied consistently across environments.




## 29. Frontend Architecture

The frontend will be implemented using React and TypeScript.

The frontend communicates with the backend exclusively through REST APIs.

Conceptual structure:

React Application
      |
      +-- Pages
      |
      +-- Components
      |
      +-- Hooks
      |
      +-- API Clients
      |
      +-- State Management
      |
      +-- Utilities
      |
      v
Spring Boot REST API

The frontend should separate:

- Presentation logic
- Application state
- API communication
- Reusable UI components
- Domain-specific functionality

Major frontend areas include:

- Authentication
- Dashboard
- Repository management
- Code Explorer
- Search
- AI Chat
- Dependency Graph
- Impact Analysis

The frontend must never contain backend secrets such as:

- Database credentials
- JWT signing secrets
- GitHub client secrets
- LLM API keys

## 30. API Architecture

The backend exposes versioned REST APIs.

Base path:

/api/v1

API responsibilities include:

- Authentication
- Repository management
- Indexing
- Search
- AI questions
- Dependency analysis
- Impact analysis

Controllers should remain thin.

A typical request flow is:

HTTP Request
     |
     v
Controller
     |
     v
DTO Validation
     |
     v
Application Service
     |
     v
Domain Logic
     |
     v
Repository / External Service
     |
     v
Response DTO
     |
     v
HTTP Response

Controllers should not contain complex business logic.

## 31. DTO Architecture

Data Transfer Objects are used to define API boundaries.

DTOs should be separated from persistence entities.

Examples:

- RegisterRequest
- LoginRequest
- LoginResponse
- RepositoryCreateRequest
- RepositoryResponse
- SearchRequest
- SearchResponse
- AskRequest
- AskResponse
- SourceReferenceResponse

DTOs should expose only information required by the client.

Database entities should not be returned directly from controllers.

Validation should be applied to incoming request DTOs.

Examples include:

- Required fields
- String length
- Valid URLs
- Valid identifiers
- Numeric boundaries

## 32. Error Handling Architecture

The backend should use centralized exception handling.

Spring's global exception handling mechanism should convert application exceptions into consistent API responses.

Conceptual flow:

Exception
    |
    v
Global Exception Handler
    |
    v
Standard Error Response

The error response should contain useful information such as:

- Timestamp
- HTTP status
- Error code
- Human-readable message
- Request path
- Optional validation details

Example:

{
  "timestamp": "...",
  "status": 404,
  "code": "REPOSITORY_NOT_FOUND",
  "message": "Repository not found",
  "path": "/api/v1/repositories/123"
}

Internal implementation details and sensitive information must not be exposed to clients.

## 33. Configuration Architecture

Application configuration should be externalized.

Environment-specific values should not be hard-coded.

Examples:

DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
GITHUB_CLIENT_ID
GITHUB_CLIENT_SECRET
LLM_API_KEY
EMBEDDING_API_KEY

Configuration should be organized by environment where appropriate.

Example environments:

- Local development
- Test
- Staging
- Production

Secrets should be provided through secure environment configuration or a secret-management system.

A safe `.env.example` file may document required configuration variables without containing real secrets.

The application should fail clearly when mandatory configuration is missing.




## 34. Docker Architecture

Docker will provide consistent development and deployment environments.

The initial local environment should support:

- Backend application
- Frontend application
- PostgreSQL
- pgvector

Conceptual structure:

Docker Compose
      |
      +-- Backend
      |
      +-- Frontend
      |
      +-- PostgreSQL + pgvector

The database should use a persistent Docker volume so development data is not lost when containers restart.

Environment variables should be supplied through environment configuration rather than hard-coded inside Docker images.

Docker configuration should support reproducible local development.

## 35. Deployment Architecture

The application should be designed so individual components can be deployed independently when required.

Initial deployment model:

Client
  |
  v
Frontend
  |
  v
Backend API
  |
  +---- PostgreSQL + pgvector
  |
  +---- GitHub API
  |
  +---- LLM Provider
  |
  +---- Embedding Provider

The production environment should use:

- HTTPS
- Secure secrets
- Production database credentials
- Restricted network access
- Proper logging
- Health checks
- Resource limits

The exact cloud provider and infrastructure services may be selected during the deployment phase.

## 36. Security Boundaries

Security boundaries should exist between:

- Browser and backend
- Backend and database
- Backend and GitHub
- Backend and AI providers
- Users and repositories

Authentication establishes user identity.

Authorization determines whether a user can access a specific repository or resource.

The backend must never trust authorization decisions made only by the frontend.

Every protected resource should validate ownership or appropriate permissions on the server.

External API credentials must remain server-side.

User-controlled input should be validated and sanitized where appropriate.

## 37. Observability Architecture

The system should provide enough observability to diagnose failures and monitor important operations.

The application should support:

- Structured logging
- Error logging
- Request tracing
- Indexing job status
- Health checks
- Important performance metrics

Important operations to observe include:

- Authentication
- Repository synchronization
- Indexing
- Embedding generation
- Search
- AI requests
- Dependency analysis

Logs must not expose:

- Passwords
- API keys
- JWT secrets
- Sensitive user information
- Authorization tokens

## 38. Testing Architecture

Testing should exist at multiple levels.

### Unit Tests

Unit tests validate isolated components such as:

- Services
- Validators
- Parsers
- Chunking logic
- Ranking logic
- Dependency analysis

### Integration Tests

Integration tests validate interactions between components.

Examples:

- Database repositories
- REST APIs
- GitHub integration
- Indexing pipeline
- Search pipeline

### End-to-End Tests

The main end-to-end workflow should cover:

Register
    |
    v
Login
    |
    v
Add GitHub Repository
    |
    v
Start Indexing
    |
    v
Index Completed
    |
    v
Search Code
    |
    v
Ask AI Question
    |
    v
Receive Answer + Sources

AI-related tests should avoid depending exclusively on exact generated text.

Retrieval quality and source grounding should also be tested.

## 39. Future Scalability Architecture

The initial system should remain a modular monolith.

The architecture should nevertheless allow future extraction of independently scalable services.

Potential future services include:

- Repository Service
- Indexing Service
- Search Service
- AI Service
- Analysis Service

A possible future architecture:

API Gateway
     |
     +---- Repository Service
     |
     +---- Indexing Service
     |
     +---- Search Service
     |
     +---- AI Service
     |
     +---- Analysis Service

This separation should only be introduced when scale, team structure, deployment requirements, or operational complexity justify it.

The initial implementation should avoid premature microservice complexity.



## 40. Architecture Decision Rules

The following rules should guide implementation decisions throughout the project.

### Rule 1: Prefer Simplicity

Choose the simplest design that satisfies the current requirement.

Avoid unnecessary abstractions, frameworks, services, and dependencies.

### Rule 2: Preserve Module Boundaries

Each backend module should have a clearly defined responsibility.

Modules should communicate through well-defined interfaces and application services.

### Rule 3: Keep Business Logic Independent

Business logic should not depend directly on:

- HTTP controllers
- Database implementation details
- Specific AI providers
- Specific GitHub client implementations

External integrations should be isolated behind interfaces where practical.

### Rule 4: Avoid Premature Optimization

Optimize based on measured performance problems.

Correctness, maintainability, and testability should come first.

### Rule 5: Prefer Explicit Data Flow

Important processing pipelines should be easy to understand.

For example:

Repository
    |
    v
Indexing
    |
    v
Parsing
    |
    v
Chunking
    |
    v
Embedding
    |
    v
Storage
    |
    v
Retrieval
    |
    v
RAG
    |
    v
AI Response

### Rule 6: Protect External Boundaries

External APIs and infrastructure components should be isolated behind dedicated clients or adapters.

Examples:

- GitHubClient
- LlmClient
- EmbeddingClient

This allows providers to be replaced without rewriting business logic.

### Rule 7: Make Important Logic Testable

Important algorithms should be independently testable.

Examples:

- Chunking
- Search ranking
- Dependency analysis
- Impact analysis
- Query processing

### Rule 8: Do Not Duplicate Responsibilities

A responsibility should have one clear owner.

For example, authentication decisions belong to the security/authentication layer rather than individual controllers.

## 41. Architecture Source of Truth

The architecture of CodeCommit AI is defined primarily by the project documentation and the actual implementation.

The following documents form the architectural source of truth:

- `docs/PROJECT_SPEC.md`
- `docs/ARCHITECTURE.md`
- `docs/DEVELOPMENT_PLAN.md`

The project specification defines:

- What the product should do
- Functional requirements
- User workflows
- Core features

The architecture document defines:

- How the system should be structured
- Module boundaries
- Data flow
- Integration boundaries
- Infrastructure expectations

The development plan defines:

- Implementation phases
- Development order
- Milestones
- Completion criteria

If implementation decisions conflict with these documents, the conflict should be identified before making a significant architectural change.

The architecture may evolve as the project develops.

However, significant architectural changes should be intentional, documented, and justified.

CodeCommit AI should prioritize:

- Correctness
- Security
- Maintainability
- Testability
- Scalability
- Clear architecture
- Developer experience

This document should be updated when major architectural decisions change.