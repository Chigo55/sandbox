# wiki-optimizer v1.2.4 전체 기능 테스트 플랜

**작성일**: 2026-05-04  
**대상**: 샌드박스 프로젝트에서 플러그인 업데이트 후 전체 기능 검증  
**플러그인 버전**: 1.2.4  
**이전 결과 참조**: `2026-05-04-wiki-optimizer-test-results-v1.2.3.md`

---

## v1.2.4 신규 수정 항목 (주요 검증 포인트)

| ID | 내용 | 검증 섹션 |
|----|------|----------|
| IMP-1 | `cmdInit()` CLAUDE.md 존재 시 위키 링크 추가 여부 대화형 프롬프트 구현 | T1-2 |
| IMP-2 | `npm test` Windows 셸 호환성 수정 (`for` 루프 → `node tests/run-all.js`) | T9-1 |
| IMP-3 | `rules/format.md`, `rules/operations.md` 예시 wikilink `cud-service` → `example-pattern` 교체 | T7 |

---

## 검증 범위

| 섹션 | 내용 |
|------|------|
| T0 | 샌드박스 환경 준비 & 플러그인 업데이트 |
| T1 | `/wiki-init` — CLAUDE.md 대화형 프롬프트(신규), 멱등성 |
| T2 | 훅 동작 — SessionStart / PreToolUse / PostToolUse / Stop |
| T3 | `/wiki-ingest` — domain / concept / pattern / 업데이트 |
| T4 | `/wiki-query` — 기본 쿼리 / 위키 미비 처리 |
| T5 | `/wiki-lint` — 기본 / --fix / --deep |
| T6 | CLI (`wiki-optimizer`) — version / help / status / init / ingest |
| T7 | rules 파일 형식 준수 & 예시 링크 교체 확인 (IMP-3) |
| T8 | 회귀 — v1.2.3 BUG-3~7 수정 유지 확인 |
| T9 | `npm test` Windows 실행 (IMP-2) |

---

## T0. 샌드박스 환경 준비 & 플러그인 업데이트

### T0-1. 샌드박스 프로젝트 생성

```bash
mkdir ~/sandbox-wiki-v124 && cd ~/sandbox-wiki-v124
git init
git config user.email "test@example.com"
git config user.name "Test"

mkdir -p src/eq src/inv src/common

cat > src/eq/EqEquipmentRepairService.java << 'EOF'
public class EqEquipmentRepairService {
  public List<RepairDTO> findRepairList(String eqId) { return null; }
  public void saveRepair(RepairDTO dto) {}
}
EOF

cat > src/inv/InvOnhandsController.java << 'EOF'
public class InvOnhandsController {
  public ModelAndView list(InvOnhandsDTO dto) { return null; }
}
EOF

cat > src/common/BaseDTO.java << 'EOF'
public class BaseDTO {
  private String errorCode;
  private String errorMsg;
}
EOF

git add .
git commit -m "feat: initial sandbox source files"
mkdir .claude
```

### T0-2. 플러그인 업데이트 & 버전 확인

```bash
/plugin update wiki-optimizer
# 또는 수동
node ~/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.4/bin/wiki-optimizer.js --version
```

**체크리스트**:
- [ ] 버전 `1.2.4` 출력됨
- [ ] `package.json`, `plugin.json`, `bin/wiki-optimizer.js` 세 파일 모두 `1.2.4`

---

## T1. `/wiki-init` 테스트

### T1-1. 최초 초기화 (CLAUDE.md 없는 경우)

**전제**: CLAUDE.md 없음, `.wiki/` 없음

**실행**: `/wiki-init`

**체크리스트**:

**구조 생성**:
- [ ] `.wiki/concept/`, `.wiki/pattern/`, `.wiki/domain/` 생성됨
- [ ] `.wiki/index.md`, `.wiki/log.md` 생성됨
- [ ] `.wiki/config.json` 생성됨
- [ ] `.wikiignore` 생성됨 (`node_modules/`, `dist/`, `.git/` 포함)

