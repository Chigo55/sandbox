# wiki-optimizer v1.2.2 전체 기능 테스트 플랜

**작성일**: 2026-05-04  
**대상**: 샌드박스 프로젝트에서 플러그인 업데이트 후 전체 기능 검증  
**플러그인 버전**: 1.2.2  
**이전 결과 참조**: `2026-05-04-wiki-optimizer-test-results-v1.2.1.md`

---

## 검증 범위

| 섹션 | 내용 |
|------|------|
| T0 | 샌드박스 환경 준비 & 플러그인 업데이트 |
| T1 | `/wiki-init` — 초기화, CLAUDE.md CLI 처리(신규) |
| T2 | 훅 동작 — SessionStart / PreToolUse / PostToolUse / Stop |
| T3 | `/wiki-ingest` — domain / concept / pattern / 업데이트 |
| T4 | `/wiki-query` — 기본 쿼리 / 위키 미비 처리 |
| T5 | `/wiki-lint` — 기본 / --fix / --deep |
| T6 | CLI (`wiki-optimizer`) — init / status / ingest / help |
| T7 | rules 파일 형식 준수 |
| T8 | 버그③ 수정 검증 — 훅 중복 등록 방지 |
| T9 | 회귀 테스트 (`npm test`) |

---

## T0. 샌드박스 환경 준비 & 플러그인 업데이트

### T0-1. 샌드박스 프로젝트 생성

```bash
# 1. 빈 테스트 프로젝트 생성
mkdir ~/sandbox-wiki-v122 && cd ~/sandbox-wiki-v122
git init
git config user.email "test@example.com"
git config user.name "Test"

# 2. 소스 파일 준비 (tree-sitter 분석 대상)
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

# 3. 초기 git 커밋
git add .
git commit -m "feat: initial sandbox source files"

# 4. .claude/ 디렉토리 (Claude Code 환경)
mkdir .claude
```

### T0-2. 플러그인 업데이트

```bash
# 플러그인 캐시 업데이트 (마켓플레이스 방식)
/plugin update wiki-optimizer

# 또는 수동 업데이트 (개발 환경)
# 플러그인 디렉토리에서 최신 버전 확인
node ~/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.2/bin/wiki-optimizer.js --version
```

**체크리스트**:
- [ ] 플러그인 버전이 `1.2.2`로 표시됨
- [ ] `package.json`, `plugin.json`, `bin/wiki-optimizer.js` 세 파일의 버전이 모두 `1.2.2`로 일치

### T0-3. 기존 v1.2.1 훅 오염 상태 시뮬레이션 (T8 전제)

> **목적**: 버그③ 수정 검증을 위해 이전 버전 절대경로 훅을 의도적으로 주입

