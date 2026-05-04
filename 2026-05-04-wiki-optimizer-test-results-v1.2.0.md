# wiki-optimizer v1.2.0 테스트 결과

**테스트 일시**: 2026-05-04  
**테스트 환경**: `/home/jeongih/project/sandbox`  
**플러그인 경로**: `~/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.0/`  
**테스트 플랜 원본**: `2026-05-04-wiki-optimizer-test-plan-v1.2.0.md`

---

## 요약

| 항목 | 수치 |
|------|------|
| 전체 테스트 케이스 | 21개 |
| 통과 | 18개 |
| 조건부 통과 (주의사항 있음) | 1개 |
| skip (정상) | 1개 |
| 불일치 발견 | 1개 |
| 발견된 버그 | 2건 |
| 회귀 테스트 (npm test 대체) | **100 passed, 0 failed** |

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
- [x] `.claude/settings.local.json`에 4종 훅 등록됨 (SessionStart/PreToolUse/PostToolUse/Stop)

**⚠ 버그①**: `register-hooks.js`가 `process.argv[2]`(= `"init"`)를 프로젝트 경로로 오해석하여 올바른 `.claude/settings.local.json` 대신 `init/.claude/settings.local.json`에 파일을 생성함. CLI를 직접 실행한 경우(`node wiki-optimizer.js init`) 항상 재현됨.

### T1-2. tree-sitter 초기 노트 생성

- [ ] ~~`.wiki/domain/eq-equipment-repair-service.md` 생성됨~~ → **skip**
- [ ] ~~`.wiki/domain/inv-onhands-controller.md` 생성됨~~ → **skip**

> `web-tree-sitter` npm 미설치로 정상 skip. CLI가 경고 메시지 출력 후 계속 진행 — 예상 이슈 항목과 일치.

### T1-3. 재실행 멱등성

- [x] 기존 파일 덮어쓰지 않음 (skip 메시지 출력)
- [x] 훅 중복 등록 안 됨 (`Already registered` 메시지 출력)

### T1-4. CLAUDE.md 없는 경우

- [x] "CLAUDE.md가 없습니다" 경고 메시지 출력됨

### T1-5. CLAUDE.md 있는 경우

- [x] 위키 링크 추가 여부 확인 질문 출력됨

---

## T2. 훅 동작 테스트

### T2-1. SessionStart 훅

- [x] system context에 `<wiki-index>` 블록 주입됨
- [x] Domain/Pattern/Concept 섹션 내용이 포함됨
- [x] index.md가 비어있으면 조용히 종료 (오류 없음)

### T2-2. PreToolUse 훅 — 광범위 탐색 차단

- [ ] `hookDecision: "block"` 반환됨 — **불일치**
- [x] `/wiki-query eq domain related files` 힌트 출력됨 (도메인 경로 포함 시)

**⚠ 버그②** (테스트 플랜 불일치):  
테스트 플랜은 `src/**/*.java`(도메인 세그먼트 없는 전체 탐색)에서 `block` 기대.  
실제 구현은 경로에 등록된 도메인 세그먼트가 포함된 경우(`src/eq/**/*.java`)에만 `block` 반환.  
`src/**/*.java` → `continue` 반환.

구현 코드 주석(`design decision: BROAD_GLOB_PATTERNS`)에서 이 동작이 의도된 설계임을 명시하고 있어 테스트 플랜 수정이 필요함.

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
- [x] YAML frontmatter 5개 필드 포함 (`id`, `last_updated`, `source_files`, `dependencies`, `tags`) — writer 초안에서 `dependencies` 누락, 수동 보완
- [x] `## 개요`, `## 화면 구조`, `## 주요 로직`, `## SP 목록`, `## 연결` 섹션 포함
- [x] `## 연결`에 최소 2개 wikilink 포함
- [x] wiki-indexer가 `index.md` Domain 섹션 갱신
- [x] wiki-indexer가 `log.md`에 항목 prepend

**주의**: wiki-writer가 `dependencies` frontmatter 필드를 빠뜨리는 경향 있음. rules/format.md 준수 강화 필요.

### T3-2. concept ingest (`BaseDTO`)

- [x] 분류 결정: `concept/` ("무엇인가" 기준)
- [x] `.wiki/concept/base-dto.md` 작성
- [x] `## 정의`, `## 상세`, `## 연결` 섹션 포함

### T3-3. pattern ingest (`pattern:BaseDTO상속패턴`)

- [x] 분류 결정: `pattern/` ("어떻게 구현하는가" 기준)
- [x] `## 문제`, `## 해법`, `## 연결` 섹션 포함

**주의**: Java 프로젝트임에도 코드 예시가 TypeScript로 작성됨. 소스 파일이 없는 순수 패턴 문서화 시 wiki-writer에게 언어 컨텍스트를 명시적으로 전달할 필요 있음.

### T3-4. 기존 노트 업데이트

- [x] 기존 노트 감지 (`existingNote: true`)
- [x] `last_updated` 기준 현행 판단 (코드 동일 → 업데이트 불필요)
- [x] `#auto-generated` 태그 없음 확인

> T1-2 skip으로 auto-generated 노트가 없어 태그 제거 경로는 미검증.

---

## T4. /wiki-query 테스트

