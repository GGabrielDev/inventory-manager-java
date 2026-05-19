# Rollout Summary

## Updated Files
- `README.md`
- `backend/README.md`
- `docs/AUDITING.md`
- `docs/ITEM-REQUEST-WORKFLOW.md`
- `backend/src/main/java/com/inventorymanager/backend/web/TestController.java`
- `.agents/rules/test-adversary-rules.md`
- `.agents/rules/auditor-rules.md`
- `.agents/prompts/local-prepush-copilot-adversary.md`
- `.agents/prompts/local-prepush-copilot-auditor.md`

## Key Reinforcements
- Clarified that non-login API routes require JWT auth and most mutating routes are permission-gated, not role-gated.
- Corrected item-request docs to match execution defaults, especially inbound branch fallback and review decision handling.
- Fixed live diagnostic controller docs to reflect that `/api/test/*` is authenticated, not public.
- Tightened the audit pipeline docs so guards fail closed on missing tests, audit gaps, auth bypass risk, and hierarchy regressions.

## New Adversary Rules
- Explicit allowlist for auth-gated exceptions: `/api/auth/login`, `/api/auth/validate`, `/api/auth/me`, `/api/test/*`, and Swagger/OpenAPI routes.
- Required invariants now include `State > Municipality > Parish > Branch > Department`.
- Write-path checks now demand matching audit commit coverage and relation commits where applicable.
- Frontend checks are only enforced when JavaFX files change.

## Follow-up Gaps
- `POST /api/item-requests/{id}/review` still treats any non-`approve` value as rejection; docs now state that behavior, but input validation could still be tightened later.
- No automated markdown linting was added for the doc/rule files themselves.