```bash
# settings.local.json에 v1.2.1 형식 훅 수동 삽입
cat > .claude/settings.local.json << 'EOF'
{
  "hooks": {
    "SessionStart": [
      { "command": "node \"/home/user/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.1/hooks/session-start-wiki-index.js\"" }
    ],
    "PreToolUse": [
      { "command": "node \"/home/user/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.1/hooks/pre-read-wiki-intercept.js\"", "matcher": "Glob|Read" }
    ],
    "PostToolUse": [
      { "command": "node \"/home/user/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.1/hooks/post-read-wiki-suggest.js\"", "matcher": "Read" }
    ],
    "Stop": [
      { "command": "node \"/home/user/.claude/plugins/cache/wiki-optimizer-plugins/wiki-optimizer/1.2.1/hooks/stop-suggest-ingest.js\"" }
    ]
  }
}
EOF
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
- [ ] `.wiki/moc/`, `.wiki/summary/` **생성 안 됨** (제거된 디렉토리)

**파일 생성**:
- [ ] `.wiki/index.md` 생성됨 (Concept / Pattern / Domain 섹션 포함 템플릿)
- [ ] `.wiki/log.md` 생성됨 (헤더만 있는 빈 로그)
- [ ] `.wiki/config.json` 생성됨 (`domainExtractors` 필드 포함)
- [ ] `.wikiignore` 생성됨 (`node_modules/`, `dist/`, `.wiki/` 등 포함)

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
- [ ] 훅 커맨드 경로가 **절대경로** 형식 (v1.2.2 개선①: `${CLAUDE_PLUGIN_ROOT}` 즉시 치환)

**CLAUDE.md 처리 (신규 v1.2.2)**:
- [ ] CLI 터미널에 "CLAUDE.md가 없습니다" 경고 메시지 출력됨
- [ ] 경고 후 초기화는 정상 완료 (프로세스 종료 안 됨)

### T1-2. 최초 초기화 (CLAUDE.md 있는 경우)

**전제**: `echo "# My Project" > CLAUDE.md` 생성 후 `/wiki-init` 재실행  
(T1-1에서 `.wiki/`가 생성된 상태이므로 `.wiki/` 수동 삭제 후 실행)

**체크리스트**:
- [ ] "위키 링크를 CLAUDE.md에 추가하시겠습니까?" 또는 유사 질문 출력됨
- [ ] 사용자 입력 대기 (y/n)

**응답 `y` 시**:
- [ ] CLAUDE.md에 `[Wiki Index](.wiki/index.md)` 또는 유사 링크 추가됨
- [ ] 기존 CLAUDE.md 내용 보존됨 (`# My Project` 유지)

**응답 `n` 시**:
- [ ] CLAUDE.md 수정 없음
- [ ] 초기화는 정상 완료됨

### T1-3. tree-sitter 초기 노트 생성

**전제**: `npm install` 완료 (web-tree-sitter 의존성)

**체크리스트**:
- [ ] `.wiki/domain/eq-equipment-repair-service.md` 생성됨
- [ ] `.wiki/domain/inv-onhands-controller.md` 생성됨
- [ ] `.wiki/domain/base-d-t-o.md` 또는 유사 파일 생성됨
- [ ] 각 노트에 YAML frontmatter 포함 (`#auto-generated` 태그)
- [ ] 각 노트에 `## 감지된 클래스`, `## 감지된 함수` 섹션 포함
- [ ] 각 노트에 `## 개요`, `## 주요 로직` TODO 섹션 포함
- [ ] `.wiki/index.md` Domain 섹션에 생성된 노트들 등록됨

> tree-sitter npm 미설치 시: CLI 경고 메시지 출력 후 계속 진행 — 정상 동작

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
- [ ] `<wiki-index>` 내에 Domain 섹션 내용 포함됨 (index.md에 내용 있는 경우)
- [ ] Concept, Pattern 섹션도 내용 있으면 포함됨
- [ ] index.md가 비어있으면 조용히 종료 (오류 없음, 빈 출력 없음)
- [ ] 훅 실행 시간 < 500ms (경량 Node.js 처리)

### T2-2. PreToolUse 훅 — 등록 도메인 재귀 Glob 차단

**전제**: index.md에 `eq` 도메인 등록됨

**방법**: `src/eq/**/*.java` Glob 시도

**체크리스트**:
- [ ] `hookDecision: "block"` 반환됨
- [ ] `/wiki-query eq domain related files` 힌트 메시지 출력됨
- [ ] Claude가 Glob 대신 `/wiki-query`를 사용하도록 유도됨

**참고 — 정상 통과 케이스**:
- `src/**/*.java` (등록 도메인 세그먼트 없음) → `hookDecision: "continue"` 반환됨
- `src/eq/EqEquipmentRepairService.java` (단일 파일, 비재귀) → `hookDecision: "continue"` 반환됨

### T2-3. PreToolUse 훅 — 미등록 도메인 재귀 Glob

**방법**: `src/newdomain/**/*.java` Glob 시도 (index.md 미등록 도메인)

**체크리스트**:
- [ ] `hookDecision: "continue"` 반환됨 (차단 안 함)
- [ ] system context에 `/wiki-ingest newdomain` 제안 메시지 출력됨

