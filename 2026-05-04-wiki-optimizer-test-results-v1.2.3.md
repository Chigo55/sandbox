# wiki-optimizer v1.2.3 테스트 결과 보고서

**테스트 일시**: 2026-05-04  
**대상 버전**: wiki-optimizer v1.2.3  
**테스트 계획서**: `2026-05-04-wiki-optimizer-test-plan-v1.2.3.md`  
**이전 버전 결과**: `2026-05-04-wiki-optimizer-test-results-v1.2.2.md`  
**테스트 환경**: Windows 11 Pro, Claude Sonnet 4.6, wiki-optimizer-sandbox 프로젝트

---

## 전체 결과 요약

| 테스트 | 항목 수 | PASS | FAIL | SKIP |
|--------|---------|------|------|------|
| T0 환경 준비 & 버전 검증 | 3 | 3 | 0 | 0 |
| T1 /wiki-init | 4 | 3 | 1 | 0 |
| T2 훅 동작 | 6 | 6 | 0 | 0 |
| T3 /wiki-ingest | 4 | 4 | 0 | 0 |
| T4 /wiki-query | 1 | 1 | 0 | 0 |
| T5 /wiki-lint --deep | 1 | 1 | 0 | 0 |
| T6 CLI 기능 (BUG-3~7 수정) | 7 | 7 | 0 | 0 |
| T7 rules 포맷 준수 | 9 | 9 | 0 | 0 |
| T8 BUG-4 슬래시 경로 매칭 | 3 | 3 | 0 | 0 |
| T9 npm test (개별 실행) | 8 | 8 | 0 | 0 |
| **합계** | **46** | **45** | **1** | **0** |

**전체 Pass율: 97.8%** (45/46)

---

## v1.2.2 → v1.2.3 버그 수정 검증 요약

| 버그 | 내용 | v1.2.2 | v1.2.3 |
|------|------|--------|--------|
| BUG-3 | cmdStatus() rules 파일 하드코딩 (`docs.md`) | ❌ FAIL | ✅ PASS |
| BUG-4 | Stop 훅 슬래시 경로 매칭 불일치 | ❌ FAIL | ✅ PASS |
| BUG-5 | `--version` / `-v` 플래그 미구현 | ❌ FAIL | ✅ PASS |
| BUG-6 | `help` 출력에 `ingest` 명령 누락 | ❌ FAIL | ✅ PASS |
| BUG-7 | 미초기화 프로젝트 안내 없음 | ❌ FAIL | ✅ PASS |

**모든 버그 수정 확인됨.**

---

## T0. 환경 준비 & 플러그인 업데이트

| ID | 테스트 | 결과 | 비고 |
|----|--------|------|------|
| T0-1 | /reload-plugins 후 0 errors | ✅ PASS | 8 agents, 4 hooks, 0 errors |
| T0-2 | 버전 일관성 (package.json, plugin.json, bin) | ✅ PASS | 세 파일 모두 `1.2.3` |
| T0-3 | config.json 슬래시 extractor 기존 설정 | ✅ PASS | `$1/$2` domainTemplate 이미 존재 |

---

## T1. /wiki-init

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T1-1 | CLAUDE.md 없는 경우 경고 출력 | ✅ PASS | 코드 분석: `CLAUDE.md가 없습니다` 경고 + 생성 가이드 출력 |
| T1-2 | CLAUDE.md 있는 경우 y/n 프롬프트 | ❌ FAIL | 미구현 — CLAUDE.md 있을 때 위키 링크 추가 여부 물어보지 않음 |
| T1-3 | tree-sitter 미설치 시 처리 | ✅ PASS | `tree-sitter unavailable` 경고 후 skip, wiki 정상 동작 |
| T1-4 | 재실행 멱등성 | ✅ PASS | 모든 파일 `already exists` skip, 훅 `Already registered` ×4 |

**T1-2 상세 (FAIL):**

```javascript
// bin/wiki-optimizer.js cmdInit() 내 CLAUDE.md 처리 (라인 317-323)
var claudeMdPath = path.join(PROJECT_DIR, 'CLAUDE.md');
if (!fs.existsSync(claudeMdPath)) {
    logWarn('CLAUDE.md가 없습니다 — ...');
    // CLAUDE.md가 있는 경우: 아무 처리 없음 ← T1-2 FAIL 원인
}
```

테스트 계획서가 요구하는 `"위키 링크를 CLAUDE.md에 추가하시겠습니까?"` 프롬프트가 없음.

---

