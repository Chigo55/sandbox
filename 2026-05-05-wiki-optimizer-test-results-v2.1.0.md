# wiki-optimizer v2.1.0 테스트 결과

**작성일**: 2026-05-05 (최종 업데이트: 2026-05-06)  
**실제 설치 버전**: v2.1.0 ✅  
**테스트 환경**: `/home/jeongih/project/sandbox` (Linux)

---

## 요약

| 섹션 | 결과 | 비고 |
|------|------|------|
| T0 | ✅ | v2.1.0 확인 |
| T1 | ✅ (T1-2 제외) | tree-sitter WASM 환경 미지원 → T1-3 graceful skip 확인 |
| T2 | ✅ | npm test 단위 테스트로 block 동작 확인 |
| T3 | ✅ | 두 그룹 형식 정상 |
| T4 | ✅ | |
| T5 | ✅ | |
| T6 | ✅ | v2.1.0 출력 확인 |
| T7 | ⚠ 부분 통과 | BUG-10: WIKI_PROJECT_DIR 미치환. 에러/보안 케이스 통과. NEW-6/7 로직 검증은 T10으로 확인 |
| T8 | ✅ | |
| T9 | ✅ | |
| T10 | ✅ **136 passed** | v2.1.0 기준 달성 |

---

## T0-2. 버전 확인

- [x] `--version` → `2.1.0`
- [x] `package.json` 버전 `2.1.0`
- [x] MCP 서버 파일: `servers/wiki-mcp-server.js` 존재

---

## T1. `wiki-optimizer init` (v2.1.0)

### T1-1. 기본 구조 생성
- [x] `.wiki/concept/`, `.wiki/pattern/`, `.wiki/domain/` 생성됨
- [x] `.wiki/index.md`, `.wiki/log.md` 생성됨
- [x] `.wiki/config.json` 생성됨
- [x] `.wikiignore` 생성됨
- [x] rules 3개 파일 복사됨
- [x] 훅 4종 등록됨
- [x] `${CLAUDE_PLUGIN_ROOT}` 미포함 (절대경로 사용)

### T1-2. tree-sitter AST 노트 자동 생성
- [ ] SKIP — `web-tree-sitter` + `tree-sitter-wasms` 설치 후에도 WASM 로딩 실패
  - 원인: Node.js 환경에서 WebAssembly 초기화(Parser.init()) 미동작 추정
  - T1-3으로 대체

### T1-3. tree-sitter 미설치 graceful skip
- [x] `⚠ tree-sitter unavailable — run: npm install (in plugin dir)` 출력됨
- [x] `⚠ Initial wiki notes skipped (wiki still functional)` 출력됨
- [x] 초기화 정상 완료, .wiki/ 기본 구조 생성됨

### T1-4. 재실행 멱등성
- [x] `already exists` 스킵
- [x] `Already registered` (훅 중복 방지)

---

## T3. `/wiki-ingest` — `## 연결` 두 그룹 형식 (NEW-1)

### T3-1. domain ingest
- [x] 3단계 디스패치 완료 (reviewer → writer → indexer)
- [x] `.wiki/domain/eq-equipment-repair.md` 생성
- [x] YAML frontmatter 5개 필드
- [x] `### 소스 파일` 그룹 — Controller.java, Service.java wikilink
- [x] `### 관련 노트` 그룹 — `[[.wiki/index]]` 포함
- [x] 두 그룹 합산 3개 wikilink
- [x] index.md Domain 섹션 갱신, log.md prepend

---

## T6. CLI 커맨드 (v2.1.0)

| 항목 | 결과 |
|------|------|
| `--version` / `-v` → `2.1.0` | ✅ |
| `help` — 6개 커맨드 (init/status/ingest/lint/query/help) | ✅ |

---

## T7. MCP 서버 — 6개 도구

### T7-0. MCP 서버 연결
- [x] wiki-optimizer 서버 연결됨
- [x] 6개 도구 확인: `wiki_get_index`, `wiki_list_notes`, `wiki_read_note`, `wiki_get_links`, `wiki_search`, `wiki_lint`
- ⚠ **BUG-10** 지속: `${workspaceFolder}` → WIKI_PROJECT_DIR 미치환으로 정상케이스 실패

### T7-1. `wiki_get_index`
- [ ] 정상케이스 — WIKI_PROJECT_DIR 미설정으로 실패
- [x] 에러케이스: `index.md가 없습니다. wiki-optimizer init 을 먼저 실행하세요.` ✅