### T2-4. PostToolUse 훅 — 미등록 도메인 Read 후 제안

**방법**: `src/inv/InvOnhandsController.java` Read (index.md 미등록 상태)

**체크리스트**:
- [ ] `systemContext`에 `/wiki-ingest inv` 또는 `/wiki-ingest inv-onhands` 제안 출력됨
- [ ] 파일 경로에서 도메인 세그먼트 정상 추출됨

### T2-5. PostToolUse 훅 — 등록 도메인 Read 후 무반응

**방법**: `.wiki/domain/eq-equipment-repair.md` Read (등록된 파일)

**체크리스트**:
- [ ] `/wiki-ingest` 제안 출력 안 됨 (이미 등록된 도메인)
- [ ] 훅이 조용히 종료됨 (오류 없음)

### T2-6. Stop 훅 — 세션 종료 인제스트 제안

**전제**: 미등록 도메인 소스 파일 변경 후 git staged 또는 unstaged 상태

```bash
# 미등록 도메인 파일 추가
echo "public class InvInventoryService {}" > src/inv/InvInventoryService.java
```

**방법**: Claude Code 세션 종료

**체크리스트**:
- [ ] 변경 파일에서 도메인 추출됨 (`inv`, `inv-inventory` 등)
- [ ] 미등록 도메인에 대해 `/wiki-ingest` 제안 출력됨
- [ ] 이미 등록된 도메인 (`eq`)은 제안 목록에서 제외됨
- [ ] 훅 실행 시 git 명령 오류 없음

---

## T3. `/wiki-ingest` 테스트

### T3-1. 도메인 ingest — 신규 생성

**실행**: `/wiki-ingest eq-equipment-repair`

**체크리스트**:

**에이전트 오케스트레이션**:
- [ ] wiki-reviewer(sonnet) 디스패치됨
- [ ] wiki-reviewer가 `src/eq/EqEquipmentRepairService.java` 읽기 수행
- [ ] 분류 결정: `domain/` (화면/기능 기준)
- [ ] wiki-writer(haiku) 디스패치됨
- [ ] wiki-indexer(haiku) 디스패치됨

**생성된 노트 형식**:
- [ ] `.wiki/domain/eq-equipment-repair.md` 파일 생성됨
- [ ] YAML frontmatter 5개 필드 포함
  - `id`: `domain_eq-equipment-repair` 형식
  - `last_updated`: `YYYY-MM-DD` 형식
  - `source_files`: 소스 경로 배열
  - `dependencies`: 의존 노트 배열
  - `tags`: 태그 배열
- [ ] `## 개요` 섹션 포함
- [ ] `## 화면 구조` 또는 `## API 목록` 섹션 포함 (구조에 맞게)
- [ ] `## 주요 로직` 섹션 포함
- [ ] `## 연결` 섹션 포함 (최소 2개 wikilink, 각 링크에 `— 이유` 설명)
- [ ] wikilink 형식: `[[.wiki/category/page-name]]`
- [ ] 코드 예시가 있다면 Java 언어로 작성됨 (소스 언어 일치)

**인덱스 갱신**:
- [ ] `.wiki/index.md` Domain 섹션에 `eq-equipment-repair` 행 추가됨
- [ ] 행 형식: `| [[.wiki/domain/eq-equipment-repair]] | 한줄 요약 | YYYY-MM-DD |`
- [ ] `.wiki/log.md` 최상단에 ingest 항목 prepend됨
- [ ] log.md 형식: `## [YYYY-MM-DD] ingest | EqEquipmentRepairService`

### T3-2. concept ingest — 신규 생성

**실행**: `/wiki-ingest BaseDTO`

