# wiki-optimizer v2.1.1 테스트 결과

**작성일**: 2026-05-06  
**설치 버전**: v2.1.1 ✅  
**이전 버전**: v2.1.0  
**테스트 환경**: `/home/jeongih/sandbox-wiki-v211` (Linux)

---

## 요약

| 섹션 | 결과 | 비고 |
|------|------|------|
| T0 버전 | ✅ `2.1.1` | |
| T1 npm test | ✅ **136 passed, 0 failed** | |
| C1 기본 커맨드 | ✅ | `2.1.1` / 6커맨드 / status 미초기화 안내 |
| **C2 tree-sitter** | ❌ **INFO-1 미완성** | WASM 존재 확인 추가됨, locateFile 적용됨, 그러나 API 불일치로 여전히 실패 |
| C3 ingest --watch | ✅ | entity:done 이벤트 정상 출력 |
| C4 lint / lint --fix | ✅ | ghost-note 탐지 및 자동 제거 |
| C5 query | ⚠ | wiki 노트 미인제스트 상태에서 "관련 노트 없음" (정상 동작) |
| **M0 BUG-10** | ✅ **수정 확인** | findProjectRoot() 동작, .mcp.json env 제거 |
| **M1 wiki_get_index** | ✅ | v2.1.0에서 실패→ v2.1.1에서 index.md 정상 반환 |
| **M2 wiki_search** | ✅ | errorCode 2건 매칭 반환 |
| **M3 wiki_list_notes** | ✅ | 3건 노트 목록 반환 (concept/pattern/domain) |
| **M4 wiki_read_note** | ✅ | 노트 전체 내용 반환 |
| **M4 wiki_get_links** | ✅ | 아웃고잉/인커밍 링크 반환 |
| M5 wiki_lint | ✅ | 리포트 형식 + 0건 |
| P1 훅 동작 | ✅ | SessionStart/PreToolUse(block)/PostToolUse 정상 |
| P2 /wiki-ingest | ✅ | `## 연결` 두 그룹 형식 정상 |
| P3 /wiki-query | ✅ | wiki-reviewer 디스패치, 출처 wikilink 포함 |
| P4 /wiki-lint | ✅ | wiki-reviewer 디스패치, M5와 깨진 링크 0건 일치 |
| R1 회귀 | ✅/❌ 혼재 | BUG-10 ✅, INFO-1 WASM ✅ 일부, API 불일치 ❌ |

---

## T0-2. 버전 확인

- [x] `--version` → `2.1.1`
- [x] `package.json` → `2.1.1`
- [x] MCP 서버 파일 존재: `servers/wiki-mcp-server.js`

---

## T1. npm test

- [x] **136 passed, 0 failed** ✅

---

## Phase 1: CLI

### C1. 기본 커맨드

- [x] `--version` → `2.1.1`
- [x] `help` → 6개 커맨드 포함
- [x] `status` → `wiki-optimizer가 초기화되지 않았습니다. 실행: wiki-optimizer init` 출력

### C2. `init` — tree-sitter AST 노트 생성 (INFO-1)

#### C2-1. 기본 구조 생성

- [x] `.wiki/` 구조 전체 생성됨
- [x] 훅 4종 v2.1.1 절대경로로 등록됨
- [x] `${CLAUDE_PLUGIN_ROOT}` 미포함 (절대경로 치환 확인)

#### C2-2. tree-sitter AST 노트 생성 ★ (INFO-1 핵심)

- [x] `Analyzing project structure (tree-sitter)...` 출력됨
- [ ] **`Generated N wiki notes` 출력 실패 — `.wiki/domain/` 노트 미생성**

**근본 원인 (신규 발견 BUG-11)**:
```
ast-analyzer.js: Parser = require('web-tree-sitter')
await Parser.init({ locateFile: ... })  ← Parser.init is not a function
```
`require('web-tree-sitter')` 반환 타입은 `object` (모듈 네임스페이스).  
`Parser.init`은 `undefined`. 실제 경로: `require('web-tree-sitter').Parser.init`.  
INFO-1은 `locateFile` 옵션을 추가했으나 require 패턴의 API 불일치는 미수정.

#### C2-3. INFO-1 수정 메커니즘 확인

- [x] `WASM exists: true` — WASM 파일 존재 사전 확인 ✅
- [ ] `init OK` — `Parser.init is not a function` 에러로 실패 ❌

#### C2-4. 재실행 멱등성

- [x] `already exists` 스킵
- [x] `Already registered` 4건 (훅 중복 방지)

### C3. `ingest --watch`

- [x] `concept:added BaseDTO` 출력됨
- [x] `entity:done EqEquipmentRepairService` 출력됨
- [x] `entity:done InvOnhandsController` 출력됨
- [x] 마지막 라인 `ingest:complete`

### C4. `lint` / `lint --fix`

- [x] `ghost-note` 깨진 링크 탐지됨
- [x] `--fix` 후 ghost-note 라인 제거됨

### C5. `query errorCode`

- [x] `🔍 Wiki Query: "errorCode"` 헤더 출력됨
- [x] `관련 노트 없음` — wiki 노트 미인제스트 상태에서 정상 동작 (빈 wiki 쿼리 안내)

---

