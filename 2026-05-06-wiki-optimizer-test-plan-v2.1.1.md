# wiki-optimizer v2.1.1 테스트 플랜

**작성일**: 2026-05-06  
**대상**: v2.1.0 → v2.1.1 수정 사항 검증 + 회귀 확인  
**플러그인 버전**: 2.1.1  
**이전 결과 참조**: `2026-05-05-wiki-optimizer-test-results-v2.1.0.md`

---

## v2.1.1 수정 항목

| ID | 항목 | 수정 내용 |
|----|------|----------|
| BUG-10 | MCP 프로젝트 루트 미탐지 | `findProjectRoot()` 추가 — 미치환 템플릿 리터럴 거부 후 `.wiki/` 탐색. `.mcp.json` env 섹션 제거 |
| INFO-1 | tree-sitter WASM 초기화 실패 | `locateFile` 옵션으로 `tree-sitter.wasm` 절대경로 명시. WASM 파일 존재 여부 사전 확인 |

---

## 검증 전략

v2.1.0에서 통과한 항목은 **빠른 회귀 확인**만 수행한다.  
v2.1.0에서 실패하거나 SKIP된 항목은 **전체 재검증**한다.

| 단계 | 섹션 | 검증 방식 | v2.1.0 결과 |
|------|------|----------|------------|
| 준비 | T0 | 전체 | — |
| 준비 | T1 | 전체 (136 passed 확인) | ✅ |
| **CLI** | **C2** | **전체 재검증** (INFO-1 수정) | ⚠ T1-2 SKIP |
| CLI | C1, C3~C5 | 빠른 회귀 | ✅ |
| **MCP** | **M0~M5** | **전체 재검증** (BUG-10 수정) | ⚠ T7 부분 통과 |
| Plugin | P1~P4 | 빠른 회귀 | ✅ |
| 회귀 | R1 | BUG-10 / INFO-1 수정 고정 확인 | — |

---

## T0. 샌드박스 환경 준비

### T0-1. 샌드박스 프로젝트 생성 (v2.1.0과 동일)

```bash
mkdir ~/sandbox-wiki-v211 && cd ~/sandbox-wiki-v211
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
wiki-optimizer --version
```

- [ ] `2.1.1` 출력됨
- [ ] `package.json`, `plugin.json`, `bin/wiki-optimizer.js` 세 파일 모두 `2.1.1`

---

## T1. `npm test` — 136 passed 확인

```bash
cd ~/path/to/wiki-optimizer
npm test
```

- [ ] **136 passed, 0 failed**

---

---

# Phase 1: CLI

---

## C1. 기본 커맨드 (빠른 회귀)

```bash
wiki-optimizer --version   # → 2.1.1
wiki-optimizer help        # → 6개 커맨드 포함
wiki-optimizer status      # → 미초기화 안내
```

- [ ] `--version` → `2.1.1`
- [ ] `help` → `init`, `status`, `ingest`, `lint`, `query`, `help` 포함
- [ ] `status` → 미초기화 안내 출력

---

## C2. `init` — tree-sitter AST 노트 생성 전체 재검증 (INFO-1 수정)

> v2.1.0에서 WASM 초기화 실패로 SKIP됐던 항목.  
> v2.1.1에서 `locateFile` 옵션 적용.

```bash
wiki-optimizer init
```

### C2-1. 기본 구조 생성 (회귀)

- [ ] `.wiki/{concept,pattern,domain}/`, `index.md`, `log.md`, `config.json`, `.wikiignore` 생성됨
- [ ] rules 3파일 복사됨
- [ ] 훅 4종 등록됨, `${CLAUDE_PLUGIN_ROOT}` 절대경로 치환 확인

### C2-2. tree-sitter AST 노트 생성 ★ (INFO-1 핵심 검증)

- [ ] `Analyzing project structure (tree-sitter)...` 메시지 출력됨
- [ ] **`Generated N wiki notes in .wiki/domain/ (N skipped)` 출력됨** (0이면 실패)

```bash
ls .wiki/domain/
```

- [ ] `.wiki/domain/` 하위에 `.md` 파일 1개 이상 존재

**생성된 노트 내용 검증** (예: `eq-equipment-repair-service.md`):

```bash
cat .wiki/domain/eq-equipment-repair-service.md
```

- [ ] YAML frontmatter — `id: domain_eq-equipment-repair-service`, `tags: ["#domain", "#auto-generated"]`
- [ ] `## 감지된 클래스` — `@Service`, `extends BaseService` 표시됨
- [ ] `## 감지된 메서드` — `findRepairList`, `saveRepair` 포함
- [ ] `## 감지된 필드` — `repairRepository` 포함
- [ ] `## 연결 / ### 소스 파일` — `[[src/eq/EqEquipmentRepairService.java]]` 포함
- [ ] `## 연결 / ### 관련 노트` — `[[.wiki/index]]` 포함
- [ ] `index.md` Domain 섹션에 자동 등록됨