**체크리스트**:
- [ ] wiki-reviewer가 `src/common/BaseDTO.java` 읽기 수행
- [ ] 분류 결정: `concept/` ("무엇인가" 기준)
- [ ] `.wiki/concept/base-dto.md` 파일 생성됨
- [ ] YAML frontmatter 포함 (`id`: `concept_base-dto`)
- [ ] `## 정의` 섹션 포함
- [ ] `## 상세` 섹션 포함 (`errorCode`, `errorMsg` 필드 설명)
- [ ] `## 연결` 섹션 포함 (최소 2개 wikilink)
- [ ] `.wiki/index.md` Concept 섹션에 등록됨
- [ ] `.wiki/log.md` 갱신됨

### T3-3. pattern ingest — 신규 생성

**실행**: `/wiki-ingest pattern:BaseDTO상속패턴`

**체크리스트**:
- [ ] 분류 결정: `pattern/` ("어떻게 구현하는가" 기준, `pattern:` 접두사 명시)
- [ ] `.wiki/pattern/base-dto-inheritance.md` 또는 유사 파일 생성됨
- [ ] YAML frontmatter 포함 (`id`: `pattern_base-dto-inheritance`)
- [ ] `## 문제` 섹션 포함
- [ ] `## 해법` 섹션 포함 (Java 코드 예시 포함)
- [ ] `## 연결` 섹션 포함 (최소 2개 wikilink)
- [ ] 코드 예시가 Java로 작성됨 (TypeScript 아님)
- [ ] `.wiki/index.md` Pattern 섹션에 등록됨

### T3-4. 기존 auto-generated 노트 업데이트

**조건**: T1-3에서 auto-generated 노트 존재  
**실행**: `/wiki-ingest eq-equipment-repair` (이미 존재하는 도메인)

**체크리스트**:
- [ ] 기존 노트 감지 (`existing_note: true` 또는 유사 상태)
- [ ] 새로 생성하지 않고 기존 노트 업데이트
- [ ] `last_updated` 날짜 갱신됨
- [ ] `#auto-generated` 태그 제거됨 (또는 없음 확인)
- [ ] 소스 코드 기반 내용으로 보강됨 (TODO 섹션이 실제 내용으로 대체)
- [ ] log.md에 `**갱신**` 항목 기록됨

### T3-5. 존재하지 않는 도메인 ingest 시도

**실행**: `/wiki-ingest nonexistent-domain`

**체크리스트**:
- [ ] wiki-reviewer가 관련 소스 파일을 찾지 못함
- [ ] "위키에 추가할 소스 파일을 찾을 수 없습니다" 또는 유사 안내 출력됨
- [ ] 빈 노트가 생성되지 않음
- [ ] `.wiki/index.md` 변경 없음

---

## T4. `/wiki-query` 테스트

### T4-1. 기본 쿼리 — 위키 내용 기반 답변

**전제**: T3 완료 (concept, pattern, domain 노트 존재)

**실행**: `/wiki-query BaseDTO errorCode 처리 방법`

**체크리스트**:
- [ ] wiki-reviewer(sonnet) 디스패치됨
- [ ] wiki-reviewer가 `.wiki/index.md` 읽기 수행
- [ ] 관련 페이지 식별: `concept/base-dto` 선택
- [ ] wiki-reviewer가 해당 페이지 읽기 수행
- [ ] 답변에 `errorCode`, `errorMsg` 필드 설명 포함
- [ ] 출처 wikilink 명시 (`[[.wiki/concept/base-dto]]`)
- [ ] 관련 노트 추가 제안 포함 (있는 경우)

### T4-2. 크로스 도메인 쿼리

**실행**: `/wiki-query EqEquipmentRepairService에서 BaseDTO 상속은 어떻게 되나요`

**체크리스트**:
- [ ] 복수 관련 페이지 식별 (`domain/eq-equipment-repair`, `concept/base-dto`, `pattern/base-dto-inheritance`)
- [ ] 최대 5개 관련 페이지 읽기
- [ ] 크로스 참조 답변 생성
- [ ] 복수 출처 wikilink 명시

### T4-3. 위키 미비 시 소스 탐색 제안

**실행**: `/wiki-query 위키에 등록되지 않은 InvInventoryService 동작`

