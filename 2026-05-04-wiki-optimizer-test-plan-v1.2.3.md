# wiki-optimizer v1.2.3 전체 기능 테스트 플랜

**작성일**: 2026-05-04  
**대상**: 샌드박스 프로젝트에서 플러그인 업데이트 후 전체 기능 검증  
**플러그인 버전**: 1.2.3  
**이전 결과 참조**: `2026-05-04-wiki-optimizer-test-results-v1.2.2.md`

---

## v1.2.3 신규 수정 항목 (주요 검증 포인트)

| ID | 내용 | 검증 섹션 |
|----|------|----------|
| BUG-3 | `cmdStatus()` rules 파일 체크 하드코딩 오류 수정 (`docs.md` → `format/operations/templates` 3파일) | T6-3 |
| BUG-4 | Stop 훅 config extractor 슬래시 경로와 index 평탄 경로 매칭 불일치 수정 (`isRegisteredInIndex()` 개선) | T2-6, T8 |
| BUG-5 | `--version` / `-v` 플래그 미구현 수정 | T6-1 |
| BUG-6 | `cmdHelp()` ingest 명령어 누락 수정 | T6-2 |
| BUG-7 | `cmdStatus()` 미초기화 프로젝트 안내 누락 수정 | T6-4 |

---

## 검증 범위

| 섹션 | 내용 |
|------|------|
| T0 | 샌드박스 환경 준비 & 플러그인 업데이트 |
| T1 | `/wiki-init` — 초기화, CLAUDE.md 처리, 멱등성 |
| T2 | 훅 동작 — SessionStart / PreToolUse / PostToolUse / Stop |
| T3 | `/wiki-ingest` — domain / concept / pattern / 업데이트 |
| T4 | `/wiki-query` — 기본 쿼리 / 위키 미비 처리 |
| T5 | `/wiki-lint` — 기본 / --fix / --deep |
| T6 | CLI (`wiki-optimizer`) — version / help / status / init / ingest |
| T7 | rules 파일 형식 준수 |
| T8 | BUG-4 수정 검증 — Stop 훅 슬래시 경로 매칭 |
| T9 | 회귀 테스트 (`npm test`) |

---

## T0. 샌드박스 환경 준비 & 플러그인 업데이트

### T0-1. 샌드박스 프로젝트 생성

```bash
mkdir ~/sandbox-wiki-v123 && cd ~/sandbox-wiki-v123
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

### T0-2. 플러그인 업데이트

```bash
# 플러그인 캐시 업데이트 (마켓플레이스 방식)
/plugin update wiki-optimizer