## T2. 훅 동작 검증

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T2-1 | SessionStart — `<wiki-index>` 주입 | ✅ PASS | Domain/Pattern/Concept 3섹션 포함, 응답 시간 < 500ms |
| T2-2 | PreToolUse — 등록 도메인 Glob 차단 | ✅ PASS | `src/eq/**/*.java` → `hookDecision: "block"` |
| T2-3 | PreToolUse — 미등록 도메인 Glob 통과 | ✅ PASS | `src/newdomain/**/*.java` → `hookDecision: "continue"` + 제안 |
| T2-4 | PostToolUse — 미등록 도메인 Read 후 제안 | ✅ PASS | `InvOnhandsController.java` → `/wiki-ingest onhands` 제안 |
| T2-5 | PostToolUse — 등록 도메인 Read 후 무반응 | ✅ PASS | `EqEquipmentRepairService.java` → 출력 없음 |
| T2-6 | Stop — BUG-4 슬래시 경로 매칭 | ✅ PASS | 상세 T8 참조 |

**훅 입력 형식 주의사항**: 훅 스크립트는 `tool_name`/`tool_input` (snake_case) 필드를 사용. `tool`/`toolInput` (camelCase) 입력 시 동작 안 함.

---

## T3. /wiki-ingest

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T3-1 | 신규 domain ingest (`inv-onhands`) | ✅ PASS | `.wiki/domain/inv-onhands.md` 생성, YAML 5필드, 3섹션, 역링크 3개 |
| T3-4 | 기존 노트 업데이트 (`eq-equipment-repair`) | ✅ PASS | 코드 예시 수정(`ResponseEntity<?>` → 실제 반환 타입), log.md update 기록 |
| T3-5 | 미존재 도메인 (`nonexistent-domain`) | ✅ PASS | 소스 없음 감지, graceful 중단, 가이드 출력 |
| T4-3대체 | wiki miss 케이스 (`InvInventoryService`) | ✅ PASS | 위키·소스 탐색 후 미등록 확인, `/wiki-ingest` 제안 |

**생성된 inv-onhands 노트 품질:**
- `id: domain_inv-onhands` ✅
- MVC/REST API 차이점 특이사항 기록 ✅ (`ModelAndView` 반환, `@GetMapping` 누락 주의사항)
- BaseDTO 미상속 현황 기록 ✅

---

## T4. /wiki-query

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T4-3 | 위키 미비 — InvInventoryService | ✅ PASS | "위키 미등록" 명시, 소스 파일 확인 후 `/wiki-ingest inv-inventory` 제안 |

---

## T5. /wiki-lint --deep

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T5-5 | `--deep` 소스 변경 대비 최신성 점검 | ✅ PASS | 미등록 소스 2건 탐지 (inv-inventory, pay-refund), 예시 깨진 링크 1건 |

**--deep 탐지 결과:**
- 콘텐츠 노트 깨진 링크: 0건 ✅
- 고아 노트: 0건 ✅
- 미등록 소스: 2건 (`InvInventoryService.java`, `PayRefundService.java`)
- 예시 링크(rules/): `[[.wiki/pattern/cud-service]]` — 수동 처리 필요

---

## T6. CLI 기능 (BUG-3~7 수정 검증)

| ID | 테스트 | 결과 | v1.2.2 | v1.2.3 |
|----|--------|------|--------|--------|
| T6-1 | `--version` 출력 | ✅ PASS | ❌ FAIL | `1.2.3` 출력 |
| T6-1b | `-v` 단축 플래그 | ✅ PASS | ❌ FAIL | `1.2.3` 출력 |
| T6-2 | `help` — `ingest` 명령 포함 | ✅ PASS | ❌ FAIL | `ingest <path>` 출력 |
| T6-3 | `status` — 초기화 완료 상태 | ✅ PASS | ❌ FAIL | rules 3파일 정상 인식 |
| T6-4 | `status` — 미초기화 안내 | ✅ PASS | ❌ FAIL | "초기화되지 않았습니다" 출력 |
| T6-5 | `init` CLI 직접 실행 | ✅ PASS | — | 멱등 실행, 모든 파일 skip |
| T6-6 | `ingest <path>` 소스 스캔 | ✅ PASS | — | 3 items 스캔 완료 |

**T6-3 수정 코드 확인 (BUG-3):**
```javascript
// v1.2.3 bin/wiki-optimizer.js cmdStatus() — 라인 337-339
var hasRules = fs.existsSync(path.join(WIKI_DIR, 'rules', 'format.md')) &&
  fs.existsSync(path.join(WIKI_DIR, 'rules', 'operations.md')) &&
  fs.existsSync(path.join(WIKI_DIR, 'rules', 'templates.md'));
// v1.2.2: fs.existsSync(path.join(WIKI_DIR, 'rules/docs.md')) ← 구버전 하드코딩
```

---

## T7. rules 포맷 준수

| 검증 항목 | concept/base-dto | pattern/base-dto-inheritance | domain/eq-equipment-repair | domain/inv-onhands |
|-----------|-----------------|------------------------------|---------------------------|-------------------|
| YAML 5개 필드 | ✅ | ✅ | ✅ | ✅ |
| id 형식 | ✅ | ✅ | ✅ | ✅ |
| last_updated | ✅ | ✅ | ✅ | ✅ |
| 분류 태그 | ✅ `#concept` | ✅ `#pattern` | ✅ `#domain` | ✅ `#domain` |
| wikilink `.md` 없음 | ✅ | ✅ | ✅ | ✅ |
| 링크 설명 (`— 이유`) | ✅ | ✅ | ✅ | ✅ |
| `## 연결` 최소 2개 | ✅ 3개 | ✅ 3개 | ✅ 3개 | ✅ 3개 |
| 임의 디렉토리 없음 | ✅ | ✅ | ✅ | ✅ |
| 파일명 kebab-case | ✅ | ✅ | ✅ | ✅ |

