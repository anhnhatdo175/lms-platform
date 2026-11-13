# LMS Platform — Agent Guide

**Project:** Graduation thesis — LMS with AI Adaptive Profile-Based RAG  
**Stack:** Java 25 + Spring Boot 4 · React 19 + Vite + TypeScript 6 · MySQL 8 · Docker Compose  
**Team:** 2 developers, 3 months, Modular Monolith

## Read source-of-truth docs before coding

| Topic | File (branch `docs`) |
|-------|---------------------|
| Business rules, state machines | `docs/Dac_ta_nghiep_vu_LMS_MVP_AI_Adaptive_RAG_v3.2.md` |
| DB tables, columns, FK, constraints | `docs/ERD_chi_tiet_LMS_MVP_AI_Adaptive_RAG_v2.2.md` |
| HTTP API, request/response, error codes | `docs/OpenAPI_LMS_MVP_AI_Adaptive_RAG_v1.5.yaml` |
| Folder structure | `docs/Cau_truc_thu_muc_thuc_dung_LMS.md` |
| Finalized decisions | `docs/LMS_Consistency_Decision_Register_v0.2.md` |
| Design tokens, colors | `frontend/src/styles/tokens.css` |
| Design system | `frontend/design-system/lms-platform/MASTER.md` |
| UI components | `frontend/GUIDE_UI_COMPONENTS.md` |
| ESLint rules | `frontend/eslint.config.js` |

> Use `git show docs:<path>` to read files from the `docs` branch.

## What these rules contain

Only things an agent commonly gets wrong and **cannot infer** from code/docs:

- [`critical-decisions.md`](critical-decisions.md) — counterintuitive design decisions
- [`conventions.md`](conventions.md) — naming conventions, gotchas with no lint rule

## Required before marking done

```bash
cd frontend; npm run lint   # must pass with 0 errors
cd frontend; npm run build  # must succeed
```