# 또는 수동 업데이트 (개발 환경)
node ~/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.3/bin/wiki-optimizer.js --version
```

**체크리스트**:
- [ ] 플러그인 버전이 `1.2.3`으로 표시됨
- [ ] `package.json`, `plugin.json`, `bin/wiki-optimizer.js` 세 파일의 버전이 모두 `1.2.3`으로 일치

### T0-3. BUG-4 검증용 환경 준비 (config extractor 슬래시 경로)

> **목적**: Stop 훅 슬래시 경로 매칭 버그 재현 및 수정 확인

T1 완료 후 `.wiki/config.json`에 슬래시를 포함한 domainExtractor 설정 추가 (T8에서 사용):

```json
{
  "domainExtractors": [
    {
      "regex": "src/([a-z]+)/([A-Za-z]+)\\.java",
      "domainTemplate": "$1/$2",
      "caseTransform": "kebab"
    }
  ]
}
```

---

## T1. `/wiki-init` 테스트

### T1-1. 최초 초기화 (CLAUDE.md 없는 경우)

**전제**: 샌드박스 루트에 CLAUDE.md 없음, `.wiki/` 없음

**실행**: `/wiki-init`

**체크리스트**:

**디렉토리 생성**:
- [ ] `.wiki/concept/` 생성됨
- [ ] `.wiki/pattern/` 생성됨
- [ ] `.wiki/domain/` 생성됨

**파일 생성**:
- [ ] `.wiki/index.md` 생성됨 (Concept / Pattern / Domain 섹션 포함 템플릿)
- [ ] `.wiki/log.md` 생성됨
- [ ] `.wiki/config.json` 생성됨 (`domainExtractors` 필드 포함)
- [ ] `.wikiignore` 생성됨 (`node_modules/`, `dist/`, `.git/` 등 포함)

**rules 복사**:
- [ ] `.wiki/rules/format.md` 복사됨
- [ ] `.wiki/rules/operations.md` 복사됨
- [ ] `.wiki/rules/templates.md` 복사됨

**훅 등록**:
- [ ] `.claude/settings.local.json`에 4종 훅 등록됨
  - `SessionStart` → `session-start-wiki-index.js`
  - `PreToolUse` → `pre-read-wiki-intercept.js` (matcher: `Glob|Read`)
  - `PostToolUse` → `post-read-wiki-suggest.js` (matcher: `Read`)
  - `Stop` → `stop-suggest-ingest.js`

**CLAUDE.md 처리**:
- [ ] CLI 터미널에 "CLAUDE.md가 없습니다" 경고 메시지 출력됨
- [ ] 경고 후 초기화는 정상 완료 (프로세스 종료 안 됨)

### T1-2. 최초 초기화 (CLAUDE.md 있는 경우)

**전제**: `echo "# My Project" > CLAUDE.md` 생성 후 `/wiki-init` 재실행

**체크리스트**:
- [ ] "위키 링크를 CLAUDE.md에 추가하시겠습니까?" 또는 유사 질문 출력됨

**응답 `y` 시**:
- [ ] CLAUDE.md에 `[Wiki Index](.wiki/index.md)` 링크 추가됨
- [ ] 기존 CLAUDE.md 내용 보존됨 (`# My Project` 유지)

**응답 `n` 시**:
- [ ] CLAUDE.md 수정 없음
- [ ] 초기화는 정상 완료됨

### T1-3. tree-sitter 초기 노트 생성

**전제**: `npm install` 완료 (web-tree-sitter 의존성)

**체크리스트**:
- [ ] `.wiki/domain/` 아래 Java 소스 파일 기반 노트 생성됨
- [ ] 각 노트에 YAML frontmatter 포함 (`#auto-generated` 태그)
- [ ] 각 노트에 `## 감지된 클래스·함수` 섹션 포함
- [ ] `.wiki/index.md` Domain 섹션에 생성된 노트들 등록됨

> tree-sitter 미설치 시: CLI 경고 메시지 출력 후 계속 진행 — 정상 동작

### T1-4. 재실행 멱등성

**실행**: `/wiki-init` 재실행 (`.wiki/` 이미 존재하는 상태)

**체크리스트**:
- [ ] 기존 파일 덮어쓰지 않음 (`already exists` 또는 `skip` 메시지 출력)
- [ ] 훅 중복 등록 안 됨 (`Already registered` 메시지 4회 출력)
- [ ] `.claude/settings.local.json` 훅 항목 수 변동 없음

---

## T2. 훅 동작 테스트

> **전제**: T1 완료, `.wiki/index.md`에 `eq` 도메인 등록됨

### T2-1. SessionStart 훅

**방법**: 새 Claude Code 세션 시작

**체크리스트**:
- [ ] system context에 `<wiki-index>` 블록 주입됨
- [ ] `<wiki-index>` 내에 Domain 섹션 내용 포함됨
- [ ] Concept, Pattern 섹션도 내용 있으면 포함됨
- [ ] index.md가 비어있으면 조용히 종료 (오류 없음)
- [ ] 훅 실행 시간 < 500ms

### T2-2. PreToolUse 훅 — 등록 도메인 재귀 Glob 차단

**전제**: index.md에 `eq` 도메인 등록됨

**방법**: `src/eq/**/*.java` Glob 시도

**체크리스트**:
- [ ] `hookDecision: "block"` 반환됨
- [ ] `/wiki-query` 힌트 메시지 출력됨

**정상 통과 케이스**:
- `src/**/*.java` (등록 도메인 세그먼트 없음) → `hookDecision: "continue"`
- `src/eq/EqEquipmentRepairService.java` (단일 파일) → `hookDecision: "continue"`

### T2-3. PreToolUse 훅 — 미등록 도메인 재귀 Glob

