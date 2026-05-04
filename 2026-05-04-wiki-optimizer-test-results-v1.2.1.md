# wiki-optimizer v1.2.1 테스트 결과

**테스트 일시**: 2026-05-04  
**테스트 환경**: `/home/jeongih/project/sandbox`  
**플러그인 경로**: `~/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.1/`  
**테스트 플랜 원본**: `2026-05-04-wiki-optimizer-test-plan-v1.2.0.md`

---

## 요약

| 항목 | 수치 |
|------|------|
| 전체 테스트 케이스 | 21개 |
| 통과 | 18개 |
| 조건부 통과 (주의사항 있음) | 1개 |
| skip (정상) | 1개 |
| CLI 미구현 (스킬 레벨만 동작) | 1개 |
| 신규 버그 발견 | 1건 |
| v1.2.0 버그/개선사항 해소 확인 | 4건 |
| 회귀 테스트 | **100 passed, 0 failed** |

---

## T1. /wiki-init 테스트

### T1-1. 최초 초기화

- [x] `.wiki/concept/` 생성됨
- [x] `.wiki/pattern/` 생성됨
- [x] `.wiki/domain/` 생성됨
- [x] `.wiki/moc/`, `.wiki/summary/` **생성 안 됨** (제거 확인)
- [x] `.wiki/index.md` 생성됨 (템플릿 형태)
- [x] `.wiki/log.md` 생성됨
- [x] `.wiki/rules/format.md` 복사됨
- [x] `.wiki/rules/operations.md` 복사됨
- [x] `.wiki/rules/templates.md` 복사됨
- [x] `.wiki/config.json` 생성됨
- [x] `.wikiignore` 생성됨 (`node_modules/`, `dist/` 등 포함)
- [x] `.claude/settings.local.json`에 4종 훅 등록됨 — **단, 중복 등록 발생 (신규 버그③)**

**⚠ 신규 버그③**: `register-hooks.js`를 직접 실행할 때, 이미 등록된 v1.2.0 절대경로 훅을 다른 항목으로 인식하여 `${CLAUDE_PLUGIN_ROOT}` 형식 훅을 중복 추가함.

```json
// 문제 상태: SessionStart에 2개 항목이 공존
"SessionStart": [
  { "command": "node \".../1.2.0/hooks/session-start-wiki-index.js\"" },  // 기존
  { "command": "node \"${CLAUDE_PLUGIN_ROOT}/hooks/session-start-wiki-index.js\"" }  // 중복 추가
]
```

추가로 `${CLAUDE_PLUGIN_ROOT}` 환경변수가 Claude Code 런타임에서 실제로 해석되는지 미확인.

**수정 방향**: `register-hooks.js`가 훅 커맨드의 파일명(basename)을 기준으로 중복 판단하도록 개선 필요. 현재는 문자열 완전 일치만 체크하여 경로 형식이 다르면 별개 항목으로 등록함.

### T1-2. tree-sitter 초기 노트 생성

- [ ] ~~`.wiki/domain/eq-equipment-repair-service.md` 생성됨~~ → **skip**
- [ ] ~~`.wiki/domain/inv-onhands-controller.md` 생성됨~~ → **skip**

> `web-tree-sitter` npm 미설치로 정상 skip. CLI가 경고 메시지 출력 후 계속 진행 — 예상 이슈 항목과 일치.

### T1-3. 재실행 멱등성

- [x] 기존 파일 덮어쓰지 않음 (`already exists` 메시지 출력)
- [x] 훅 중복 등록 안 됨 (`Already registered` 메시지 출력)

> 2회차 CLI 실행 시 `${CLAUDE_PLUGIN_ROOT}` 형식 훅을 기준으로 중복 판단하여 추가 등록 없음. 1회차에서 발생한 버그③ 이후의 재실행은 멱등.

### T1-4. CLAUDE.md 없는 경우

- [ ] "CLAUDE.md가 없습니다" 경고 메시지 출력됨 — **CLI 바이너리 미구현**

> `bin/wiki-optimizer.js`에 CLAUDE.md 관련 로직 없음. 스킬(LLM) 레벨에서만 처리 가능.  
> `/wiki-init` 스킬 실행 시에는 LLM이 직접 판단하여 경고 메시지 출력.

### T1-5. CLAUDE.md 있는 경우

- [x] 위키 링크 추가 여부 확인 질문 출력됨 (스킬 레벨)

---

## T2. 훅 동작 테스트

### T2-1. SessionStart 훅

- [x] system context에 `<wiki-index>` 블록 주입됨
- [x] Domain/Pattern/Concept 섹션 내용이 포함됨
- [x] index.md가 비어있으면 조용히 종료 (오류 없음)

### T2-2. PreToolUse 훅 — 광범위 탐색 차단

- [x] `hookDecision: "block"` 반환됨 (`src/eq/**/*.java` Glob, 등록 도메인 세그먼트 포함)
- [x] `/wiki-query eq domain related files` 힌트 출력됨
- [x] `src/**/*.java` (등록 도메인 세그먼트 없음) → `hookDecision: "continue"` 반환됨

