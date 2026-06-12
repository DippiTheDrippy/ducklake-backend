# Backend

## Structure

The backend uses a **feature-based structure**. Instead of separating all controllers, services, repositories, and DTOs into global folders, code is grouped by feature.

```txt
src/main/java/se/kth/
├── admin/
├── dataset/
├── credential/
├── garage/
├── ducklake/
├── security/
└── common/
```

Each feature package contains the files needed for that part of the application, for example:

```txt
dataset/
├── DatasetResource.java
├── DatasetService.java
├── DatasetRepository.java
```

### Package Overview

`admin/`
Contains admin-only endpoints, such as creating datasets, updating datasets, and managing dataset permissions.

`dataset/`
Handles normal dataset browsing, dataset details, and metadata shown to users.

`credential/`
Handles generated credentials and connection information for accessing DuckLake datasets.

`garage/`
Contains Garage/S3 object store integration, such as bucket and key handling.

`ducklake/`
Contains DuckLake-specific logic, such as catalog handling and connection snippet generation.

`security/`
Handles the current authenticated user and permission checks based on Keycloak groups/roles.

`common/`
Contains shared code such as exceptions and error responses.

### Layer Rules

`Resource` = HTTP/controller layer
`Service` = business logic
`Repository` = database access
`Client` = external API access
`Dto` = data sent to or from the frontend