**방법**: `src/newdomain/**/*.java` Glob 시도

**체크리스트**:
- [ ] `hookDecision: "continue"` 반환됨 (차단 안 함)
- [ ] system context에 `/wiki-ingest newdomain` 제안 출력됨

### T2-4. PostToolUse 훅 — 미등록 도메인 Read 후 제안

**방법**: `src/inv/InvOnhandsController.java` Read (index.md 미등록 상태)

**체크리스트**:
- [ ] `systemContext`에 `/wiki-ingest inv` 제안 출력됨

### T2-5. PostToolUse 훅 — 등록 도메인 Read 후 무반응

**방법**: `src/eq/EqEquipmentRepairService.java` Read (등록된 도메인)

**체크리스트**:
- [ ] `/wiki-ingest` 제안 출력 안 됨
- [ ] 훅이 조용히 종료됨

### T2-6. Stop 훅 — 미등록 도메인 변경 시 제안

**전제**: 미등록 도메인 소스 파일 변경 (git staged 또는 modified 상태)

```bash
echo "public class InvInventoryService {}" > src/inv/InvInventoryService.java
```

**방법**: Claude Code 세션 종료

**체크리스트**:
- [ ] 변경 파일에서 도메인 추출됨
- [ ] 미등록 도메인에 대해 `/wiki-ingest` 제안 출력됨
- [ ] 등록된 도메인 (`eq`)은 제안 목록에서 제외됨

---

## T3. `/wiki-ingest` 테스트

### T3-1. 도메인 ingest — 신규 생성

**실행**: `/wiki-ingest eq-equipment-repair`

**체크리스트**:

**에이전트 오케스트레이션**:
- [ ] wiki-reviewer 디스패치됨 (분석)
- [ ] wiki-writer 디스패치됨 (작성)
- [ ] wiki-indexer 디스패치됨 (갱신)

**생성된 노트 형식**:
- [ ] `.wiki/domain/eq-equipment-repair.md` 파일 생성됨
- [ ] YAML frontmatter 5개 필드: `id`, `last_updated`, `source_files`, `dependencies`, `tags`
- [ ] `id` 형식: `domain_eq-equipment-repair`
- [ ] `## 개요`, `## 주요 로직`, `## 연결` 섹션 포함
- [ ] `## 연결` 섹션에 최소 2개 wikilink, 각 링크에 `— 이유` 설명 포함
- [ ] wikilink 형식: `[[.wiki/category/page-name]]` (`.md` 확장자 없음)
- [ ] 코드 예시가 있다면 Java 언어로 작성됨

**인덱스 갱신**:
- [ ] `.wiki/index.md` Domain 섹션에 행 추가됨
- [ ] 행 형식: `| [[.wiki/domain/eq-equipment-repair]] | 한줄 요약 | YYYY-MM-DD |`
- [ ] `.wiki/log.md` 최상단에 ingest 항목 prepend됨
- [ ] log.md 형식: `## [YYYY-MM-DD] ingest | ...`

### T3-2. concept ingest — 신규 생성

**실행**: `/wiki-ingest BaseDTO`

**체크리스트**:
- [ ] 분류 결정: `concept/` ("무엇인가" 기준)
- [ ] `.wiki/concept/base-dto.md` 생성됨
- [ ] YAML frontmatter 포함 (`id`: `concept_base-dto`)
- [ ] `## 정의`, `## 상세`, `## 연결` 섹션 포함
- [ ] `.wiki/index.md` Concept 섹션에 등록됨
- [ ] `.wiki/log.md` 갱신됨

### T3-3. pattern ingest — 신규 생성

**실행**: `/wiki-ingest pattern:BaseDTO상속패턴`

**체크리스트**:
- [ ] 분류 결정: `pattern/`
- [ ] `.wiki/pattern/base-dto-inheritance.md` 또는 유사 파일 생성됨
- [ ] `id` 형식: `pattern_base-dto-inheritance`
- [ ] `## 문제`, `## 해법`, `## 연결` 섹션 포함
- [ ] 코드 예시가 Java로 작성됨
- [ ] `.wiki/index.md` Pattern 섹션에 등록됨

### T3-4. 기존 auto-generated 노트 업데이트