**rules 복사**:
- [ ] `.wiki/rules/format.md` 복사됨
- [ ] `.wiki/rules/operations.md` 복사됨
- [ ] `.wiki/rules/templates.md` 복사됨

**훅 등록**:
- [ ] `.claude/settings.local.json`에 SessionStart / PreToolUse / PostToolUse / Stop 4종 훅 등록됨

**CLAUDE.md 없는 경우 처리**:
- [ ] "CLAUDE.md가 없습니다" 경고 메시지 출력됨
- [ ] 초기화는 정상 완료됨 (프로세스 종료 안 됨)

### T1-2. CLAUDE.md 있는 경우 대화형 프롬프트 (IMP-1 수정)

**전제**: `echo "# My Project" > CLAUDE.md` 생성, `.wiki/` 수동 삭제 후 재실행

**실행**: `/wiki-init`

**체크리스트 — 프롬프트 출력**:
- [ ] "CLAUDE.md에 위키 링크를 추가하시겠습니까? (y/N)" 프롬프트 출력됨 **(IMP-1 핵심)**
- [ ] 프롬프트가 초기화 완료 후 마지막에 출력됨

**응답 `y` 시**:
- [ ] CLAUDE.md에 `[Wiki Index](.wiki/index.md)` 링크 추가됨
- [ ] `## 참고 문서` 섹션에 삽입됨 (섹션 없으면 새로 생성)
- [ ] 기존 CLAUDE.md 내용 보존됨 (`# My Project` 유지)
- [ ] "CLAUDE.md에 위키 링크 추가됨" 메시지 출력됨

**응답 `N` 또는 Enter(기본값) 시**:
- [ ] CLAUDE.md 수정 없음
- [ ] "CLAUDE.md 수정 건너뜀" 메시지 출력됨
- [ ] 초기화는 정상 완료됨

**이미 링크 있는 경우**:
- [ ] 프롬프트 없이 "CLAUDE.md에 위키 링크 이미 포함됨" skip 메시지 출력됨

### T1-3. CLAUDE.md에 `## 참고 문서` 섹션이 이미 있는 경우

**전제**: CLAUDE.md에 아래 내용 포함 후 `/wiki-init` 실행:
```markdown
## 참고 문서
- [기존 문서](./docs/)
```

**응답 `y` 시**:
- [ ] 기존 `## 참고 문서` 섹션 내부에 위키 링크 삽입됨 (섹션 중복 생성 안 됨)
- [ ] 기존 항목 (`- [기존 문서](./docs/)`) 보존됨

### T1-4. tree-sitter 초기 노트 생성

**체크리스트**:
- [ ] `.wiki/domain/` 아래 Java 소스 파일 기반 노트 생성됨
- [ ] 각 노트에 `#auto-generated` 태그 포함
- [ ] tree-sitter 미설치 시 경고 후 skip, wiki 정상 동작

### T1-5. 재실행 멱등성

**실행**: `/wiki-init` 재실행 (`.wiki/` 이미 존재)

**체크리스트**:
- [ ] 기존 파일 덮어쓰지 않음 (`skip` 메시지)
- [ ] 훅 중복 등록 안 됨 (`Already registered` ×4)

---

## T2. 훅 동작 테스트

> **전제**: T1 완료, `.wiki/index.md`에 `eq` 도메인 등록됨

### T2-1. SessionStart 훅

**방법**: 새 Claude Code 세션 시작

**체크리스트**:
- [ ] system context에 `<wiki-index>` 블록 주입됨
- [ ] Domain / Pattern / Concept 섹션 포함됨
- [ ] index.md 비어있으면 조용히 종료

### T2-2. PreToolUse 훅 — 등록 도메인 Glob 차단

**방법**: `src/eq/**/*.java` Glob 시도

