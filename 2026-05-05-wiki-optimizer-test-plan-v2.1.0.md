# wiki-optimizer v2.1.0 전체 기능 테스트 플랜

**작성일**: 2026-05-05  
**대상**: 샌드박스 프로젝트에서 플러그인 업데이트 후 전체 기능 검증  
**플러그인 버전**: 2.1.0  
**이전 플랜 참조**: `2026-05-05-wiki-optimizer-test-plan-v1.2.5.md`

---

## 테스트 설계 철학

세 인터페이스를 다음 순서로 검증한다:

```
CLI → MCP → Plugin
알고리즘   알고리즘+AI   AI 에이전트
(결정론적)  (구조화 읽기)  (LLM 완전 위임)
```

- **CLI 먼저**: LLM 없이 코어 알고리즘과 파일 시스템 동작 검증. 빠르고 비용 없음.
- **MCP 두 번째**: CLI가 만든 위키 구조를 MCP 도구로 읽어 공유 코어(`lib/`) 검증.
- **Plugin 마지막**: 훅 자동화와 스킬/에이전트로 LLM 기반 전체 워크플로우 검증.

각 단계는 이전 단계 결과물을 재사용한다.

---

## v2.0.0 ~ v2.1.0 신규/변경 항목

| ID | 버전 | 내용 | 검증 섹션 |
|----|------|------|----------|
| NEW-1 | v2.0.0 | `## 연결` 섹션 두 그룹 분리 (`### 소스 파일` / `### 관련 노트`) | C4, M4, P3 |
| NEW-2 | v2.0.0 | CLI `init` — tree-sitter AST 기반 domain 노트 자동 생성 | C2 |
| NEW-3 | v2.0.0 | CLI `lint` 커맨드 추가 | C4 |
| NEW-4 | v2.0.0 | CLI `query` 커맨드 추가 | C5 |
| NEW-5 | v2.0.0 | MCP stdio 서버 4개 도구 | M1~M6 |
| NEW-6 | v2.1.0 | MCP `wiki_list_notes` | M3 |
| NEW-7 | v2.1.0 | MCP `wiki_get_links` | M4 |

---

## 검증 범위

| 단계 | 섹션 | 내용 |
|------|------|------|
| **준비** | T0 | 샌드박스 환경 구성 |
| **준비** | T1 | `npm test` — 자동화 단위 테스트 (136 passed) |
| **CLI** | C1 | `--version` / `help` / `status` |
| **CLI** | C2 | `init` — 구조 생성 + AST 노트 (NEW-2) |
| **CLI** | C3 | `ingest` — 파일 스캔 |
| **CLI** | C4 | `lint` / `lint --fix` (NEW-3) |
| **CLI** | C5 | `query` (NEW-4) |
| **MCP** | M0 | 서버 연결 확인 |
| **MCP** | M1 | `wiki_get_index` |
| **MCP** | M2 | `wiki_search` |
| **MCP** | M3 | `wiki_list_notes` (NEW-6) |
| **MCP** | M4 | `wiki_read_note` + `wiki_get_links` (NEW-7) |
| **MCP** | M5 | `wiki_lint` |
| **Plugin** | P1 | 훅 — SessionStart / PreToolUse / PostToolUse / Stop |
| **Plugin** | P2 | `/wiki-ingest` — LLM 노트 생성 |
| **Plugin** | P3 | `/wiki-query` |
| **Plugin** | P4 | `/wiki-lint` / `--fix` / `--deep` |
| **회귀** | R1 | v1.2.x 이전 수정 유지 확인 |

---

## T0. 샌드박스 환경 준비

### T0-1. 샌드박스 프로젝트 생성