### C2-3. INFO-1 수정 메커니즘 확인

```bash
node -e "
const path = require('path');
const fs = require('fs');
const Parser = require('web-tree-sitter');
const dir = path.dirname(require.resolve('web-tree-sitter'));
const wasm = path.join(dir, 'tree-sitter.wasm');
console.log('WASM exists:', fs.existsSync(wasm));
Parser.init({ locateFile: (f) => path.join(dir, f) }).then(() => console.log('init OK')).catch(e => console.log('init FAIL:', e.message));
" 2>&1
```

- [ ] `WASM exists: true` 출력됨
- [ ] `init OK` 출력됨

### C2-4. 재실행 멱등성 (회귀)

```bash
wiki-optimizer init
```

- [ ] 파일 덮어쓰지 않음, 훅 중복 등록 안 됨
- [ ] `Generated 0 wiki notes ... (N skipped)` 출력됨

---

## C3. `ingest` (빠른 회귀)

```bash
wiki-optimizer ingest --watch src/
```

- [ ] `entity:done EqEquipmentRepairService` 출력됨
- [ ] `entity:done InvOnhandsController` 출력됨
- [ ] 마지막 라인 `ingest:complete`

---

## C4. `lint` (빠른 회귀)

```bash
# 깨진 링크 삽입 후 lint / fix
echo "- [[.wiki/domain/ghost-note]]" >> .wiki/domain/eq-equipment-repair-service.md
wiki-optimizer lint
wiki-optimizer lint --fix
grep "ghost-note" .wiki/domain/eq-equipment-repair-service.md
```

- [ ] `ghost-note` 깨진 링크 탐지됨
- [ ] `--fix` 후 `grep` 결과 없음

---

## C5. `query` (빠른 회귀)

```bash
wiki-optimizer query errorCode
```

- [ ] `🔍 Wiki Query: "errorCode"` 출력됨
- [ ] 관련 노트 1건 이상 표시됨

---

---

# Phase 2: MCP

> BUG-10 수정 후 전체 재검증.  
> v2.1.0에서 정상케이스 전부 실패했던 항목이다.

---

## M0. 서버 연결 + 프로젝트 루트 탐지 확인 (BUG-10 핵심 검증)

**방법**: Claude Code에서 `/mcp` 커맨드 실행

- [ ] `wiki-optimizer` 서버 Connected 표시됨
- [ ] 6개 도구 목록 확인 (`wiki_get_index`, `wiki_list_notes`, `wiki_read_note`, `wiki_get_links`, `wiki_search`, `wiki_lint`)

**`findProjectRoot()` 동작 확인** — 미치환 템플릿 리터럴 처리:

```bash
# ${workspaceFolder} 리터럴 전달 시 fallback 동작 확인
WIKI_PROJECT_DIR='${workspaceFolder}' node -e "
const path = require('path');
const fs = require('fs');
const envDir = process.env.WIKI_PROJECT_DIR;
const rejected = envDir && envDir.startsWith('\${');
console.log('rejected:', rejected);  // → true
"
```

- [ ] `rejected: true` 출력됨

**`.wiki/` 탐색 경로 확인**:

```bash
# 샌드박스 루트에서 실행 — .wiki/가 존재하므로 현재 디렉토리 반환
node -e "
const fs = require('fs');
const path = require('path');
function findProjectRoot() {
  let dir = process.cwd();
  for (let i = 0; i < 10; i++) {
    if (fs.existsSync(path.join(dir, '.wiki'))) return dir;
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  return process.cwd();
}
console.log(findProjectRoot());
" 2>&1
```

- [ ] 샌드박스 프로젝트 루트 경로 출력됨 (`.wiki/` 있는 디렉토리)

---

## M1. `wiki_get_index` ★ (BUG-10 수정 후 첫 정상케이스 검증)

```json
{ "method": "tools/call", "params": { "name": "wiki_get_index", "arguments": {} } }
```

- [ ] `index.md` 전체 내용 반환됨 (**v2.1.0에서 실패했던 항목**)
- [ ] Domain 섹션에 AST 자동 생성 노트 항목 포함됨

**에러케이스 (회귀)**:

- [ ] `.wiki/` 없는 환경에서 `index.md가 없습니다.` 반환됨

---

## M2. `wiki_search` ★ (BUG-10 수정 후 정상케이스 검증)

```json
{ "method": "tools/call", "params": { "name": "wiki_search", "arguments": { "keyword": "errorCode" } } }
```

- [ ] 관련 노트 반환됨 (**v2.1.0에서 실패했던 항목**)
- [ ] **C5 결과와 동일한 노트가 반환됨** (CLI ↔ MCP 공유 코어 확인)

**에러케이스 (회귀)**:

- [ ] 빈 keyword → `isError: true`
- [ ] 매칭 없는 keyword → `관련 노트 없음` (에러 아님)

---

## M3. `wiki_list_notes` ★ (BUG-10 수정 후 정상케이스 검증)

**전체 목록**:

