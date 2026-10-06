# Weekly upstream synchronization

Both forks use `.github/workflows/upstream-sync.yml` (`Weekly upstream sync`).
App runs Monday 02:17 UTC; the distribution repository runs Monday 03:17 UTC.
GitHub schedules can be delayed. Manual `workflow_dispatch` uses the same path.
No update means prepare succeeds and remaining jobs skip; no PR or notification.

The app workflow merges `brunodev85/winlator-app:main` into the fork. The main
workflow merges `brunodev85/winlator:main`, preserves the fork app URL, and pins
`app` to `wx40217/winlator-app:main`. Main refuses to proceed until that app main
contains the latest app upstream. A delayed/failed app run therefore causes a
readable main failure rather than silently leaving the app behind.

Prepare creates an immutable candidate bundle before running candidate code.
Validate has only contents:read and no persisted Git credentials. It runs a
complete debug APK build, real unit tests (at least 10; both fork suites required),
and checks package identity, path rebasing, game library, APK signature, ZIP,
DEX, RootFS and ARM64 ELF headers. It uploads no APK. Integrate uses only its
repository's ephemeral GITHUB_TOKEN with contents:write and pull-requests:write;
it executes trusted base scripts, never candidate build code. There is no PAT,
extra OAuth, cross-repo write, secret, pull_request_target or untrusted PR trigger.
Official GitHub actions are pinned by SHA.

After success, integrate creates/reuses a deterministic candidate branch and PR,
merges with the expected head SHA, then verifies the merge and tested candidate
on remote main, including identical trees. Tests run in this workflow because a
GITHUB_TOKEN-generated PR cannot be relied upon to trigger other workflows.
It never force-pushes or modifies another branch. If main moves after validation,
it fails for a fresh run. GitHub's merge API has no expected-base parameter;
a concurrent main update in the final API race is detected by tree comparison
and requires takeover. Upstream changes to automation require human review.

## Failure takeover

Inspect the latest run of `Weekly upstream sync` in each repository. A failed
prepare means merge conflict, app ordering, invariants or fetch failure; failed
validate means build/test/APK failure; failed integrate means permission, branch
race, PR/merge or remote verification failure. The final `takeover` job writes
`UPSTREAM_SYNC_NEEDS_TAKEOVER` to logs and job summary. Inspect cancelled/timed-out
runs too: cancellation can prevent the summary job from running. A failed job
may leave a PR or branch; reuse/review it, never force-push over newer work.
Handle app first, then main. The existing Codex task is the failure responder;
this repository does not create another hosted task or send notifications.
GitHub may send account-configured Actions emails independently of this code.

If GitHub forbids GITHUB_TOKEN from creating PRs, integrate reports that error;
it does not enable the repository setting or obtain a stronger credential.
Changing that setting needs separate owner approval. Fork schedules may initially
be disabled; public repositories can have schedules disabled after 60 days of
inactivity. No keepalive commits are made. Enabling a disabled workflow is an
owner action; `workflow_dispatch` can confirm setup without waiting for Monday.

## Local verification

The cloud snapshot's tools remain useful for takeover, not for GitHub hosted
runners. Each new shell: `source /workspace/.winlator-env/activate.sh`.
The old start.md package/version/NO-SOURCE paragraphs describe upstream and are
outdated: fork package is com.wxwinlat and it has 10 real unit tests.

Run `python3 scripts/test_upstream_sync.py` for local Git fixtures covering
no-update, merge preservation, conflict, deterministic retry, protected workflow,
gitlink update, app ordering and stale main refusal. Run
`python3 scripts/verify_sync.py .` in app, or
`python3 scripts/verify_sync.py app` in main after submodule initialization.

Lint is not a gate here: the historical targetSdk28 lint failure remains separate.
Device installation, UI interaction, Windows games and production signing require
separate validation. Current APKs are debug-signed and never released by this flow.