```bash
mkdir ~/sandbox-wiki-v210 && cd ~/sandbox-wiki-v210
git init
git config user.email "test@example.com"
git config user.name "Test"

mkdir -p src/eq src/inv src/common

cat > src/eq/EqEquipmentRepairService.java << 'EOF'
@Service
public class EqEquipmentRepairService extends BaseService {
  private final EqRepairRepository repairRepository;
  public List<RepairDTO> findRepairList(String eqId) { return null; }
  public void saveRepair(RepairDTO dto) {}
}
EOF

cat > src/inv/InvOnhandsController.java << 'EOF'
@Controller
public class InvOnhandsController {
  public ModelAndView list(InvOnhandsDTO dto) { return null; }
}
EOF

cat > src/common/BaseDTO.java << 'EOF'
public class BaseDTO {
  private String errorCode;
  private String errorMsg;
  public String getErrorCode() { return errorCode; }
}
EOF

git add . && git commit -m "feat: initial sandbox source files"
mkdir .claude
```

### T0-2. 플러그인 업데이트 & 버전 확인

```bash
/plugin update wiki-optimizer
node ~/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/2.1.0/bin/wiki-optimizer.js --version
```

- [ ] 버전 `2.1.0` 출력됨
- [ ] `package.json`, `plugin.json`, `bin/wiki-optimizer.js` 세 파일 모두 `2.1.0`
- [ ] `servers/wiki-mcp-server.js` 파일 존재

---

## T1. `npm test` — 자동화 단위 테스트

> CLI / MCP 수동 테스트 전에 먼저 실행한다. 코어 로직의 이상을 조기 탐지한다.

```bash
cd ~/path/to/wiki-optimizer
npm test
```

- [ ] 오류 없이 완료
- [ ] **136 passed, 0 failed**

**테스트 파일별 기대 결과**:

| 테스트 파일 | 기대 |
|------------|------|
| `test-wiki-reader.js` | 9 passed |
| `test-pattern-detector.js` | 18 passed |
| `test-agents.js` | 28 passed |
| `test-pre-read-hook.js` | 6 passed |
| `test-post-read-hook.js` | 7 passed |
| `test-ingest-watch.js` | 10 passed |
| `test-register-hooks.js` | 12 passed |
| `test-session-start-hook.js` | 6 passed |
| `test-stop-hook.js` | 6 passed |
| `test-init-claude-md.js` | 6 passed |
| `test-wiki-linter.js` | 6 passed |
| `test-wiki-searcher.js` | 6 passed |
| `test-mcp-server.js` | **10 passed** |

**mcp-server 신규 케이스 확인** (NEW-6, NEW-7):
- [ ] `TOOLS 배열에 6개 도구 정의됨` — PASS
- [ ] `wiki_list_notes — 전체 노트 목록 반환` — PASS
- [ ] `wiki_list_notes — category 필터 동작` — PASS
- [ ] `wiki_get_links — 아웃고잉·인커밍 링크 반환` — PASS
- [ ] `wiki_get_links — 존재하지 않는 노트는 에러 반환` — PASS

---

---

# Phase 1: CLI

> LLM 없이 알고리즘 기반 동작을 검증한다.  
> 이 단계의 결과물(`.wiki/` 구조와 초기 노트)이 MCP / Plugin 단계의 입력이 된다.

---

## C1. 기본 커맨드

### C1-1. `--version` / `-v`

```bash
wiki-optimizer --version
wiki-optimizer -v
```

- [ ] `2.1.0` 출력됨 (두 플래그 모두)

### C1-2. `help`

```bash
wiki-optimizer help
```

- [ ] `init`, `status`, `ingest`, `lint`, `query`, `help` 6개 커맨드 설명 포함됨

### C1-3. `status` — 미초기화 상태

```bash
wiki-optimizer status
```

- [ ] "wiki-optimizer가 초기화되지 않았습니다" 안내 출력됨
- [ ] `wiki-optimizer init` 실행 안내 포함됨

---

## C2. `init` — 구조 생성 + AST 노트 (NEW-2)

```bash
wiki-optimizer init
```

### C2-1. 기본 구조 생성

- [ ] `.wiki/concept/`, `.wiki/pattern/`, `.wiki/domain/` 생성됨
- [ ] `.wiki/index.md`, `.wiki/log.md` 생성됨
- [ ] `.wiki/config.json` 생성됨
- [ ] `.wikiignore` 생성됨 (`node_modules/`, `dist/`, `.git/` 포함)
- [ ] `.wiki/rules/format.md`, `operations.md`, `templates.md` 복사됨

