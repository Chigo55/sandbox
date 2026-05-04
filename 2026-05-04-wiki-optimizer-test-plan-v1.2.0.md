# wiki-optimizer v1.2.0 테스트 플랜

**작성일**: 2026-05-04  
**대상**: 샌드박스 프로젝트에서 플러그인 전체 기능 검증

---

## 샌드박스 환경 준비

```bash
# 1. 빈 테스트 프로젝트 생성
mkdir ~/sandbox-wiki-test && cd ~/sandbox-wiki-test
git init

# 2. 소스 파일 준비 (tree-sitter 분석 대상)
mkdir -p src/eq src/inv
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

# 3. .claude/ 디렉토리 (Claude Code 환경 시뮬레이션)
mkdir .claude
```

---

## T1. /wiki-init 테스트

### T1-1. 최초 초기화

**실행**: 샌드박스 프로젝트에서 `/wiki-init`

**체크리스트**:
- [ ] `.wiki/concept/` 생성됨
- [ ] `.wiki/pattern/` 생성됨
- [ ] `.wiki/domain/` 생성됨
- [ ] `.wiki/moc/`, `.wiki/summary/` **생성 안 됨** (제거 확인)
- [ ] `.wiki/index.md` 생성됨 (템플릿 형태)
- [ ] `.wiki/log.md` 생성됨
- [ ] `.wiki/rules/format.md` 복사됨
- [ ] `.wiki/rules/operations.md` 복사됨
- [ ] `.wiki/rules/templates.md` 복사됨
- [ ] `.wiki/config.json` 생성됨
- [ ] `.wikiignore` 생성됨 (node_modules/, dist/ 등 포함)
- [ ] `.claude/settings.local.json`에 4종 훅 등록됨 (SessionStart/PreToolUse/PostToolUse/Stop)

### T1-2. tree-sitter 초기 노트 생성

**전제**: `npm install -g wiki-optimizer` (또는 플러그인 디렉토리에서 `npm install`)

**체크리스트**:
- [ ] `.wiki/domain/eq-equipment-repair-service.md` 생성됨
- [ ] `.wiki/domain/inv-onhands-controller.md` 생성됨
- [ ] `.wiki/domain/base-d-t-o.md` 또는 유사 파일 생성됨
- [ ] 각 노트에 YAML frontmatter 포함 (`#auto-generated` 태그)
- [ ] 각 노트에 `## 감지된 클래스`, `## 감지된 함수` 섹션 포함
- [ ] 각 노트에 `## 개요`, `## 주요 로직` TODO 섹션 포함
- [ ] `.wiki/index.md` Domain 섹션에 생성된 노트들 등록됨

### T1-3. 재실행 멱등성

**실행**: `/wiki-init` 재실행

**체크리스트**:
- [ ] 기존 파일 덮어쓰지 않음 (skip 메시지 출력)
- [ ] 훅 중복 등록 안 됨

### T1-4. CLAUDE.md 없는 경우

**체크리스트**:
- [ ] "CLAUDE.md가 없습니다" 경고 메시지 출력됨

### T1-5. CLAUDE.md 있는 경우

**준비**: `echo "# Test" > CLAUDE.md`

**체크리스트**:
- [ ] 위키 링크 추가 여부 확인 질문 출력됨

---

## T2. 훅 동작 테스트

### T2-1. SessionStart 훅

**방법**: 새 Claude Code 세션 시작

**체크리스트**:
- [ ] system context에 `<wiki-index>` 블록 주입됨
- [ ] Domain/Pattern/Concept 섹션 내용이 포함됨 (index.md에 내용 있는 경우)
- [ ] index.md가 비어있으면 조용히 종료 (오류 없음)

### T2-2. PreToolUse 훅 — 광범위 탐색 차단

**설계 결정**: 경로에 등록된 도메인 세그먼트가 포함된 재귀 Glob만 `block`. false positive 방지를 위한 의도된 동작.  
(`pattern-detector.js` 주석 참고: "Only `**` (recursive glob) is classified as broad")

**방법**: `src/eq/**/*.java` Glob 시도 (index.md에 `eq` 도메인 등록 후)

**체크리스트**:
- [ ] `hookDecision: "block"` 반환됨
- [ ] `/wiki-query eq domain related files` 힌트 출력됨

**참고** (정상 동작):
- `src/**/*.java` (등록 도메인 세그먼트 없음) → `hookDecision: "continue"` 반환됨

### T2-3. PreToolUse 훅 — 미등록 도메인 통과

**방법**: `src/new-domain/**/*.java` Glob 시도 (미등록 도메인)

**체크리스트**:
- [ ] `hookDecision: "continue"` 반환됨
- [ ] 위키 쿼리 제안 메시지 출력됨

### T2-4. PostToolUse 훅 — 미등록 도메인 제안

**방법**: 미등록 도메인 소스 파일 Read

**체크리스트**:
- [ ] `systemContext`에 `/wiki-ingest {domain}` 제안 출력됨

### T2-5. Stop 훅 — 세션 종료 제안