**체크리스트**:
- [ ] "위키에 없는 내용입니다" 또는 유사 안내 출력됨
- [ ] `/wiki-ingest inv-inventory` 또는 유사 제안 출력됨
- [ ] 추가 소스 탐색 여부 질문 (선택적)

---

## T5. `/wiki-lint` 테스트

### T5-1. 기본 lint — 정상 상태

**전제**: T3 완료, 위키 일관성 있는 상태

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] wiki-reviewer가 모든 `.wiki/**/*.md` 읽기 수행
- [ ] 고아 노트 탐지 실행 (index.md 미등록 파일 검사)
- [ ] 깨진 링크 탐지 실행 (존재하지 않는 wikilink 검사)
- [ ] 고립 노트 탐지 실행 (`## 연결` 링크 0개 검사)
- [ ] 리포트 출력: "정상 N / 이슈 0" 형태
- [ ] 이슈 없으면 수정 동작 없음

### T5-2. 고아 노트 탐지

**전제**: `.wiki/domain/orphan-note.md` 수동 생성 (index.md 미등록)

```bash
echo "# Orphan" > .wiki/domain/orphan-note.md
```

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] `orphan-note` 고아 노트 1건 탐지됨
- [ ] 리포트에 고아 노트 파일명 명시됨

### T5-3. 깨진 링크 탐지

**전제**: 기존 노트의 `## 연결` 섹션에 존재하지 않는 wikilink 추가

```bash
# eq-equipment-repair.md에 깨진 링크 2건 추가
echo "- [[.wiki/pattern/nonexistent-pattern]] — 테스트용 깨진 링크" >> .wiki/domain/eq-equipment-repair.md
echo "- [[.wiki/concept/nonexistent-concept]] — 테스트용 깨진 링크" >> .wiki/domain/eq-equipment-repair.md
```

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] 깨진 링크 2건 탐지됨
- [ ] 리포트에 파일명 및 깨진 링크 경로 명시됨

### T5-4. `--fix` 모드

**전제**: T5-2, T5-3 이슈 상태 유지

**실행**: `/wiki-lint --fix`

**체크리스트**:
- [ ] 깨진 링크 수정 시도 (유사 노트로 교체 또는 제거)
- [ ] 고립 노트에 연결 추가됨 (해당 시)
- [ ] `log.md`에 lint --fix 항목 기록됨
- [ ] 리포트에 수정 내역 명시됨 (수정 건수, 파일명)

### T5-5. `--deep` 모드

**전제**: git commit 이력 존재, 소스 파일 변경 후 위키 미갱신 상태 시뮬레이션

```bash
# 소스 파일 변경 후 커밋 (위키는 갱신 안 함)
echo "  public List<RepairDTO> findAll() { return null; }" >> src/eq/EqEquipmentRepairService.java
git add .
git commit -m "feat: add findAll method"
```

**실행**: `/wiki-lint --deep`

**체크리스트**:
- [ ] wiki-reviewer가 `git log` 실행 (Bash 도구 사용)
- [ ] 소스 변경 후 위키 미갱신 노트 감지
- [ ] "오래된 페이지" 목록 출력 (`eq-equipment-repair` 포함)
- [ ] `/wiki-ingest eq-equipment-repair` 제안 출력됨
- [ ] git 이력 없으면 빈 결과 (오류 없음)

---

## T6. CLI (`wiki-optimizer`) 테스트

### T6-1. `wiki-optimizer --version`

**실행**: `wiki-optimizer --version`

**체크리스트**:
- [ ] `1.2.2` 출력됨
- [ ] `package.json`, `plugin.json`, `bin/wiki-optimizer.js` 버전과 일치

### T6-2. `wiki-optimizer help`

**실행**: `wiki-optimizer help`

**체크리스트**:
- [ ] 사용 가능한 서브커맨드 목록 출력됨 (`init`, `status`, `ingest`, `help`)
- [ ] 각 커맨드 설명 포함