### C2-2. 훅 등록

```bash
cat .claude/settings.local.json | python3 -m json.tool
```

- [ ] SessionStart / PreToolUse / PostToolUse / Stop 4종 등록됨
- [ ] 등록된 커맨드에 `${CLAUDE_PLUGIN_ROOT}` 미포함 (절대경로로 치환됨)

### C2-3. tree-sitter AST 초기 노트 생성 (NEW-2)

- [ ] `Analyzing project structure (tree-sitter)...` 메시지 출력됨
- [ ] `.wiki/domain/` 하위에 Java 소스 기준 노트 생성됨
- [ ] `Generated N wiki notes in .wiki/domain/ (N skipped)` 출력됨

**생성된 노트 내용 검증** (`eq-equipment-repair-service.md` 또는 유사 파일):

```bash
ls .wiki/domain/
cat .wiki/domain/eq-equipment-repair-service.md
```

- [ ] YAML frontmatter 5개 필드 (`id`, `last_updated`, `source_files`, `dependencies`, `tags`)
- [ ] `tags`에 `#auto-generated` 포함
- [ ] `## 감지된 클래스` — `@Service` 어노테이션, `extends BaseService` 표시됨
- [ ] `## 감지된 메서드` — `findRepairList`, `saveRepair` 포함
- [ ] `## 감지된 필드` — `repairRepository` 포함
- [ ] **`## 연결` 두 그룹 형식** (NEW-1):
  ```
  ### 소스 파일
  - [[src/eq/EqEquipmentRepairService.java]] — 자동 감지된 소스 파일

  ### 관련 노트
  - [[.wiki/index]] — 전체 카탈로그
  ```
- [ ] `index.md` Domain 섹션에 자동 등록됨

### C2-4. CLAUDE.md 처리 (없는 경우)

- [ ] `⚠ CLAUDE.md가 없습니다` 경고 출력됨
- [ ] 초기화는 정상 완료됨 (프로세스 중단 없음)

### C2-5. tree-sitter 미설치 시 graceful skip

**전제**: `web-tree-sitter` 미설치 환경

- [ ] 경고 후 skip, `.wiki/` 기본 구조는 정상 생성됨

### C2-6. `status` — 초기화 완료 상태

```bash
wiki-optimizer status
```

- [ ] `.wiki/` 구조 ✓ 표시됨
- [ ] 페이지 수 표시됨 (AST 생성 노트 수)
- [ ] 훅 등록 상태 ✓ 표시됨

### C2-7. 재실행 멱등성

```bash
wiki-optimizer init
```

- [ ] 기존 파일 덮어쓰지 않음 (`already exists` skip 메시지)
- [ ] 훅 중복 등록 안 됨
- [ ] AST 노트 중복 생성 안 됨 (`Generated 0 wiki notes ... (N skipped)`)

---

## C3. `ingest` — 파일 스캔

```bash
wiki-optimizer ingest src/eq/
```

- [ ] 스캔 결과 표시됨
- [ ] `Run /wiki-ingest to add them to the wiki.` 안내 포함됨 (LLM 작업은 스킬 사용)

**`--watch` 모드 이벤트 스트림**:

```bash
wiki-optimizer ingest --watch src/
```

- [ ] `entity:done EqEquipmentRepairService` 출력됨 (@Service 키워드)
- [ ] `entity:done InvOnhandsController` 출력됨 (@Controller 키워드)
- [ ] `module:start eq` / `module:start inv` 출력됨
- [ ] 마지막 라인: `ingest:complete`

---

## C4. `lint` — 정적 분석 (NEW-3)

### C4-1. 기본 lint

```bash
wiki-optimizer lint
```

- [ ] `📋 Wiki Lint 결과` 헤더 출력됨
- [ ] 고아 노트 / 깨진 링크 / 고립 노트 3개 항목 출력됨
- [ ] 이슈 수 표시됨