> **v1.2.0 버그② 수정됨**: 이전 버전에서는 `src/eq/**/*.java`에서 `block`이 반환되지 않았으나 v1.2.1에서 정상 동작 확인.

### T2-3. PreToolUse 훅 — 미등록 도메인 통과

- [x] `hookDecision: "continue"` 반환됨
- [x] 위키 쿼리 제안 메시지 출력됨

### T2-4. PostToolUse 훅 — 미등록 도메인 제안

- [x] `systemContext`에 `/wiki-ingest {domain}` 제안 출력됨

> 실제 출력: `/wiki-ingest onhands` (파일 경로에서 도메인 세그먼트 추출 확인)

### T2-5. Stop 훅 — 세션 종료 제안

- [x] 미등록 도메인에 대해 `/wiki-ingest` 제안 출력됨

> 출력 예: `/wiki-ingest common/base`, `/wiki-ingest onhands/inv-onhands`

---

## T3. /wiki-ingest 테스트

### T3-1. 도메인 ingest (`eq-equipment-repair`)

- [x] wiki-reviewer(sonnet) 디스패치됨
- [x] 분류 결정: `domain/` (소스 파일 기반)
- [x] wiki-writer가 `.wiki/domain/eq-equipment-repair.md` 작성
- [x] YAML frontmatter 5개 필드 포함 (`id`, `last_updated`, `source_files`, `dependencies`, `tags`)
- [x] `## 개요`, `## 화면 구조`, `## 주요 로직`, `## API 목록`, `## 연결` 섹션 포함
- [x] `## 연결`에 최소 2개 wikilink 포함 (각 링크에 `— 이유` 설명)
- [x] wiki-indexer가 `index.md` Domain 섹션 갱신
- [x] wiki-indexer가 `log.md`에 항목 prepend

> **v1.2.0 버그① 수정됨**: `dependencies` 필드 누락 없음.  
> **참고**: JPA/REST 구조로 SP 없음 → `## SP 목록` 대신 `## API 목록`으로 적합하게 대체됨.

### T3-2. concept ingest (`BaseDTO`)

- [x] 분류 결정: `concept/` ("무엇인가" 기준)
- [x] `.wiki/concept/base-dto.md` 작성
- [x] `## 정의`, `## 상세`, `## 연결` 섹션 포함

### T3-3. pattern ingest (`pattern:BaseDTO상속패턴`)

- [x] 분류 결정: `pattern/` ("어떻게 구현하는가" 기준)
- [x] `## 문제`, `## 해법`, `## 연결` 섹션 포함
- [x] **Java 코드 예시 정상 생성**

> **v1.2.0 개선사항③ 해소**: 이전 버전에서 Java 프로젝트에서 TypeScript 코드가 생성되던 문제가 v1.2.1에서 수정됨. 소스 언어(Java) 컨텍스트 전달 규칙이 ingest 스킬에 추가된 것으로 확인.

### T3-4. 기존 노트 업데이트

- [x] 기존 노트 감지 (`existing_note: true`)
- [x] 소스 코드와 노트 비교 후 불일치 3건 발견 및 수정:
  1. `equipmentId` 파라미터 타입: `Long` → `String`, 필수 → 선택(`required=false`)
  2. `equipmentId` null 시 전체 조회(`findAll`) 분기 누락 → 추가
  3. 수리 등록 요청 바디: `EqEquipmentRepairDTO` → `RepairRequest`
- [x] `last_updated` 반영됨
- [x] `#auto-generated` 태그 없음 확인

---

## T4. /wiki-query 테스트

### T4-1. 기본 쿼리 (`BaseDTO errorCode 처리 방법`)

- [x] wiki-reviewer가 index.md에서 관련 페이지 식별
- [x] 관련 페이지 읽기 후 답변 생성
- [x] 출처 wikilink 명시 (`[[.wiki/concept/base-dto]]`, `[[.wiki/pattern/base-dto-inheritance]]`)

### T4-2. 위키 미비 시 소스 탐색 제안

- [x] "위키에 없는 내용" 안내 출력
- [x] `/wiki-ingest inv-inventory` 제안 출력

---

## T5. /wiki-lint 테스트

### T5-1. 기본 lint

- [x] 고아 노트 탐지 (없음 확인)
- [x] 깨진 링크 탐지 (없음 확인 — 초기 상태 이슈 없음)
- [x] 고립 노트 탐지 (없음 확인)
- [x] 리포트 출력 (정상 3 / 이슈 0)

### T5-2. --fix 모드

> 깨진 링크 테스트를 위해 `eq-equipment-repair.md`에 인위적으로 2건 추가 후 --fix 실행.

- [x] 깨진 링크 2건 수정:
  - `[[.wiki/pattern/cud-service]]` → `[[.wiki/pattern/base-dto-inheritance]]` (유사 노트로 교체)
  - `[[.wiki/concept/repair-status]]` → 제거 (대응 노트 없음)
- [x] log.md에 lint 항목 기록됨

### T5-3. --deep 모드