**조건**: T1-3에서 auto-generated 노트 존재  
**실행**: `/wiki-ingest eq-equipment-repair` (이미 존재하는 도메인)

**체크리스트**:
- [ ] 기존 노트 감지 후 신규 생성 아닌 업데이트 수행
- [ ] `last_updated` 날짜 갱신됨
- [ ] `#auto-generated` 태그 제거됨
- [ ] TODO 섹션이 실제 내용으로 대체됨
- [ ] log.md에 갱신 항목 기록됨

### T3-5. 존재하지 않는 도메인 ingest 시도

**실행**: `/wiki-ingest nonexistent-domain`

**체크리스트**:
- [ ] "소스 파일을 찾을 수 없습니다" 또는 유사 안내 출력됨
- [ ] 빈 노트 생성 안 됨
- [ ] `.wiki/index.md` 변경 없음

---

## T4. `/wiki-query` 테스트

### T4-1. 기본 쿼리 — 위키 내용 기반 답변

**전제**: T3 완료 (concept, pattern, domain 노트 존재)

**실행**: `/wiki-query BaseDTO errorCode 처리 방법`

**체크리스트**:
- [ ] wiki-reviewer 디스패치됨 (index.md 탐색)
- [ ] 관련 페이지 식별: `concept/base-dto` 선택
- [ ] 답변에 `errorCode`, `errorMsg` 필드 설명 포함
- [ ] 출처 wikilink 명시: `[[.wiki/concept/base-dto]]`

### T4-2. 크로스 도메인 쿼리

**실행**: `/wiki-query EqEquipmentRepairService에서 BaseDTO 상속은 어떻게 되나요`

**체크리스트**:
- [ ] 복수 관련 페이지 식별 및 읽기 (최대 5개)
- [ ] 복수 출처 wikilink 명시

### T4-3. 위키 미비 시 ingest 제안

**실행**: `/wiki-query 위키에 없는 InvInventoryService 동작`

**체크리스트**:
- [ ] "위키에 없는 내용" 또는 유사 안내 출력됨
- [ ] `/wiki-ingest` 제안 출력됨

---

## T5. `/wiki-lint` 테스트

### T5-1. 기본 lint — 정상 상태

**전제**: T3 완료, 위키 일관성 있는 상태

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] 고아 노트 / 깨진 링크 / 고립 노트 탐지 실행
- [ ] 리포트 출력 ("정상 N / 이슈 0" 형태)

### T5-2. 고아 노트 탐지

**준비**:
```bash
echo "# Orphan" > .wiki/domain/orphan-note.md
```

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] `orphan-note` 고아 노트 1건 탐지됨
- [ ] 리포트에 파일명 명시됨

### T5-3. 깨진 링크 탐지

**준비**:
```bash
echo "- [[.wiki/pattern/nonexistent-pattern]] — 테스트용" >> .wiki/domain/eq-equipment-repair.md
echo "- [[.wiki/concept/nonexistent-concept]] — 테스트용" >> .wiki/domain/eq-equipment-repair.md
```

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] 깨진 링크 2건 탐지됨
- [ ] 리포트에 파일명 및 링크 경로 명시됨

### T5-4. `--fix` 모드

**전제**: T5-2, T5-3 이슈 상태 유지

**실행**: `/wiki-lint --fix`

**체크리스트**:
- [ ] 자동 수정 가능 항목 처리됨
- [ ] `log.md` 최상단에 lint 항목 prepend됨
- [ ] 리포트에 수정 내역 명시됨

### T5-5. `--deep` 모드

**준비**:
```bash
echo "  public List<RepairDTO> findAll() { return null; }" >> src/eq/EqEquipmentRepairService.java
git add . && git commit -m "feat: add findAll method"
```

**실행**: `/wiki-lint --deep`

**체크리스트**:
- [ ] 소스 변경 후 위키 미갱신 노트 감지
- [ ] "오래된 페이지" 목록에 `eq-equipment-repair` 포함
- [ ] `/wiki-ingest eq-equipment-repair` 제안 출력됨

---

## T6. CLI (`wiki-optimizer`) 테스트

### T6-1. `--version` / `-v` 플래그 (BUG-5 수정)