**전항목 PASS — 필수 규칙 9/9 준수.**

---

## T8. BUG-4 수정 검증 — Stop 훅 슬래시 경로 매칭

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T8-1 | 등록된 도메인 슬래시 경로 → ingest 제안 없음 | ✅ PASS | `equipment/eq-equipment-repair` → 등록 인식, 제안 없음 |
| T8-2 | 미등록 도메인 슬래시 경로 → ingest 제안 출력 | ✅ PASS | `refund/pay-refund` → `/wiki-ingest refund/pay-refund` 제안 |
| T8-3 | 3자 이하 세그먼트 오탐 방지 | ✅ PASS | `pay` (3자) → `3 > 3` false → 단독 비교 skip |

**수정 코드 (isRegisteredInIndex):**
```javascript
// v1.2.3 hooks/stop-suggest-ingest.js — 라인 90-95
function isRegisteredInIndex(domain) {
  const lower = indexContent.toLowerCase();
  const d = domain.toLowerCase();
  if (lower.includes(d)) return true;
  // 슬래시 경로 분리 → 세그먼트별 비교 (3자 이하 제외)
  return d.split('/').some(seg => seg.length > 3 && lower.includes(seg));
}
// v1.2.2: lower.includes(d) 단일 체크 → "equipment/eq-equipment-repair"가 index의
//         "eq-equipment-repair"에 미매칭 → false 반환 → 불필요한 ingest 제안 발생
```

---

## T9. npm test (개별 실행)

| 테스트 파일 | 결과 | 통과 수 |
|------------|------|---------|
| test-agents.js | ✅ PASS | 28 passed |
| test-wiki-reader.js | ✅ PASS | 9 passed |
| test-pattern-detector.js | ✅ PASS | 18 passed |
| test-register-hooks.js | ✅ PASS | 12 passed |
| test-pre-read-hook.js | ✅ PASS | 6 passed |
| test-post-read-hook.js | ✅ PASS | 7 passed |
| test-session-start-hook.js | ✅ PASS | 6 passed |
| test-stop-hook.js | ✅ PASS | 6 passed |
| **합계** | ✅ **92 passed, 0 failed** | |

> **참고**: `npm test` 직접 실행 시 Windows 환경에서 `for f in tests/test-*.js` bash 루프 구문 호환성 문제로 exit 1. 개별 파일 `node tests/test-*.js` 실행 시 전부 PASS.

---

## 잔존 이슈

| 번호 | 심각도 | 내용 |
|------|--------|------|
| IMP-1 | LOW | T1-2: CLAUDE.md 있을 때 위키 링크 추가 여부 대화형 확인 미구현 |
| IMP-2 | LOW | `npm test` Windows 셸 호환성 — `package.json` scripts의 `for` 루프를 cross-platform으로 수정 필요 (`cross-env`, `jest`, 또는 `node -e "require('./tests/run-all')"`) |
| IMP-3 | INFO | rules/ 예시 링크 `[[.wiki/pattern/cud-service]]` 미존재 — 문서 정리 권장 |

---

## v1.2.2 → v1.2.3 개선 비교

| 항목 | v1.2.2 | v1.2.3 |
|------|--------|--------|
| CLI --version / -v | ❌ unknown command | ✅ 1.2.3 출력 |
| CLI help (ingest 포함) | ❌ 누락 | ✅ ingest <path> 표시 |
| CLI status rules 체크 | ❌ docs.md 하드코딩 | ✅ format/operations/templates 3파일 |
| CLI status 미초기화 안내 | ❌ 안내 없음 | ✅ "초기화되지 않았습니다" + init 가이드 |
| Stop 훅 슬래시 경로 매칭 | ❌ 오탐 | ✅ 세그먼트 분리 비교 |
| npm test 통과 | ✅ 92 (개별 실행) | ✅ 92 (개별 실행) |
| 전체 Pass율 | 86.4% (38/44) | **97.8% (45/46)** |

---

## 결론

**wiki-optimizer v1.2.3은 v1.2.2에서 식별된 BUG-3~7 전부 수정 완료.**

- T6 CLI 5개 항목 모두 PASS (v1.2.2에서 4개 FAIL → 0개 FAIL)
- T8 BUG-4 슬래시 경로 매칭 3개 항목 전부 PASS
- 핵심 기능(ingest/query/lint/hooks) 계속 안정적 동작
- 잔존 이슈: T1-2 CLAUDE.md 대화 프롬프트 미구현 (LOW), npm test Windows 호환성 (LOW)

**v1.2.3 배포 적합 판정.**