**방법**: git 변경 파일이 있는 상태에서 세션 종료

**체크리스트**:
- [ ] 미등록 도메인에 대해 `/wiki-ingest` 제안 출력됨

---

## T3. /wiki-ingest 테스트

### T3-1. 도메인 ingest

**실행**: `/wiki-ingest eq-equipment-repair`

**체크리스트**:
- [ ] wiki-reviewer(sonnet) 디스패치됨
- [ ] 분류 결정: `domain/` (소스 파일 기반)
- [ ] wiki-writer가 `.wiki/domain/eq-equipment-repair.md` 작성
- [ ] YAML frontmatter 5개 필드 포함
- [ ] `## 개요`, `## 화면 구조`, `## 주요 로직`, `## SP 목록`, `## 연결` 섹션 포함
- [ ] `## 연결`에 최소 2개 wikilink 포함
- [ ] wiki-indexer가 `index.md` Domain 섹션 갱신
- [ ] wiki-indexer가 `log.md`에 항목 prepend (`**갱신**` 항목 포함 확인)

### T3-2. concept ingest

**실행**: `/wiki-ingest BaseDTO`

**체크리스트**:
- [ ] 분류 결정: `concept/` ("무엇인가" 기준)
- [ ] `.wiki/concept/base-dto.md` 작성
- [ ] `## 정의`, `## 상세`, `## 연결` 섹션 포함

### T3-3. pattern ingest

**실행**: `/wiki-ingest pattern:BaseDTO상속패턴`

**체크리스트**:
- [ ] 분류 결정: `pattern/` ("어떻게 구현하는가" 기준)
- [ ] `## 문제`, `## 해법`, `## 연결` 섹션 포함

### T3-4. 기존 노트 업데이트

**조건**: T1-2에서 auto-generated 노트 존재
**실행**: `/wiki-ingest eq-equipment-repair` (이미 존재하는 도메인)

**체크리스트**:
- [ ] 기존 노트 업데이트 (새로 생성 아님)
- [ ] `last_updated` 갱신됨
- [ ] `#auto-generated` 태그 제거 여부 확인

---

## T4. /wiki-query 테스트

### T4-1. 기본 쿼리

**실행**: `/wiki-query BaseDTO errorCode 처리 방법`

**체크리스트**:
- [ ] wiki-reviewer가 index.md에서 관련 페이지 식별
- [ ] 관련 페이지 읽기 후 답변 생성
- [ ] 출처 wikilink 명시 (`[[.wiki/concept/base-dto]]`)

### T4-2. 위키 미비 시 소스 탐색 제안

**실행**: `/wiki-query 위키에 없는 내용 질문`

**체크리스트**:
- [ ] "위키에 없는 내용" 안내 출력
- [ ] `/wiki-ingest` 제안 출력

---

## T5. /wiki-lint 테스트

### T5-1. 기본 lint

**실행**: `/wiki-lint`

**체크리스트**:
- [ ] 고아 노트 탐지 (index.md 미등록 파일)
- [ ] 깨진 링크 탐지 (존재하지 않는 wikilink)
- [ ] 고립 노트 탐지 (`## 연결` 0개)
- [ ] 리포트 출력 (정상/이슈 수)

### T5-2. --fix 모드

**실행**: `/wiki-lint --fix`

**체크리스트**:
- [ ] 고립 노트에 연결 추가됨
- [ ] log.md에 lint 항목 기록됨

### T5-3. --deep 모드

**전제**: git commit 이력 존재, 오래된 위키 노트 존재

**실행**: `/wiki-lint --deep`

**체크리스트**:
- [ ] wiki-reviewer가 `git log` 실행 (Bash 도구 사용)
- [ ] 코드 변경됐지만 위키 갱신 안 된 노트 감지
- [ ] "오래된 페이지" 목록 출력

---

## T6. rules 파일 참조 테스트

**체크리스트**:
- [ ] `/wiki-ingest` 실행 시 wiki-writer가 `rules/format.md` wikilink 형식 준수
- [ ] 생성된 노트의 `id` 형식이 `rules/format.md` 기준 일치
- [ ] 생성된 노트에 `## 연결` 섹션 + 최소 2개 wikilink 포함

---

## T7. 회귀 테스트

**실행**: `npm test` (플러그인 디렉토리)

**체크리스트**:
- [ ] 전체 테스트 통과 (0 failed)
- [ ] wiki-reviewer model: sonnet 허용 확인
- [ ] register-hooks 멱등성 통과
- [ ] pre/post read hook 로직 통과

---

## 예상 이슈 및 주의사항

| 항목 | 주의 |
|------|------|
| tree-sitter npm 미설치 | `npm install` 없으면 T1-2 skip — 정상 동작 확인 |
| wiki-reviewer sonnet | 응답 속도 haiku 대비 느림 — 정상 |
| --deep lint | git 이력 없으면 빈 결과 — 정상 |
| auto-generated 노트 | T3-1과 중복 시 update 동작 확인 필요 |
