# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Build and Run
- `sbt compile` - Compile all modules
- `sbt runServer` - Run the main application server (shortcuts to endpoints-runner)
- `sbt "project endpoints-runner" run` - Run the main application directly
- `sbt test` - Run all tests across modules
- `sbt "project <module-name>" test` - Run tests for a specific module
- `sbt scalafmtAll` - Format all Scala code using scalafmt
- `sbt clean` - Clean build artifacts

### Docker
- `sbt "project endpoints-runner" docker:publishLocal` - Build Docker image locally

### Development Workflow
- `sbt compile` - Compile all modules before development
- `sbt scalafmtAll` - Format code (run before commits)
- `sbt test` - Run full test suite
- `sbt "project <module>" clean compile test` - Clean build and test specific module

## Architecture Overview

This is a modular Scala application built with SBT, structured as a microservices platform with layered architecture:

### Core Structure
- **Scala 2.13** with **Cats Effect** for functional programming
- **SBT multi-module** project with clear separation of concerns
- **HTTP4s** for web services, **Skunk** for PostgreSQL, **Redis4Cats** for caching

### Module Organization

#### Main Application (`endpoints/`)
- `00-domain` - Core domain models and business logic
- `01-repos` - Database repositories and SQL queries  
- `02-core` - Services layer with business logic and integrations
- `03-api` - HTTP API routes and endpoints
- `03-jobs` - Background job processing
- `04-server` - HTTP server configuration
- `05-runner` - Main application entry point

#### Supporting Libraries (`supports/`)
- `database/` - Database connectivity (Skunk + Flyway migrations)
- `services/` - HTTP4s server utilities and health endpoints
- `jobs/` - Cron job scheduling with fs2-cron
- `redis/` - Redis client configuration
- `sttp/` - HTTP client utilities
- `logback/` - Logging and error notification

#### Integrations (`integrations/`)
- `github/` - GitHub API client for commit data and repository operations
- `telegram/` - Telegram Bot API client for notifications
- `anthropic/` - Anthropic AI API client for Claude integration
- `aws/s3/` - S3 integration for file storage

#### Shared (`common/`)
- Common utilities, syntax extensions, domain types, and effects

#### Testing (`test-tools/`)
- Shared test utilities, generators, and test cases using Weaver

### Key Technologies
- **Database**: PostgreSQL with Skunk (functional SQL client)
- **Migration**: Flyway for database migrations
- **HTTP**: HTTP4s with Ember server/client
- **JSON**: Circe for JSON encoding/decoding
- **Validation**: Refined types for compile-time validation
- **Config**: PureConfig for type-safe configuration
- **Jobs**: fs2-cron for scheduled background tasks
- **Testing**: Weaver test framework with ScalaCheck generators

### Configuration
- Configuration files in `endpoints/05-runner/src/main/resources/reference.conf`
- Environment-based configuration using PureConfig
- Database migrations in `endpoints/01-repos/src/main/resources/db/migration/`

### Running the Application
The main entry point is `teamflow.Main` which:
1. Loads environment configuration  
2. Starts HTTP server module
3. Optionally starts background jobs module
4. Runs both concurrently using Cats Effect fibers

## Development Patterns

### Domain Layer (`00-domain`)
- Uses newtype for type-safe IDs (e.g., `UserId`, `ProjectId`)
- Enums for business states using Enumeratum
- Input/Filter classes for API and query operations
- JSON codecs with Circe

### Repository Layer (`01-repos`) 
- Skunk for type-safe SQL queries
- SQL fragments for dynamic query building
- Soft delete pattern (using `deleted_at` fields)
- Database codecs in `sql/package.scala`

### Service Layer (`02-core`)
- Business logic and validation
- Error handling with `AError` for user-facing errors
- ID generation using `ID.make[F, EntityId]` pattern
- Integration with external APIs (GitHub, Anthropic, etc.)

### API Layer (`03-api`)
- HTTP4s routes with authentication middleware
- Request/response JSON encoding/decoding
- Language-based error messages
- Authorization based on user roles and privileges

## Key Conventions

### ID Generation
Always use `ID.make[F, EntityId]` from `common/utils/ID.scala` for creating new entity IDs.

### Error Handling
Use `AError.BadRequest()`, `AError.Unauthorized()`, etc. for business logic errors that should be returned to users.

### Database Queries
- Use Skunk's fragment composition for dynamic queries
- Implement soft deletes with `deleted_at IS NULL` conditions
- Add proper indexes for performance

### Configuration
Environment variables override defaults in `reference.conf`. Key configurations:
- Database connection via `POSTGRES_*` variables
- Redis via `REDIS_URI` 
- External API keys via service-specific environment variables