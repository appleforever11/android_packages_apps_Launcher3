# Exact-source controller checks

These are independent Android instrumentation checks, **not a Launcher3QuickStep platform build**. The harness compiles the actual `SecondaryDrawerResizeController.java` from the source tree together with a small test Activity. It does not repackage the installed launcher or use the private prototype helper.

On October 18, 2026, the checked-in script was executed on the Thor lower display (logical display 1) using SDK 35 build tools and the production source hash recorded in [source-sha256.txt](source-sha256.txt).

- [First run: 31 checks passed](first-run.txt).
- [After force-stopping and restarting the test process: 33 checks passed](restart-run.txt).

Coverage: attachment to the physical lower screen; visible test panel; default ratios; non-grip touches passing through; oversized and undersized gesture bounds; persistence into a reconstructed controller; gesture cancellation restoring original ratios; direct resize/reset methods; and persisted ratios surviving test-process restart after pending disk writes are flushed.

The tests are not exhaustive. They do not instantiate `SecondaryDragLayer`, certify its inset math in Launcher3, wire or traverse its accessibility actions with TalkBack, test icon launching/pinning, test a reboot, or validate other display configurations. “Direct enlarge/shrink method” checks are not accessibility end-to-end tests.

## Findings during test development

The source was changed to select its preferences when the view is attached, using the view's actual display, rather than relying on the activity's display during inflation. This avoids selecting a key before the secondary display is established.

An early restart test also killed the process before asynchronous SharedPreferences writes were flushed, causing an assertion failure. The harness now explicitly waits for the test fixture's disk writes before force-stopping. The passing result therefore does not promise that Android's asynchronous `apply()` survives an immediate power loss or process kill before its write finishes.

## Reproduce

Set `ANDROID_JAR` (tested with API 35), `ANDROID_BUILD_TOOLS`, `JAVA_HOME`, and `ANDROID_SERIAL` to an explicitly connected test device, then run `build-and-run.sh`.

The script creates and signs a disposable test APK locally, replaces only the dedicated `dev.kevinhowe.thordrawertests` test package, temporarily opens a test panel on display 1, and saves its results in a temporary directory. It uses no private release key. Return to the normal launcher after testing; the test package can be uninstalled.

The generated APK/key are not committed. No test result here is used to give a Gerrit Verified vote or mark the series ready for merge.