### C4-2. 깨진 링크 탐지

```bash
echo "- [[.wiki/domain/ghost-note]] — 테스트용" >> .wiki/domain/eq-equipment-repair-service.md
wiki-optimizer lint
```

- [ ] `ghost-note` 깨진 링크 탐지됨

### C4-3. `--fix` — 깨진 링크 자동 제거

```bash
wiki-optimizer lint --fix
grep "ghost-note" .wiki/domain/eq-equipment-repair-service.md
```

- [ ] `깨진 링크 1건 자동 수정됨` 메시지 출력됨
- [ ] `grep` 결과 없음 (라인 제거 확인)

### C4-4. `.wiki/` 없는 경우 에러 처리

```bash
mkdir /tmp/nowiki && cd /tmp/nowiki && wiki-optimizer lint
```

- [ ] `.wiki/ 디렉토리가 없습니다. 먼저 wiki-optimizer init 을 실행하세요.` 출력됨

---

## C5. `query` — 키워드 검색 (NEW-4)

### C5-1. 기본 검색

```bash
cd ~/sandbox-wiki-v210
wiki-optimizer query errorCode
```

- [ ] `🔍 Wiki Query: "errorCode"` 헤더 출력됨
- [ ] 관련 노트 1건 이상 표시됨
- [ ] 각 노트에 매칭 횟수 (`(N회 매칭)`) 표시됨
- [ ] 매칭 라인 미리보기 (최대 3줄) 출력됨

### C5-2. 매칭 없는 키워드

```bash
wiki-optimizer query xyznotexistsabc
```

- [ ] `관련 노트 없음` 메시지 출력됨 (에러 없음)

### C5-3. 빈 키워드 에러 처리

```bash
wiki-optimizer query
```

- [ ] `사용법: wiki-optimizer query <키워드>` 출력됨 + exit(1)

---

---

# Phase 2: MCP

> CLI가 만든 위키를 구조화된 도구로 읽으며 공유 코어(`lib/`)를 검증한다.  
> MCP는 **읽기/분석 전용**. 쓰기 없음 — 수정은 AI가 자신의 도구로 직접 처리한다.

---

## M0. 서버 연결 확인

**방법**: Claude Code에서 `/mcp` 커맨드 실행

- [ ] `wiki-optimizer` 서버가 목록에 표시됨
- [ ] 상태: Connected
- [ ] 6개 도구 목록:
  - [ ] `wiki_get_index`
  - [ ] `wiki_list_notes`
  - [ ] `wiki_read_note`
  - [ ] `wiki_get_links`
  - [ ] `wiki_search`
  - [ ] `wiki_lint`

**모듈 export 확인** (require.main 가드):

```bash
node -e "const m = require('./plugins/wiki-optimizer/servers/wiki-mcp-server.js'); console.log(m.TOOLS.length, typeof m.handleToolCall);"
```

- [ ] `6 function` 출력됨 (MCP 루프 실행 안 됨)

---

## M1. `wiki_get_index`

**호출** (인자 없음):

```json
{ "method": "tools/call", "params": { "name": "wiki_get_index", "arguments": {} } }
```

- [ ] `index.md` 전체 내용 반환됨
- [ ] Domain 섹션에 AST 자동 생성 노트 항목 포함됨
- [ ] Pattern / Concept 섹션 포함됨

**에러 케이스**: `.wiki/index.md` 없는 프로젝트

- [ ] `index.md가 없습니다. wiki-optimizer init 을 먼저 실행하세요.` 반환됨

---

## M2. `wiki_search`

**정상 검색**:

```json
{ "method": "tools/call", "params": { "name": "wiki_search", "arguments": { "keyword": "errorCode" } } }
```

- [ ] 관련 노트 목록 반환됨
- [ ] 매칭 횟수 포함됨
- [ ] 미리보기 라인 포함됨
- [ ] **C5-1 결과와 동일한 노트가 반환됨** (CLI와 MCP가 동일 코어 사용 확인)

**빈 keyword 거부**:

```json
{ "method": "tools/call", "params": { "name": "wiki_search", "arguments": { "keyword": "" } } }
```

- [ ] `isError: true` + `keyword 파라미터가 필요합니다.` 반환됨

**매칭 없는 키워드**:

- [ ] `관련 노트 없음` 메시지 반환됨 (isError 없음)

---

## M3. `wiki_list_notes` (NEW-6)

**전체 목록**:

```json
{ "method": "tools/call", "params": { "name": "wiki_list_notes", "arguments": {} } }
```

- [ ] `## domain (N건)` 섹션 출력됨
- [ ] 각 노트에 `- [[.wiki/domain/xxx]]` 경로 포함됨
- [ ] 제목, 갱신일(`2026-05-05`), 태그 포함됨

**카테고리 필터**:

```json
{ "method": "tools/call", "params": { "name": "wiki_list_notes", "arguments": { "category": "domain" } } }
```

- [ ] `domain` 섹션만 반환됨
- [ ] `concept` / `pattern` 노트 미포함됨

**잘못된 category**:

```json
{ "method": "tools/call", "params": { "name": "wiki_list_notes", "arguments": { "category": "invalid" } } }
```

- [ ] `isError: true` + `category는 concept, pattern, domain 중 하나여야 합니다.` 반환됨

---

## M4. `wiki_read_note` + `wiki_get_links` (NEW-7)

### M4-1. `wiki_read_note` — 정상

```json
{ "method": "tools/call", "params": { "name": "wiki_read_note", "arguments": { "note_path": ".wiki/domain/eq-equipment-repair-service" } } }
```

- [ ] 노트 전체 내용 반환됨
- [ ] `## 연결` 두 그룹 확인 (NEW-1):
  - [ ] `### 소스 파일` 그룹 포함됨
  - [ ] `### 관련 노트` 그룹 포함됨

### M4-2. `wiki_read_note` — 에러 케이스

**존재하지 않는 노트**:

```json
{ "method": "tools/call", "params": { "name": "wiki_read_note", "arguments": { "note_path": ".wiki/domain/ghost" } } }
```

- [ ] `isError: true` 반환됨

**경로 순회 시도** (보안):

```json
{ "method": "tools/call", "params": { "name": "wiki_read_note", "arguments": { "note_path": "../../etc/passwd" } } }
```

- [ ] `isError: true` + `잘못된 노트 경로` 반환됨

### M4-3. `wiki_get_links` — 링크 그래프 (NEW-7)

```json
{ "method": "tools/call", "params": { "name": "wiki_get_links", "arguments": { "note_path": ".wiki/domain/eq-equipment-repair-service" } } }
```

- [ ] `### 아웃고잉 위키 노트 (N건)` 섹션 존재
- [ ] `### 아웃고잉 소스 파일 (N건)` 섹션 존재
  - [ ] `[[src/eq/EqEquipmentRepairService.java]]` 포함됨
- [ ] `### 인커밍 (N건)` 섹션 존재
- [ ] `[[.wiki/index]]` / `[[.wiki/log]]`는 아웃고잉 위키 노트에서 제외됨

**경로 순회 시도** (보안):

```json
{ "method": "tools/call", "params": { "name": "wiki_get_links", "arguments": { "note_path": "../../etc/passwd" } } }
```

- [ ] `isError: true` 반환됨

---

## M5. `wiki_lint`

```json
{ "method": "tools/call", "params": { "name": "wiki_lint", "arguments": {} } }
```

- [ ] `Wiki Lint 리포트` 반환됨
- [ ] `### 고아 노트`, `### 깨진 링크`, `### 고립 노트` 3개 섹션 포함됨
- [ ] **C4-1 결과와 이슈 항목이 일치함** (CLI와 MCP가 동일 코어 사용 확인)

**참고**: `wiki_lint`는 리포트만 반환. 수정은 CLI `lint --fix` 또는 스킬 `/wiki-lint --fix` 전용.

---

---

# Phase 3: Plugin