**체크리스트**:
- [ ] `hookDecision: "block"` 반환됨
- [ ] `/wiki-query` 힌트 출력됨

**통과 케이스**:
- `src/**/*.java` → `hookDecision: "continue"`
- `src/eq/EqEquipmentRepairService.java` (단일 파일) → `hookDecision: "continue"`

### T2-3. PreToolUse 훅 — 미등록 도메인 Glob

**방법**: `src/newdomain/**/*.java` Glob 시도

**체크리스트**:
- [ ] `hookDecision: "continue"` + `/wiki-ingest` 제안 출력됨

### T2-4. PostToolUse 훅 — 미등록 도메인 Read

**방법**: `src/inv/InvOnhandsController.java` Read

**체크리스트**:
- [ ] `/wiki-ingest inv` 제안 출력됨

### T2-5. PostToolUse 훅 — 등록 도메인 Read

**방법**: `src/eq/EqEquipmentRepairService.java` Read

**체크리스트**:
- [ ] 출력 없음 (등록된 도메인)

### T2-6. Stop 훅 — 미등록 도메인 변경 시 제안

```bash
echo "public class InvInventoryService {}" > src/inv/InvInventoryService.java
```

**방법**: Claude Code 세션 종료

**체크리스트**:
- [ ] `/wiki-ingest` 제안 출력됨
- [ ] 등록된 도메인 (`eq`)은 제안 목록에서 제외됨

---

## T3. `/wiki-ingest` 테스트

### T3-1. 도메인 ingest — 신규 생성

**실행**: `/wiki-ingest eq-equipment-repair`

**체크리스트**:
- [ ] wiki-reviewer → wiki-writer → wiki-indexer 3단계 디스패치됨
- [ ] `.wiki/domain/eq-equipment-repair.md` 생성됨
- [ ] YAML frontmatter 5개 필드 (`id`, `last_updated`, `source_files`, `dependencies`, `tags`)
- [ ] `## 개요`, `## 주요 로직`, `## 연결` 섹션 포함
- [ ] `## 연결` 최소 2개 wikilink, 각 `— 이유` 설명
- [ ] wikilink: `[[.wiki/category/page-name]]` 형식 (`.md` 없음)
- [ ] `.wiki/index.md` Domain 섹션 행 추가됨
- [ ] `.wiki/log.md` 최상단에 ingest 항목 prepend됨

### T3-2. concept ingest

**실행**: `/wiki-ingest BaseDTO`

**체크리스트**:
- [ ] 분류: `concept/`
- [ ] `.wiki/concept/base-dto.md` 생성됨, `id: concept_base-dto`
- [ ] `## 정의`, `## 상세`, `## 연결` 포함
- [ ] index.md Concept 섹션 등록됨

### T3-3. pattern ingest

**실행**: `/wiki-ingest pattern:BaseDTO상속패턴`

**체크리스트**:
- [ ] `.wiki/pattern/base-dto-inheritance.md` 생성됨
- [ ] `## 문제`, `## 해법`, `## 연결` 포함
- [ ] 코드 예시 Java로 작성됨

### T3-4. 기존 auto-generated 노트 업데이트

**실행**: `/wiki-ingest eq-equipment-repair` (이미 존재)

**체크리스트**:
- [ ] 신규 생성 아닌 업데이트
- [ ] `last_updated` 갱신, `#auto-generated` 태그 제거
- [ ] log.md에 갱신 항목 기록됨

### T3-5. 존재하지 않는 도메인

**실행**: `/wiki-ingest nonexistent-domain`

**체크리스트**:
- [ ] 소스 없음 안내 출력, 빈 노트 생성 안 됨

---

## T4. `/wiki-query` 테스트

### T4-1. 기본 쿼리

**실행**: `/wiki-query BaseDTO errorCode 처리 방법`

**체크리스트**:
- [ ] wiki-reviewer 디스패치 → 관련 페이지 식별
- [ ] 출처 wikilink 명시 (`[[.wiki/concept/base-dto]]`)