### T4-1. 기본 쿼리 (`BaseDTO errorCode 처리 방법`)

- [x] wiki-reviewer가 index.md에서 관련 페이지 식별
- [x] 관련 페이지 읽기 후 답변 생성
- [x] 출처 wikilink 명시 (`[[.wiki/concept/base-dto]]`, `[[.wiki/pattern/base-dto-inheritance]]`)

### T4-2. 위키 미비 시 소스 탐색 제안

- [x] "위키에 없는 내용" 안내 출력
- [x] `/wiki-ingest` 제안 출력

---

## T5. /wiki-lint 테스트

### T5-1. 기본 lint

- [x] 고아 노트 탐지 (없음 확인)
- [x] 깨진 링크 탐지 — 2건 발견 (`[[.wiki/pattern/cud-service]]`, `[[.wiki/concept/repair-status]]`)
- [x] 고립 노트 탐지 (없음 확인)
- [x] 리포트 출력 (정상 2 / 이슈 2)

### T5-2. --fix 모드

- [x] 깨진 링크를 실존하는 wikilink로 교체
- [x] log.md에 lint 항목 기록됨

### T5-3. --deep 모드

- [x] wiki-reviewer가 `git log` 실행 (Bash 도구 사용)
- [x] 코드 변경됐지만 위키 갱신 안 된 노트 점검 — 오래된 페이지 없음 (위키가 코드보다 최신)
- [x] 미등록 도메인(`InvInventoryService`) 탐지 및 별도 분류

---

## T6. rules 파일 참조 테스트

- [x] `/wiki-ingest` 실행 시 wiki-writer가 `rules/format.md` wikilink 형식 준수
- [x] 생성된 노트의 `id` 형식이 `rules/format.md` 기준 일치 (`{분류}_{kebab-case}`)
- [x] 생성된 노트에 `## 연결` 섹션 + 최소 2개 wikilink 포함

---

## T7. 회귀 테스트

> `package.json`에 `test` 스크립트 미정의 → `node tests/test-*.js` 개별 실행으로 대체

- [x] 전체 테스트 통과 (**100 passed, 0 failed**)
- [x] wiki-reviewer model: sonnet 허용 확인 (`test-agents.js` 통과)
- [x] register-hooks 멱등성 통과 (`test-register-hooks.js` 통과)
- [x] pre/post read hook 로직 통과 (`test-pre-read-hook.js`, `test-post-read-hook.js` 통과)

---

## 발견된 버그 및 개선 사항

### 버그①: register-hooks.js 경로 오계산

- **파일**: `scripts/register-hooks.js`
- **증상**: CLI 실행 시 `.claude/settings.local.json` 대신 `init/.claude/settings.local.json` 생성
- **원인**: `const projectDir = process.argv[2] || process.cwd()` — CLI 서브커맨드(`"init"`)를 프로젝트 경로로 해석
- **재현**: `node bin/wiki-optimizer.js init` (샌드박스 프로젝트 루트에서 실행)
- **수정 방향**: `register-hooks.js`를 독립 실행이 아닌 `require()`로 호출 시 `process.argv` 대신 `process.cwd()`를 항상 사용하도록 변경

```js
// 현재 (버그)
const projectDir = process.argv[2] || process.cwd();

// 수정안
const projectDir = process.cwd();
```

### 버그②: T2-2 테스트 플랜과 구현 동작 불일치

- **파일**: `hooks/pre-read-wiki-intercept.js`, `hooks/lib/pattern-detector.js`
- **테스트 플랜 기대**: `src/**/*.java` Glob → `hookDecision: "block"`
- **실제 동작**: 경로에 등록된 도메인 세그먼트 없으면 `continue` 반환
- **구현 의도**: `pattern-detector.js` 주석에 명시된 설계 결정 ("false positive 방지")
- **수정 방향**: 테스트 플랜의 T2-2 시나리오를 `src/eq/**/*.java`로 수정하거나, 도메인 등록 후 전체 탐색도 차단하는 정책으로 구현 변경

### 개선 사항①: wiki-writer frontmatter 누락

- **증상**: T3-1에서 `dependencies` 필드 누락
- **수정 방향**: wiki-writer 프롬프트에 5개 필드 체크리스트 강제 출력 추가

### 개선 사항②: npm test 스크립트 미정의

- **증상**: `npm test` → `Missing script: "test"` 오류
- **수정 방향**: `package.json`에 `"test": "node tests/test-*.js"` 추가

### 개선 사항③: wiki-writer 언어 컨텍스트

- **증상**: Java 프로젝트에서 pattern ingest 시 TypeScript 코드 예시 생성
- **수정 방향**: ingest 스킬에서 소스 파일 확장자를 wiki-writer에 전달하여 언어 일치

---

## 생성된 위키 파일 목록

```
.wiki/
├── index.md               (갱신: Concept 1건, Pattern 1건, Domain 1건)
├── log.md                 (갱신: 4건 기록)
├── config.json
├── rules/
│   ├── format.md
│   ├── operations.md
│   └── templates.md
├── concept/
│   └── base-dto.md        (신규 생성)
├── pattern/
│   └── base-dto-inheritance.md  (신규 생성)
└── domain/
    └── eq-equipment-repair.md   (신규 생성, --fix로 깨진 링크 수정)
```