> 훅 자동화 + 스킬/에이전트 LLM 워크플로우를 검증한다.  
> CLI가 만든 `.wiki/` 구조와 MCP로 확인한 내용을 기반으로 AI 기반 노트를 추가·보강한다.

---

## P1. 훅 동작

### P1-1. SessionStart 훅 — index 자동 주입

**방법**: 새 Claude Code 세션 시작

- [ ] system context에 `<wiki-index>` 블록 주입됨
- [ ] Domain 섹션에 C2에서 생성한 AST 노트 포함됨
- [ ] Pattern / Concept 섹션 포함됨

**에지 케이스**: `.wiki/` 없는 프로젝트에서 세션 시작

- [ ] `<wiki-index>` 없음, 에러 없음 (조용히 종료)

### P1-2. PreToolUse 훅 — 등록 도메인 Glob 차단

**방법**: `src/eq/**/*.java` Glob 시도

- [ ] `hookDecision: "block"` 반환됨
- [ ] `/wiki-query eq` 힌트 포함됨

### P1-3. PreToolUse 훅 — 미등록 도메인 Glob 통과

**방법**: `src/newdomain/**/*.java` Glob 시도

- [ ] `hookDecision: "continue"` + `/wiki-ingest` 제안 출력됨

### P1-4. PostToolUse 훅 — 미등록 도메인 Read 시 제안

**방법**: `src/inv/InvOnhandsController.java` Read

- [ ] `/wiki-ingest inv` 제안 출력됨

### P1-5. PostToolUse 훅 — 등록 도메인 Read 시 무음

**방법**: `src/eq/EqEquipmentRepairService.java` Read

- [ ] 출력 없음

### P1-6. Stop 훅 — 미등록 도메인 변경 시 제안

```bash
echo "public class InvInventoryService {}" > src/inv/InvInventoryService.java
```

**방법**: 세션 종료

- [ ] `/wiki-ingest inv` 제안 출력됨
- [ ] 등록된 도메인 (`eq`)은 제안 목록에서 제외됨

---

## P2. `/wiki-ingest` — LLM 노트 생성

### P2-1. domain ingest — `## 연결` 두 그룹 형식 (NEW-1)

**실행**: `/wiki-ingest eq-equipment-repair`

- [ ] wiki-reviewer → wiki-writer → wiki-indexer 3단계 디스패치됨
- [ ] `.wiki/domain/eq-equipment-repair.md` 생성됨
- [ ] YAML frontmatter 5개 필드 존재
- [ ] **`## 연결` 두 그룹 형식** 준수:

```bash
grep -A 10 "## 연결" .wiki/domain/eq-equipment-repair.md
```

  - [ ] `### 소스 파일` — `[[src/eq/EqEquipmentRepairService.java]]` 포함
  - [ ] `### 관련 노트` — `[[.wiki/index]]` 포함
  - [ ] 두 그룹 합산 최소 2개 wikilink
- [ ] index.md Domain 섹션 갱신됨
- [ ] log.md 최상단에 항목 prepend됨

### P2-2. concept ingest

**실행**: `/wiki-ingest BaseDTO`

- [ ] 분류: `concept/`, `id: concept_base-dto`
- [ ] `source_files`에 `src/common/BaseDTO.java` 포함됨
- [ ] `## 연결 / ### 소스 파일`에 `[[src/common/BaseDTO.java]]` 포함됨

### P2-3. 코드 언어 정합성

**실행**: `/wiki-ingest eq-equipment-repair` (Java 소스)

