## 1. Layout Structure

- [x] 1.1 Update the task create/edit modal container in `frontend/app/pages/task-management/[id].vue` to use a wider responsive max width and a viewport-bounded max height.
- [x] 1.2 Split the modal into header, scrollable form body, and footer action areas without changing existing field bindings or submit handlers.
- [x] 1.3 Move the cancel and submit buttons into the footer action area so they remain reachable when the form body scrolls.

## 2. Form Grid

- [x] 2.1 Convert the task configuration form body to a responsive grid that is single-column by default and two-column on desktop width.
- [x] 2.2 Assign appropriate column spans to wide sections such as STRM path, rename regex help, filtering rules, STRM URL options, and checkbox options.
- [x] 2.3 Keep the existing form field order, labels, placeholders, helper text, validation attributes, and `v-model` bindings unchanged.

## 3. Verification

- [x] 3.1 Verify the create task modal displays as two columns on desktop width and single column on narrow width.
- [x] 3.2 Verify the edit task modal scrolls internally when content exceeds the viewport and all configuration fields are reachable.
- [x] 3.3 Verify the cancel, create, save, saving, and disabled submit states still behave as before.
