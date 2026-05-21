## 1. Data Model

- [x] 1.1 Add a Flyway migration for `source_type`, preserving existing rows as `OPENLIST`
- [x] 1.2 Update `OpenlistConfig` entity and `OpenlistConfigDto` with `sourceType`
- [x] 1.3 Update `OpenlistConfigMapper.xml` result maps, column lists, insert, and update statements
- [x] 1.4 Adjust DTO validation so OpenList-only fields are required only for `OPENLIST`

## 2. Backend APIs

- [x] 2.1 Update OpenList config create/update/list/detail responses to include data source fields
- [x] 2.2 Add local config handling that does not require Base URL, token, username, base path, or local path
- [x] 2.3 Add local directory tree query endpoint returning `name`, `path`, and `hasChildren`
- [x] 2.4 Add task path validation that dispatches by associated config `sourceType`
- [x] 2.5 Update task create/update flow to validate `LOCAL` task paths as existing directories

## 3. File Processing

- [x] 3.1 Implement a local file service that lists directories and reads local file content
- [x] 3.2 Update `FileDiscoveryHandler` to discover files from OpenList or local filesystem based on `sourceType`
- [x] 3.3 Ensure local file models contain name, type, normalized path, URL-as-local-path, and size
- [x] 3.4 Update STRM generation path handling so local mode writes local file paths and never appends sign
- [x] 3.5 Update NFO, image, and subtitle handlers to read related files from the configured data source
- [x] 3.6 Ensure incremental orphan cleanup receives the correct source path semantics for local mode

## 4. Frontend

- [x] 4.1 Add data source type selection to add/edit config modals
- [x] 4.2 Show OpenList fields only for `OPENLIST`, and show no path field for `LOCAL` configs
- [x] 4.3 Update config card display, delete confirmation, and task navigation text for both source types
- [x] 4.4 Add local directory tree selector for task path when the current config is `LOCAL`
- [x] 4.5 Keep the existing OpenList task path input and validation for `OPENLIST`
- [x] 4.6 Submit and edit task forms with source-aware task path values

## 5. Verification

- [x] 5.1 Run `openspec validate support-local-file-data-source --strict`
- [x] 5.2 Manually verify OpenList config create/edit and task create/edit still use OpenList validation
- [x] 5.3 Manually verify local config create/edit, local tree selection, local task save, and local task execution flow