- [ ] 코드 예시가 Java로 작성됨 (```java 코드블록)

### P2-4. 기존 노트 업데이트

**실행**: `/wiki-ingest eq-equipment-repair` (이미 존재)

- [ ] 신규 생성 아닌 업데이트 처리됨
- [ ] `last_updated` 갱신됨
- [ ] `#auto-generated` 태그 제거됨
- [ ] log.md에 update 항목 기록됨

---

## P3. `/wiki-query`

### P3-1. 기본 쿼리

**실행**: `/wiki-query BaseDTO errorCode 처리 방법`

- [ ] wiki-reviewer 디스패치 → 관련 페이지 식별
- [ ] 출처 wikilink 명시됨 (`[[.wiki/concept/base-dto]]`)

### P3-2. 위키 미비 시 ingest 제안

**실행**: `/wiki-query InvInventoryService 처리 흐름`

- [ ] 소스 탐색 후 `/wiki-ingest` 제안 출력됨

---

## P4. `/wiki-lint`

### P4-1. 기본 lint

**실행**: `/wiki-lint`

- [ ] wiki-reviewer 디스패치 → 고아/깨진/고립 노트 리포트
- [ ] **M5 결과와 이슈 항목이 일치함** (Plugin ↔ MCP 일관성 확인)

### P4-2. 고아 노트 탐지

```bash
echo "# Orphan" > .wiki/domain/orphan-note.md
```

- [ ] `orphan-note` 탐지됨

### P4-3. `--fix` 모드

**실행**: `/wiki-lint --fix`

- [ ] wiki-writer + wiki-indexer 디스패치됨
- [ ] 고아 노트 index.md 등록 처리됨
- [ ] 연결 없는 노트에 역링크 추가됨

### P4-4. `--deep` 모드

```bash
echo "  public void deleteRepair(String id) {}" >> src/eq/EqEquipmentRepairService.java
git add . && git commit -m "feat: add deleteRepair"
```

**실행**: `/wiki-lint --deep`

- [ ] git log 30일치 변경과 위키 `last_updated` 비교됨
- [ ] 소스 변경 후 미갱신 노트 탐지됨

---

---

## R1. 회귀 — v1.2.x 이전 수정 유지 확인

| 항목 | 수정 버전 | 검증 섹션 | 체크 |
|------|---------|----------|------|
| CRLF CLAUDE.md 섹션 삽입 (BUG-8) | v1.2.5 | T1 mcp-server | - [ ] 회귀 없음 |
| `cmdStatus()` rules 파일 체크 (BUG-3) | v1.2.3 | C2-6 | - [ ] 회귀 없음 |
| Stop 훅 슬래시 경로 매칭 (BUG-4) | v1.2.3 | P1-6 | - [ ] 회귀 없음 |
| `--version` / `-v` (BUG-5) | v1.2.3 | C1-1 | - [ ] 회귀 없음 |
| `help` ingest 포함 (BUG-6) | v1.2.3 | C1-2 | - [ ] 회귀 없음 |
| 미초기화 status 안내 (BUG-7) | v1.2.3 | C1-3 | - [ ] 회귀 없음 |
| 훅 중복 등록 방지 | v1.2.2 | C2-7 | - [ ] 회귀 없음 |
| `${CLAUDE_PLUGIN_ROOT}` 절대경로 치환 | v1.2.2 | C2-2 | - [ ] 회귀 없음 |

---

## 예상 이슈 및 주의사항

| 항목 | 상황 | 대처 |
|------|------|------|
| C2-3 AST ID 형식 | 클래스명에 따라 kebab-case 변환이 예상과 다를 수 있음 | `ls .wiki/domain/` 로 실제 파일명 확인 후 검증 |
| C2-5 tree-sitter 미설치 | `web-tree-sitter` 없으면 C2-3 불가 | 경고 후 skip 동작만 확인 (C2-5) |
| M0 서버 미연결 | `.mcp.json`의 `${CLAUDE_PLUGIN_ROOT}` 미치환 시 | 수동으로 절대경로로 수정 후 재연결 |
| M4 MCP 수동 호출 | JSON-RPC raw 입력이 어려우면 | Claude Code에서 도구명으로 호출 요청 |
| P2-3 코드 언어 | wiki-writer가 언어 컨텍스트 무시 시 | `agents/wiki-writer.md` 코드 언어 규칙 재확인 |
| P4-1 vs M5 일관성 | 이슈 수가 다를 경우 | Plugin은 LLM 판단이 다를 수 있음 — 고아/깨진 링크는 일치해야 함 |

---

## 테스트 결과 파일명

`2026-05-05-wiki-optimizer-test-results-v2.1.0.md`