**실행**:
```bash
wiki-optimizer --version
wiki-optimizer -v
```

**체크리스트**:
- [ ] `--version` 실행 시 `1.2.3` 출력됨
- [ ] `-v` 실행 시 `1.2.3` 출력됨
- [ ] `package.json`, `plugin.json`, `bin/wiki-optimizer.js` 버전과 일치

### T6-2. `help` 명령어 (BUG-6 수정)

**실행**: `wiki-optimizer help`

**체크리스트**:
- [ ] `init` 명령어 설명 포함
- [ ] `status` 명령어 설명 포함
- [ ] `ingest` 명령어 설명 포함 **(BUG-6 수정: 이전 버전에서 누락됨)**
- [ ] `help` 명령어 설명 포함

### T6-3. `status` — 초기화 완료 상태 (BUG-3 수정)

**전제**: T1 완료 (`.wiki/` 구조 존재, rules 3파일 복사됨)

**실행**: `wiki-optimizer status`

**체크리스트**:
- [ ] `.wiki/` 구조 존재 여부 표시됨
- [ ] 훅 등록 상태 표시됨 (4종 훅 각각)
- [ ] 노트 수 통계 출력됨 (domain N, concept N, pattern N)
- [ ] rules 파일 체크: `format.md`, `operations.md`, `templates.md` 3개 **(BUG-3 수정: 이전에는 `docs.md` 하드코딩)**
- [ ] 이슈 없으면 정상 상태 표시

### T6-4. `status` — 미초기화 상태 (BUG-7 수정)

**전제**: `.wiki/` 없는 새 디렉토리

**실행**: `wiki-optimizer status`

**체크리스트**:
- [ ] "wiki-optimizer가 초기화되지 않았습니다" 또는 유사 안내 출력됨 **(BUG-7 수정: 이전에는 안내 없음)**
- [ ] `wiki-optimizer init` 실행 안내 출력됨
- [ ] 오류 없이 정상 종료됨 (exit code 0)

### T6-5. `init` CLI 직접 실행

**전제**: 미초기화 샌드박스

**실행**: `wiki-optimizer init`

**체크리스트**:
- [ ] `.wiki/` 구조 생성됨 (T1-1과 동일 체크리스트)
- [ ] 훅 등록됨
- [ ] CLAUDE.md 처리 로직 실행됨

### T6-6. `ingest <path>`

**실행**: `wiki-optimizer ingest src/eq/`

**체크리스트**:
- [ ] `src/eq/` 하위 소스 파일 스캔됨
- [ ] tree-sitter 설치 시 초기 도메인 노트 생성 시도됨

---

## T7. rules 파일 형식 준수 테스트

**전제**: T3 완료 (domain, concept, pattern 노트 존재)

**체크리스트**:

**format.md 준수**:
- [ ] 노트 `id` 형식 일치: `domain_eq-equipment-repair`, `concept_base-dto`, `pattern_base-dto-inheritance`
- [ ] wikilink 형식: `[[.wiki/category/page-name]]` (`.md` 확장자 없음, 절대경로 아님)
- [ ] YAML frontmatter 5개 필드 모두 존재

**templates.md 준수**:
- [ ] domain 노트: `## 개요`, `## 주요 로직`, `## 연결` 포함
- [ ] concept 노트: `## 정의`, `## 상세`, `## 연결` 포함
- [ ] pattern 노트: `## 문제`, `## 해법`, `## 연결` 포함

**operations.md 준수**:
- [ ] `## 연결` 섹션 최소 2개 wikilink 포함
- [ ] 각 wikilink에 `— 이유` 설명 포함
- [ ] log.md 항목이 항상 최상단 prepend됨 (하단 추가 안 됨)
- [ ] index.md 행 형식: `| [[.wiki/분류/파일명]] | 한줄 요약 | YYYY-MM-DD |`

---

## T8. BUG-4 수정 검증 — Stop 훅 슬래시 경로 매칭

> **목적**: config extractor가 `$1/$2` 형태의 슬래시 포함 경로를 반환할 때
> index.md 등록 여부 판단이 올바르게 동작하는지 확인

### T8-1. 슬래시 경로 도메인 등록 확인