### T6-3. `wiki-optimizer status`

**전제**: T1 완료 (`.wiki/` 구조 존재, 훅 등록 완료)

**실행**: `wiki-optimizer status`

**체크리스트**:
- [ ] `.wiki/` 구조 존재 여부 표시됨
- [ ] 훅 등록 상태 표시됨 (4종 훅 각각)
- [ ] 노트 수 통계 출력됨 (domain N, concept N, pattern N)
- [ ] 이슈 있으면 경고 표시

### T6-4. `wiki-optimizer status` — 미초기화 상태

**전제**: `.wiki/` 없는 새 디렉토리

**실행**: `wiki-optimizer status`

**체크리스트**:
- [ ] "wiki-optimizer가 초기화되지 않았습니다" 또는 유사 메시지
- [ ] `wiki-optimizer init` 실행 안내 출력됨

### T6-5. `wiki-optimizer init` (CLI 직접 실행)

**전제**: 미초기화 샌드박스

**실행**: `wiki-optimizer init`

**체크리스트**:
- [ ] `.wiki/` 구조 생성됨 (T1-1과 동일 체크리스트)
- [ ] 훅 등록됨
- [ ] CLAUDE.md 처리 로직 실행됨 (T1-1, T1-2 동일)

### T6-6. `wiki-optimizer ingest <path>`

**실행**: `wiki-optimizer ingest src/eq/`

**체크리스트**:
- [ ] `src/eq/` 하위 소스 파일 스캔됨
- [ ] 분류 결과 출력됨 (entity / concept 분류)
- [ ] 초기 도메인 노트 생성 시도됨 (tree-sitter 설치 시)

---

## T7. rules 파일 형식 준수 테스트

**전제**: T3 완료 (domain, concept, pattern 노트 존재)

**체크리스트**:

**format.md 준수**:
- [ ] 생성된 노트의 `id` 형식이 `{분류}_{kebab-case}` 기준 일치
  - `domain_eq-equipment-repair`
  - `concept_base-dto`
  - `pattern_base-dto-inheritance`
- [ ] wikilink 형식: `[[.wiki/category/page-name]]` (절대 경로 아님)
- [ ] YAML frontmatter 필드 순서: `id → last_updated → source_files → dependencies → tags`

**templates.md 준수**:
- [ ] domain 노트: `## 개요`, `## 화면 구조` (또는 `## API 목록`), `## 주요 로직`, `## 연결` 포함
- [ ] concept 노트: `## 정의`, `## 상세`, `## 연결` 포함
- [ ] pattern 노트: `## 문제`, `## 해법`, `## 연결` 포함

**operations.md 준수**:
- [ ] `## 연결` 섹션에 최소 2개 wikilink 포함
- [ ] 각 wikilink에 `— 이유` 설명 포함
- [ ] index.md 행 형식 일치

---

## T8. 버그③ 수정 검증 — 훅 중복 등록 방지

> **전제**: T0-3에서 v1.2.1 절대경로 훅이 의도적으로 주입된 상태

### T8-1. v1.2.2 훅 등록 실행

**실행**: `node <plugin-path>/scripts/register-hooks.js`  
또는 `/wiki-init` 실행

**체크리스트**:
- [ ] 실행 후 SessionStart 훅 항목이 **1개만** 존재 (중복 추가 없음)
- [ ] PreToolUse, PostToolUse, Stop 훅도 각각 **1개만** 존재
- [ ] v1.2.1 절대경로 훅이 v1.2.2 훅으로 **교체** 또는 v1.2.2 훅이 **추가 안 됨**
- [ ] `Already registered` 또는 유사 메시지 출력됨

### T8-2. 중복 판단 로직 검증

**체크리스트**:
- [ ] basename 기준으로 동일 훅 파일 인식 (경로 형식 무관)
  - `".../1.2.1/hooks/session-start-wiki-index.js"` 와 `".../1.2.2/hooks/session-start-wiki-index.js"` → 동일 훅으로 인식
