Fix the Android app's online data loading and crash issues.

Problems:

- The app does not send any network requests to the online server.
- Online content is never loaded, so the learning library always displays "Pustaka belajar tidak tersedia".
- This issue can also cause the app to crash or force close.

Tasks:

1. Trace the complete data-loading flow from the Android UI to the API layer.
2. Find out why no HTTP requests are being made. Check API configuration, base URL, repositories, ViewModels, network permissions, request triggers, and error handling.
3. Fix the actual cause and ensure the app correctly connects to the existing backend and loads real data from MongoDB through its APIs.
4. Make sure loading, empty, offline, and server-error states are handled safely. A failed request must never cause a crash.
5. Fix any related force-close issues and prevent null data, parsing errors, or unexpected API responses from crashing the app.
6. Add persistent crash and error logging. Save logs to:

"Android/data/<app.package.name>/files/logs/"

Include timestamps, exception details, stack traces, API request URLs, HTTP status codes, and relevant error information. Do not log passwords, tokens, or other sensitive data.

7. Ensure logs are saved even when an uncaught exception causes the app to terminate. Use a safe logging mechanism and avoid introducing additional crashes.
8. Add a way to inspect or export diagnostic logs if practical.

Important:

- Inspect the existing code before making changes.
- Do not replace the existing backend or use mock data.
- Preserve the current UI and unrelated features.
- Test actual API requests and verify that online content appears in the app.
- Build the Android app and fix any compilation errors.

Focus on making online data loading work reliably and preventing crashes. Do not simply hide the error message.