**전제**: T0-3에서 `domainExtractors` 설정 추가됨, `eq/EqEquipmentRepairService` 도메인 등록됨

```bash
# 등록된 도메인 파일 변경
echo "  // v2" >> src/eq/EqEquipmentRepairService.java
git add .
```

**방법**: Claude Code 세션 종료

**체크리스트**:
- [ ] Stop 훅이 `eq/eq-equipment-repair-service` 또는 유사 슬래시 경로 추출
- [ ] index.md에 `eq` 세그먼트가 등록되어 있으므로 → `/wiki-ingest` 제안 **안 나옴**
- [ ] `isRegisteredInIndex()` 세그먼트별 비교 로직 정상 동작

### T8-2. 미등록 슬래시 경로 도메인

```bash
mkdir -p src/pay
echo "public class PayRefundService {}" > src/pay/PayRefundService.java
git add .
```

**방법**: Claude Code 세션 종료

**체크리스트**:
- [ ] `pay/pay-refund-service` 또는 유사 슬래시 경로 추출
- [ ] `pay` 세그먼트가 index.md에 없으므로 → `/wiki-ingest pay/pay-refund-service` 제안 출력됨
- [ ] 세그먼트 길이 3자 초과 조건 정상 적용 (`pay` → 3자, 경계값 테스트)

### T8-3. 세그먼트 길이 경계값

**체크리스트**:
- [ ] 3자 이하 세그먼트 (`eq`, `inv`, `pay`)는 오탐 방지를 위해 단독 비교 건너뜀
- [ ] 4자 이상 세그먼트는 비교 대상 포함

---

## T9. 회귀 테스트

### T9-1. 자동화 테스트 전체 실행

**실행**: `npm test` (플러그인 루트에서)

**체크리스트**:
- [ ] 전체 테스트 통과 (0 failed)
- [ ] 기존 100 passed 이상 유지

**개별 테스트 통과 확인**:

| 테스트 파일 | 핵심 검증 | v1.2.2 기준 |
|------------|----------|------------|
| `test-agents.js` | 에이전트 model, tools 스펙 | 28 passed |
| `test-register-hooks.js` | 훅 등록 멱등성 | 10+ passed |
| `test-wiki-reader.js` | index.md 섹션 추출 | 9 passed |
| `test-pattern-detector.js` | 광범위 탐색 분류 | 18 passed |
| `test-pre-read-hook.js` | PreToolUse block/continue | 6 passed |
| `test-post-read-hook.js` | PostToolUse 도메인 감지 | 7 passed |
| `test-session-start-hook.js` | context 주입 | 6 passed |
| `test-stop-hook.js` | git diff 도메인 추출, **슬래시 경로 매칭(신규)** | 6+ passed |

### T9-2. v1.2.2 해소 항목 유지 확인

| v1.2.2 버그/개선 | v1.2.2 해소 | v1.2.3 유지 확인 |
|------------------|------------|-----------------|
| 버그③ register-hooks.js basename 중복 판단 | ✅ | - [ ] 회귀 없음 |
| 개선① 절대경로 훅 등록 | ✅ | - [ ] 회귀 없음 |
| 개선② CLAUDE.md 없는 경우 경고 출력 | ✅ | - [ ] 회귀 없음 |

---

## 예상 이슈 및 주의사항

| 항목 | 상황 | 대처 |
|------|------|------|
| tree-sitter 미설치 | T1-3 skip | CLI 경고 출력 후 계속 진행 — 정상 |
| wiki-reviewer 응답 속도 | sonnet 사용으로 haiku 대비 느림 | 정상 — 30초 이내 |
| T8-2 `pay` 세그먼트 경계값 | 3자 이하 건너뜀 정책 → `pay` 미감지 가능 | 정상 동작 — 오탐 방지 의도된 설계 |
| T5-5 `--deep` | git 이력 없으면 빈 결과 | 정상 — T0-1에서 최소 1 commit 필요 |
| BUG-4 검증 | config.json extractor 수동 설정 필요 (T0-3) | T0-3 선행 필수 |

---

## 테스트 결과 파일명

`2026-05-04-wiki-optimizer-test-results-v1.2.3.md`