### T4-2. 위키 미비 시 ingest 제안

**실행**: `/wiki-query 위키에 없는 InvInventoryService 동작`

**체크리스트**:
- [ ] "위키에 없음" 안내 + `/wiki-ingest` 제안 출력됨

---

## T5. `/wiki-lint` 테스트

### T5-1. 기본 lint

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] 고아 노트 / 깨진 링크 / 고립 노트 탐지 실행
- [ ] 리포트 출력

### T5-2. 고아 노트 탐지

```bash
echo "# Orphan" > .wiki/domain/orphan-note.md
```

**체크리스트**:
- [ ] `orphan-note` 탐지됨

### T5-3. 깨진 링크 탐지

```bash
echo "- [[.wiki/pattern/nonexistent-pattern]] — 테스트용" >> .wiki/domain/eq-equipment-repair.md
```

**체크리스트**:
- [ ] 깨진 링크 탐지됨

### T5-4. `--fix` 모드

**실행**: `/wiki-lint --fix`

**체크리스트**:
- [ ] 자동 수정 항목 처리됨
- [ ] log.md에 lint 항목 기록됨

### T5-5. `--deep` 모드

```bash
echo "  public List<RepairDTO> findAll() { return null; }" >> src/eq/EqEquipmentRepairService.java
git add . && git commit -m "feat: add findAll"
```

**실행**: `/wiki-lint --deep`

**체크리스트**:
- [ ] 소스 변경 후 위키 미갱신 노트 감지
- [ ] `/wiki-ingest eq-equipment-repair` 제안 출력됨

---

## T6. CLI (`wiki-optimizer`) 테스트

### T6-1. `--version` / `-v`

**체크리스트**:
- [ ] `wiki-optimizer --version` → `1.2.4` 출력됨
- [ ] `wiki-optimizer -v` → `1.2.4` 출력됨

### T6-2. `help`

**체크리스트**:
- [ ] `init`, `status`, `ingest`, `help` 4개 명령어 설명 포함

### T6-3. `status` — 초기화 완료 상태

**체크리스트**:
- [ ] `.wiki/` 구조 표시됨
- [ ] 훅 등록 상태 표시됨
- [ ] rules 3파일 (`format.md`, `operations.md`, `templates.md`) 정상 인식

### T6-4. `status` — 미초기화 상태

**체크리스트**:
- [ ] "초기화되지 않았습니다" 안내 출력됨
- [ ] `wiki-optimizer init` 실행 안내 포함

### T6-5. `init` CLI 직접 실행

**체크리스트**:
- [ ] `.wiki/` 구조 생성됨
- [ ] CLAUDE.md 처리 로직 실행됨 (T1-2 동일)

### T6-6. `ingest <path>`

**실행**: `wiki-optimizer ingest src/eq/`

**체크리스트**:
- [ ] `src/eq/` 하위 소스 파일 스캔됨

---

## T7. rules 파일 형식 준수 & IMP-3 확인

**전제**: T3 완료 (domain, concept, pattern 노트 존재)

### T7-1. 생성된 노트 포맷 준수

**체크리스트**:
- [ ] `id` 형식: `domain_eq-equipment-repair`, `concept_base-dto`, `pattern_base-dto-inheritance`
- [ ] wikilink: `[[.wiki/category/page-name]]` (`.md` 없음, 절대경로 아님)
- [ ] `## 연결` 최소 2개 wikilink, 각 `— 이유` 설명
- [ ] log.md 항목 항상 최상단 prepend

### T7-2. 복사된 rules 파일 예시 링크 확인 (IMP-3)

**체크리스트**:
- [ ] `.wiki/rules/format.md`에 `cud-service` wikilink **없음** (IMP-3 수정)
- [ ] `.wiki/rules/format.md`에 `example-pattern` 예시 사용됨
- [ ] `.wiki/rules/operations.md`에 `cud-service` wikilink **없음**
- [ ] `.wiki/rules/operations.md`에 `example-pattern` 예시 사용됨