- [x] wiki-reviewer가 `git log` 실행 (Bash 도구 사용)
- [x] 코드 변경됐지만 위키 갱신 안 된 노트 점검 → 오래된 노트 없음 (위키가 코드보다 최신)
- [x] 미등록 도메인(`InvInventoryService`) 1건 탐지 및 `/wiki-ingest inv-inventory` 제안

---

## T6. rules 파일 참조 테스트

- [x] `/wiki-ingest` 실행 시 wiki-writer가 `rules/format.md` wikilink 형식 준수
- [x] 생성된 노트의 `id` 형식이 `rules/format.md` 기준 일치 (`{분류}_{kebab-case}`)
  - `domain_eq-equipment-repair`, `concept_base-dto`, `pattern_base-dto-inheritance`
- [x] 생성된 노트에 `## 연결` 섹션 + 최소 2개 wikilink 포함 (모든 링크에 `— 이유` 설명 포함)

---

## T7. 회귀 테스트

- [x] 전체 테스트 통과 (**100 passed, 0 failed**)
- [x] wiki-reviewer model: sonnet 허용 확인 (`test-agents.js` 28 passed)
- [x] register-hooks 멱등성 통과 (`test-register-hooks.js` 10 passed)
- [x] pre/post read hook 로직 통과 (`test-pre-read-hook.js` 6 passed, `test-post-read-hook.js` 7 passed)
- [x] `npm test` 스크립트 정의됨 — **v1.2.0 개선사항② 해소**

> 테스트 파일 9개, 개별 통과 현황:

| 테스트 파일 | 통과 |
|------------|------|
| test-agents.js | 28 |
| test-ingest-watch.js | 10 |
| test-pattern-detector.js | 18 |
| test-post-read-hook.js | 7 |
| test-pre-read-hook.js | 6 |
| test-register-hooks.js | 10 |
| test-session-start-hook.js | 6 |
| test-stop-hook.js | 6 |
| test-wiki-reader.js | 9 |
| **합계** | **100** |

---

## v1.2.0 대비 해소 현황

| 구분 | 내용 | v1.2.1 상태 |
|------|------|------------|
| 버그① | `register-hooks.js` CLI 경로 오계산 | ✅ 해소 |
| 버그② | T2-2 테스트 플랜 불일치 (`src/eq/**` block) | ✅ 해소 |
| 개선① | wiki-writer `dependencies` frontmatter 누락 | ✅ 해소 |
| 개선② | `npm test` 스크립트 미정의 | ✅ 해소 |
| 개선③ | Java 프로젝트에서 TypeScript 코드 생성 | ✅ 해소 |

---

## 신규 발견 버그 및 개선 사항 (v1.2.1)

### 버그③: 훅 중복 등록 (버전 간 호환 미처리)

- **파일**: `scripts/register-hooks.js`
- **증상**: 이미 다른 버전의 훅(절대경로 형식)이 등록된 상태에서 재실행 시 `${CLAUDE_PLUGIN_ROOT}` 형식의 훅을 중복 추가
- **원인**: 중복 판단이 커맨드 문자열 완전 일치 기반 → 경로 형식(절대경로 vs 환경변수)이 다르면 다른 훅으로 인식
- **수정 방향**: 훅 커맨드에서 파일명(basename)만 추출하여 중복 판단

```js
// 현재 (버그): 문자열 완전 일치
existing.command === newCommand

// 수정안: basename 기준 중복 판단
path.basename(existing.command) === path.basename(newCommand)
```

### 개선 사항①: `${CLAUDE_PLUGIN_ROOT}` 환경변수 해석 여부 미확인

- **증상**: v1.2.1 신규 등록 훅이 `${CLAUDE_PLUGIN_ROOT}/hooks/...` 형식을 사용하나 Claude Code 런타임에서 이 환경변수가 실제로 해석되는지 검증되지 않음
- **수정 방향**: 등록 시 `CLAUDE_PLUGIN_ROOT` 값을 즉시 절대경로로 치환하거나, 런타임 해석 여부를 공식 문서에서 확인 후 처리

### 개선 사항②: T1-4/T1-5 CLAUDE.md 처리 CLI 미구현

- **증상**: `bin/wiki-optimizer.js`에 CLAUDE.md 존재 여부 확인 및 경고/질문 로직 없음
- **수정 방향**: CLI init 완료 후 CLAUDE.md 유무를 체크하고 터미널에 안내 메시지 출력

---

## 생성된 위키 파일 목록

```
.wiki/
├── index.md               (갱신: Concept 1건, Pattern 1건, Domain 1건)
├── log.md                 (갱신: 6건 기록 — init, ingest 3건, lint --fix, update)
├── config.json
├── rules/
│   ├── format.md
│   ├── operations.md
│   └── templates.md
├── concept/
│   └── base-dto.md             (신규 생성)
├── pattern/
│   └── base-dto-inheritance.md (신규 생성)
└── domain/
    └── eq-equipment-repair.md  (신규 생성 → T3-4에서 코드 불일치 3건 수정)
```