## Phase 2: MCP — BUG-10 수정 전체 재검증

### M0. 서버 연결 + `findProjectRoot()` (BUG-10 핵심)

- [x] wiki-optimizer 서버 Connected
- [x] 6개 도구 확인
- [x] **`.mcp.json` env 섹션 제거됨** (`env` 키 없음)
- [x] **미치환 템플릿 거부**: `WIKI_PROJECT_DIR='${workspaceFolder}'` → `rejected: true`
- [x] **`.wiki/` 탐색**: `findProjectRoot()` → 샌드박스 루트 반환

### M1. `wiki_get_index` ★ (v2.1.0 실패 → v2.1.1 수정)

- [x] index.md 전체 내용 반환됨 ✅
- [x] Domain/Concept/Pattern 섹션 포함
- [x] 에러케이스: `index.md가 없습니다.` 반환

### M2. `wiki_search` ★

- [x] `errorCode` 검색 → 2건 반환 (concept/base-dto 9회, pattern/base-dto-inheritance 7회)
- [x] 각 매칭 라인 미리보기 포함
- [x] 빈 keyword → `isError: true`
- [x] 매칭 없는 keyword → `관련 노트 없음` (에러 아님)

### M3. `wiki_list_notes` ★

- [x] 전체 목록: concept 1건 / pattern 1건 / domain 1건 반환
- [x] 각 노트에 경로, 제목, 갱신일, 태그 포함
- [x] `category=domain` → domain 노트만 반환
- [x] 잘못된 category → `isError: true`

### M4. `wiki_read_note` + `wiki_get_links` ★

#### M4-1. `wiki_read_note`
- [x] 노트 전체 내용 반환됨 ✅
- [x] `## 연결 / ### 소스 파일` / `### 관련 노트` 두 그룹 확인 (P2 업데이트 후)

#### M4-2. `wiki_get_links`
- [x] `### 아웃고잉 위키 노트` — base-dto, base-dto-inheritance 포함
- [x] `### 인커밍` 섹션 존재
- ⚠ `### 아웃고잉 소스 파일 (0건)` — source_files YAML 인라인 배열 파싱 이슈
- [x] 존재하지 않는 노트 → `isError: true`
- [x] `../../etc/passwd` → `isError: true`

### M5. `wiki_lint`

- [x] `Wiki Lint 리포트` + 3개 섹션 반환
- [x] P4 /wiki-lint와 **깨진 링크 0건 일치** ✅
- [x] 이슈 수 합계 표시

---

## Phase 3: Plugin (빠른 회귀)

### P1. 훅 동작

- [x] SessionStart: `<wiki-index>` 블록 주입됨
- [x] PreToolUse: `src/eq/**/*.java` → `hookDecision: "block"` ✅
- [x] PostToolUse: 미등록 도메인 → `/wiki-ingest` 제안

### P2. `/wiki-ingest` — `## 연결` 두 그룹

- [x] `### 소스 파일` — Controller.java, Service.java wikilink 포함
- [x] `### 관련 노트` — `[[.wiki/index]]` 포함
- [x] log.md prepend됨

### P3. `/wiki-query`

- [x] wiki-reviewer 디스패치됨
- [x] 출처 wikilink (`[[.wiki/concept/base-dto]]`, `[[.wiki/pattern/base-dto-inheritance]]`) 포함

### P4. `/wiki-lint`

- [x] wiki-reviewer 디스패치됨
- [x] M5 결과와 깨진 링크 0건 일치 (시스템 파일 필터링 차이는 예상된 동작)

---

## R1. 수정 항목 회귀 고정 확인

| 수정 | 버전 | 결과 |
|------|------|------|
| BUG-10: `findProjectRoot()` — 미치환 템플릿 거부 | v2.1.1 | ✅ |
| BUG-10: `findProjectRoot()` — `.wiki/` 탐색 | v2.1.1 | ✅ |
| BUG-10: `.mcp.json` env 섹션 제거 | v2.1.1 | ✅ |
| INFO-1: WASM 파일 존재 사전 확인 | v2.1.1 | ✅ |
| INFO-1: `locateFile` 옵션 적용 | v2.1.1 | ✅ (코드 존재) |
| INFO-1: tree-sitter init 성공 | v2.1.1 | ❌ API 불일치로 여전히 실패 |
| CRLF CLAUDE.md (BUG-8) | v1.2.5 | ✅ 회귀 없음 (T1 npm test 확인) |
| 훅 중복 등록 방지 | v1.2.2 | ✅ 회귀 없음 |

---

## 발견된 신규 버그

| 번호 | 항목 | 심각도 | 세부사항 |
|------|------|--------|---------|
| BUG-11 | INFO-1 미완성 — Parser.init API 불일치 | 높음 | `require('web-tree-sitter')` 반환 객체에 `.init` 없음. `require('web-tree-sitter').Parser.init()`이 정확한 경로. v2.1.1에서 locateFile 추가 + WASM 확인 추가했으나 require 패턴 미수정으로 tree-sitter 여전히 unavailable |
| BUG-12 | wiki_get_links 소스 파일 0건 | 낮음 | source_files YAML 인라인 배열(`["a","b"]`) 파싱 이슈로 아웃고잉 소스 파일 미탐지 |
