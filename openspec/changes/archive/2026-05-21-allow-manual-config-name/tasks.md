## 1. Backend Contract

- [x] 1.1 Update OpenList configuration DTO/API descriptions so `username` is treated as the user-provided configuration name.
- [x] 1.2 Change create/update validation so every configuration requires a non-empty configuration name.
- [x] 1.3 Remove LOCAL configuration name auto-generation from create logic.
- [x] 1.4 Apply configuration-name uniqueness checks consistently for OPENLIST and LOCAL configurations.
- [x] 1.5 Update backend error messages from username-oriented wording to configuration-name wording.

## 2. Frontend Form Flow

- [x] 2.1 Add a required configuration name field to the add configuration modal.
- [x] 2.2 Add the configuration name field to the edit configuration modal and initialize it from the existing configuration.
- [x] 2.3 Keep the user-entered configuration name when OpenList validation returns remote user information.
- [x] 2.4 Reset configuration name together with the rest of the modal form state.

## 3. Frontend Display Text

- [x] 3.1 Display the user-provided configuration name on configuration cards.
- [x] 3.2 Update delete and enable/disable confirmation labels to use the configuration name.
- [x] 3.3 Review task management configuration details for user-facing wording that should say configuration name instead of username.

## 4. Verification

- [x] 4.1 Manually inspect the create and edit request payloads to confirm they submit the user-entered configuration name.
- [x] 4.2 Manually inspect backend create logic to confirm no automatic configuration name is generated.
- [x] 4.3 Confirm the OpenSpec status reports the change as apply-ready.
