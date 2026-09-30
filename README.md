\# CodeCommit AI



AI-powered codebase understanding and analysis platform.



\## Project Status



🚧 Under Development



\## Overview



CodeCommit AI helps developers understand, search, analyze, and navigate large software codebases using AI-powered code intelligence.



\## Core Features



\- GitHub repository integration

\- Repository indexing

\- Code parsing and symbol extraction

\- Semantic code search

\- Hybrid keyword + semantic search

\- AI-powered codebase chat

\- Retrieval-Augmented Generation (RAG)

\- Source references for AI answers

\- Dependency analysis

\- Impact analysis

\- Code and method explanations



\## Technology Stack



\### Backend

\- Java 21

\- Spring Boot

\- Spring Security

\- Spring Data JPA

\- Maven



\### Frontend

\- React

\- TypeScript

\- Vite

\- Tailwind CSS



\### Database

\- PostgreSQL

\- pgvector



\### AI

\- Large Language Model

\- Embeddings

\- RAG



\### Infrastructure

\- Docker

\- Docker Compose

\- GitHub API



\## Architecture



```text

React Frontend

&#x20;     |

&#x20;     v

Spring Boot REST API

&#x20;     |

&#x20;     +-------------------+

&#x20;     |                   |

&#x20;     v                   v

PostgreSQL + pgvector   GitHub API

&#x20;     |

&#x20;     v

Code Intelligence

&#x20;     |

&#x20;     +-- Indexing

&#x20;     +-- Search

&#x20;     +-- RAG

&#x20;     +-- Dependency Analysis

&#x20;     +-- Impact Analysis

Incremental indexing test.