- [ ] `${CLAUDE_PLUGIN_ROOT}` 형식 vs 절대경로 형식 → 동일 훅으로 인식

### T8-3. 개선① — 등록된 훅 경로 형식

**체크리스트**:
- [ ] `.claude/settings.local.json`에 등록된 훅 커맨드가 **절대경로** 형식
  - `"node \"/absolute/path/to/1.2.2/hooks/session-start-wiki-index.js\""`
- [ ] `${CLAUDE_PLUGIN_ROOT}` 환경변수 형식 **없음** (런타임 해석 불필요)

### T8-4. 다른 플러그인 훅 보존

**전제**: settings.local.json에 다른 플러그인 훅 항목 존재

**체크리스트**:
- [ ] 다른 플러그인 훅 항목 변경 없음
- [ ] wiki-optimizer 훅만 교체됨

---

## T9. 회귀 테스트

### T9-1. 자동화 테스트 전체 실행

**실행**: `npm test` (플러그인 디렉토리에서)

**체크리스트**:
- [ ] 전체 테스트 통과 (0 failed)
- [ ] 기존 100 passed 이상 유지

**개별 테스트 통과 확인**:

| 테스트 파일 | 핵심 검증 | 최소 통과 |
|------------|----------|----------|
| `test-agents.js` | 에이전트 model, tools 스펙 | 28 passed |
| `test-register-hooks.js` | 훅 등록 멱등성, **basename 중복 판단(신규)** | 10+ passed |
| `test-wiki-reader.js` | index.md 섹션 추출 | 9 passed |
| `test-pattern-detector.js` | 광범위 탐색 분류 | 18 passed |
| `test-pre-read-hook.js` | PreToolUse block/continue | 6 passed |
| `test-post-read-hook.js` | PostToolUse 도메인 감지 | 7 passed |
| `test-session-start-hook.js` | context 주입 | 6 passed |
| `test-stop-hook.js` | git diff 도메인 추출 | 6 passed |
| `test-ingest-watch.js` | 파일 스캔 | 10 passed |

### T9-2. v1.2.1 해소 항목 유지 확인

| v1.2.0 버그/개선 | v1.2.1 해소 | v1.2.2 유지 확인 |
|------------------|------------|-----------------|
| 버그① register-hooks.js 경로 오계산 | ✅ | - [ ] 회귀 없음 |
| 버그② src/eq/** block 미동작 | ✅ | - [ ] 회귀 없음 |
| 개선① dependencies frontmatter 누락 | ✅ | - [ ] 회귀 없음 |
| 개선② npm test 스크립트 미정의 | ✅ | - [ ] 회귀 없음 |
| 개선③ Java 프로젝트 TypeScript 코드 생성 | ✅ | - [ ] 회귀 없음 |

---

## 예상 이슈 및 주의사항

| 항목 | 상황 | 대처 |
|------|------|------|
| tree-sitter 미설치 | T1-3 skip | CLI 경고 출력 후 계속 진행 — 정상 |
| wiki-reviewer 응답 속도 | sonnet 사용으로 haiku 대비 느림 | 정상 — 30초 이내 |
| T0-3 오염 환경 | 실 환경에서 v1.2.1 훅 수동 삽입 필요 | 정확한 경로로 주입할 것 |
| T5-5 --deep | git 이력 없으면 빈 결과 | 정상 — T0-1에서 최소 1 commit 필요 |
| T8 버전 교체 vs 유지 | 기존 훅 교체(새 버전으로 갱신) vs 기존 유지(새 버전 추가 안 함) 동작 중 하나 — 둘 다 중복 없으면 통과 | 실제 동작 방식 기록 |
| CLAUDE.md CLI 구현 여부 | bin/wiki-optimizer.js 직접 구현 vs 스킬(LLM) 레벨 처리 | 구분하여 기록 |

---

## 테스트 결과 파일명

`2026-05-04-wiki-optimizer-test-results-v1.2.2.md`