> **참고**: `/wiki-lint --deep` 실행 시 rules/ 파일의 예시 링크(`[[.wiki/pattern/example-pattern]]`)가 탐지될 수 있음 — rules/는 문서 파일이므로 정상 동작

---

## T8. 회귀 — v1.2.3 수정 항목 유지 확인

| 항목 | v1.2.3 해소 | v1.2.4 유지 확인 |
|------|------------|-----------------|
| BUG-3 `cmdStatus()` rules 파일 체크 | ✅ | - [ ] 회귀 없음 (T6-3) |
| BUG-4 Stop 훅 슬래시 경로 매칭 | ✅ | - [ ] 회귀 없음 (T2-6) |
| BUG-5 `--version` / `-v` | ✅ | - [ ] 회귀 없음 (T6-1) |
| BUG-6 `help` ingest 포함 | ✅ | - [ ] 회귀 없음 (T6-2) |
| BUG-7 미초기화 status 안내 | ✅ | - [ ] 회귀 없음 (T6-4) |

---

## T9. `npm test` Windows 실행 (IMP-2)

### T9-1. `npm test` Windows 직접 실행

**실행** (플러그인 디렉토리에서, Windows PowerShell):

```powershell
cd plugins/wiki-optimizer
npm test
```

**체크리스트**:
- [ ] `npm test` 실행 시 오류 없이 정상 완료됨 **(IMP-2 핵심: 이전에는 bash for 루프 오류)**
- [ ] `node tests/run-all.js` 가 실행됨 (`package.json` scripts 확인)
- [ ] 전체 테스트 통과 (0 failed)
- [ ] 기존 102 passed 이상 유지

**개별 테스트 통과 확인**:

| 테스트 파일 | v1.2.3 기준 |
|------------|------------|
| `test-agents.js` | 28 passed |
| `test-wiki-reader.js` | 9 passed |
| `test-pattern-detector.js` | 18 passed |
| `test-register-hooks.js` | 12 passed |
| `test-pre-read-hook.js` | 6 passed |
| `test-post-read-hook.js` | 7 passed |
| `test-session-start-hook.js` | 6 passed |
| `test-stop-hook.js` | 6 passed |
| `test-ingest-watch.js` | 10 passed |

### T9-2. `run-all.js` 동작 확인

**체크리스트**:
- [ ] `tests/run-all.js` 파일 존재
- [ ] `tests/test-*.js` 파일을 알파벳 순으로 자동 수집하여 실행
- [ ] 개별 테스트 실패 시 FAIL 메시지 출력 후 exit 1

### T9-3. v1.2.3 회귀 항목 유지 확인

| v1.2.3 항목 | v1.2.4 유지 확인 |
|------------|-----------------|
| 훅 등록 멱등성 | - [ ] 회귀 없음 |
| 절대경로 훅 등록 | - [ ] 회귀 없음 |

---

## 예상 이슈 및 주의사항

| 항목 | 상황 | 대처 |
|------|------|------|
| T1-2 readline 프롬프트 | CLI 비대화형 환경(파이프/리다이렉션)에서는 프롬프트가 나오지 않을 수 있음 | 터미널 직접 실행으로 테스트 |
| T1-3 CLAUDE.md 섹션 삽입 | `## 참고 문서` 섹션 중복 생성 여부 확인 필요 | T1-3에서 별도 검증 |
| T7-2 rules/ 예시 링크 | lint가 `example-pattern` 깨진 링크로 탐지할 수 있음 | 정상 — 문서 예시 파일, 수동 처리 불필요 |
| tree-sitter 미설치 | T1-4 skip | 경고 후 계속 — 정상 |

---

## 테스트 결과 파일명

`2026-05-04-wiki-optimizer-test-results-v1.2.4.md`