### T7-2. `wiki_list_notes` (NEW-6)
- [x] 전체 목록: `위키 노트가 없습니다` (빈 wiki 대상이나 포맷 정상)
- [x] category 필터: `위키 노트가 없습니다`
- [x] 잘못된 category: `isError: true` + `category는 concept, pattern, domain 중 하나여야 합니다.`

### T7-3. `wiki_read_note`
- [ ] 정상케이스 — WIKI_PROJECT_DIR 미설정
- [x] 존재하지 않는 노트 → `isError: true` + `노트를 찾을 수 없습니다`
- [x] 경로 순회(`../../etc/passwd`) → `isError: true` + `잘못된 노트 경로`

### T7-4. `wiki_get_links` (NEW-7)
- [ ] 정상케이스 — WIKI_PROJECT_DIR 미설정
- [x] 존재하지 않는 노트 → `isError: true` + `노트를 찾을 수 없습니다`
- [x] 경로 순회(`../../etc/passwd`) → `isError: true` + `잘못된 노트 경로`

### T7-5. `wiki_search`
- [x] 빈 키워드 → `isError: true` + `keyword 파라미터가 필요합니다`
- [x] 매칭 없는 키워드 → `관련 노트 없음` (에러 아님)

### T7-6. `wiki_lint`
- [x] `Wiki Lint 리포트` 형식
- [x] `### 고아 노트`, `### 깨진 링크`, `### 고립 노트` 3섹션
- [x] 이슈 수 합계 표시됨

---

## T8. rules 파일 형식

- [x] domain 노트 `### 소스 파일` / `### 관련 노트` 두 그룹
- [x] templates.md 4개 템플릿 모두 두 그룹 형식

---

## T9. 회귀 — 모두 통과

| 항목 | 버전 | 결과 |
|------|------|------|
| CRLF CLAUDE.md (BUG-8) | v1.2.5 | ✅ |
| cmdStatus() rules 체크 (BUG-3) | v1.2.3 | ✅ |
| `--version` / `-v` (BUG-5) | v1.2.3 | ✅ |
| `help` ingest 포함 (BUG-6) | v1.2.3 | ✅ |
| 훅 중복 등록 방지 (v1.2.2) | v1.2.2 | ✅ |
| `${CLAUDE_PLUGIN_ROOT}` 절대경로 (v1.2.2) | v1.2.2 | ✅ |

---

## T10. `npm test` — **136 passed, 0 failed** ✅

| 테스트 파일 | v1.2.5 기준 | v2.1.0 결과 |
|------------|------------|------------|
| `test-wiki-reader.js` | 9 | 9 ✅ |
| `test-pattern-detector.js` | 18 | 18 ✅ |
| `test-agents.js` | 28 | 28 ✅ |
| `test-pre-read-hook.js` | 6 | 6 ✅ |
| `test-post-read-hook.js` | 7 | 7 ✅ |
| `test-ingest-watch.js` | 10 | 10 ✅ |
| `test-register-hooks.js` | 12 | 12 ✅ |
| `test-session-start-hook.js` | 6 | 6 ✅ |
| `test-stop-hook.js` | 6 | 6 ✅ |
| `test-init-claude-md.js` | 6 | 6 ✅ |
| `test-wiki-linter.js` | 6 | 6 ✅ |
| `test-wiki-searcher.js` | 6 | 6 ✅ |
| `test-mcp-server.js` | 6 | **10 ✅** (NEW-6/7 추가) |
| **합계** | **126** | **136** ✅ |

### T10-2. `test-mcp-server.js` 신규 케이스 (NEW-6, NEW-7)
- [x] `TOOLS 배열에 6개 도구 정의됨` — PASS
- [x] `wiki_list_notes — 전체 노트 목록 반환` — PASS
- [x] `wiki_list_notes — category 필터 동작` — PASS
- [x] `wiki_get_links — 아웃고잉·인커밍 링크 반환` — PASS
- [x] `wiki_get_links — 존재하지 않는 노트는 에러 반환` — PASS

### T10-3. MCP 모듈 export
- [x] `node -e "..."` → `6 function` 출력됨

---

## 발견된 버그 / 개선 필요 항목

| 번호 | 항목 | 심각도 | 세부사항 |
|------|------|--------|---------|
| BUG-10 | MCP `${workspaceFolder}` 미치환 | 높음 | `.mcp.json`의 `WIKI_PROJECT_DIR: "${workspaceFolder}"`가 프로젝트 경로로 치환되지 않아 MCP 정상케이스 실패 |
| INFO-1 | tree-sitter WASM 로딩 실패 | 낮음 | `web-tree-sitter` + `tree-sitter-wasms` 설치 후에도 Node.js 환경에서 WASM 초기화 미동작. graceful skip 동작은 정상 |