```json
{ "method": "tools/call", "params": { "name": "wiki_list_notes", "arguments": {} } }
```

- [ ] `## domain (N건)` 섹션 + 노트 목록 반환됨 (**v2.1.0에서 `위키 노트가 없습니다` 반환됐던 항목**)
- [ ] 각 노트에 경로, 제목, 갱신일, 태그 포함됨

**카테고리 필터 (회귀)**:

```json
{ "method": "tools/call", "params": { "name": "wiki_list_notes", "arguments": { "category": "domain" } } }
```

- [ ] domain 노트만 반환됨

**에러케이스 (회귀)**:

- [ ] 잘못된 category → `isError: true`

---

## M4. `wiki_read_note` + `wiki_get_links` ★ (BUG-10 수정 후 정상케이스 검증)

### M4-1. `wiki_read_note` 정상케이스

```json
{ "method": "tools/call", "params": { "name": "wiki_read_note", "arguments": { "note_path": ".wiki/domain/eq-equipment-repair-service" } } }
```

- [ ] 노트 전체 내용 반환됨 (**v2.1.0에서 실패했던 항목**)
- [ ] `## 연결 / ### 소스 파일` / `### 관련 노트` 두 그룹 확인

### M4-2. `wiki_get_links` 정상케이스

```json
{ "method": "tools/call", "params": { "name": "wiki_get_links", "arguments": { "note_path": ".wiki/domain/eq-equipment-repair-service" } } }
```

- [ ] `### 아웃고잉 소스 파일` — `[[src/eq/EqEquipmentRepairService.java]]` 포함됨 (**v2.1.0에서 실패했던 항목**)
- [ ] `### 인커밍` 섹션 존재

**에러케이스 (회귀)**:

- [ ] 존재하지 않는 노트 → `isError: true`
- [ ] `../../etc/passwd` → `isError: true`

---

## M5. `wiki_lint` ★ (BUG-10 수정 후 정상케이스 검증)

```json
{ "method": "tools/call", "params": { "name": "wiki_lint", "arguments": {} } }
```

- [ ] `Wiki Lint 리포트` + 3개 섹션 반환됨
- [ ] **C4 lint 결과와 이슈 항목 일치** (CLI ↔ MCP 공유 코어 확인)

---

---

# Phase 3: Plugin (빠른 회귀)

> v2.1.0에서 전부 통과. 수정 사항과 무관하여 핵심 시나리오만 확인한다.

---

## P1. 훅 동작 (빠른 회귀)

- [ ] **SessionStart**: `<wiki-index>` 블록 주입됨, AST 생성 노트 포함됨
- [ ] **PreToolUse**: `src/eq/**/*.java` → `hookDecision: "block"`
- [ ] **PostToolUse**: 미등록 도메인 Read → `/wiki-ingest` 제안
- [ ] **Stop**: 미등록 파일 변경 후 세션 종료 → `/wiki-ingest` 제안

---

## P2. `/wiki-ingest` — `## 연결` 두 그룹 형식 (빠른 회귀)

**실행**: `/wiki-ingest eq-equipment-repair`

```bash
grep -A 8 "## 연결" .wiki/domain/eq-equipment-repair.md
```

- [ ] `### 소스 파일` — `[[src/eq/EqEquipmentRepairService.java]]` 포함
- [ ] `### 관련 노트` — `[[.wiki/index]]` 포함
- [ ] index.md 갱신됨, log.md prepend됨

---

## P3. `/wiki-query` (빠른 회귀)

**실행**: `/wiki-query BaseDTO errorCode`

- [ ] wiki-reviewer 디스패치됨, 출처 wikilink 포함됨

---

## P4. `/wiki-lint` — MCP 결과와 일관성 확인

**실행**: `/wiki-lint`

- [ ] wiki-reviewer 디스패치됨
- [ ] **M5 결과와 고아/깨진 링크 이슈 항목 일치**

---

---

## R1. 수정 항목 회귀 고정 확인

| 수정 | 버전 | 검증 섹션 | 체크 |
|------|------|----------|------|
| BUG-10: `findProjectRoot()` — 미치환 템플릿 거부 | v2.1.1 | M0 | - [ ] |
| BUG-10: `findProjectRoot()` — `.wiki/` 탐색 | v2.1.1 | M0 | - [ ] |
| BUG-10: `.mcp.json` env 섹션 제거 | v2.1.1 | M0 | - [ ] |
| INFO-1: `locateFile` 옵션 적용 | v2.1.1 | C2-2, C2-3 | - [ ] |
| INFO-1: WASM 파일 존재 사전 확인 | v2.1.1 | C2-3 | - [ ] |
| CRLF CLAUDE.md (BUG-8) | v1.2.5 | T1 npm test | - [ ] 회귀 없음 |
| 훅 중복 등록 방지 | v1.2.2 | C2-4 | - [ ] 회귀 없음 |

---

## 테스트 결과 파일명

`2026-05-06-wiki-optimizer-test-results-v2.1.1.md`
