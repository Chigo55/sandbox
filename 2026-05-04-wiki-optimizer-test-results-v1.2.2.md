# wiki-optimizer v1.2.2 테스트 결과 보고서

**테스트 일시**: 2026-05-04  
**대상 버전**: wiki-optimizer v1.2.2  
**테스트 계획서**: `2026-05-04-wiki-optimizer-test-plan-v1.2.2.md`  
**테스트 환경**: Windows 11 Pro, Claude Sonnet 4.6, wiki-optimizer-sandbox 프로젝트

---

## 전체 결과 요약

| 테스트 | 항목 수 | PASS | FAIL | SKIP |
|--------|---------|------|------|------|
| T0 설치·구조 검증 | 6 | 6 | 0 | 0 |
| T1 wiki-init | 3 | 3 | 0 | 0 |
| T2 훅 동작 검증 | 6 | 4 | 2 | 0 |
| T3 /wiki-ingest | 5 | 5 | 0 | 0 |
| T4 /wiki-query | 3 | 3 | 0 | 0 |
| T5 /wiki-lint | 5 | 5 | 0 | 0 |
| T6 CLI 기능 | 4 | 0 | 4 | 0 |
| T7 rules 포맷 준수 | 9 | 9 | 0 | 0 |
| T8 훅 중복 등록 방지 | 2 | 2 | 0 | 0 |
| T9 npm test | 1 | 1 | 0 | 0 |
| **합계** | **44** | **38** | **6** | **0** |

**전체 Pass율: 86.4%** (38/44)

---

## T0: 설치·구조 검증

**목적**: v1.2.2 설치 후 /doctor 오류 0건 확인, 플러그인 구조 검증

| ID | 테스트 | 결과 | 비고 |
|----|--------|------|------|
| T0-1 | /doctor 오류 0건 | ✅ PASS | agents 8개, hooks 4개, 오류 0건 |
| T0-2 | plugin.json agents 배열 정확 | ✅ PASS | wiki-reviewer/writer/indexer 3개 등록 |
| T0-3 | plugin.json hooks 필드 없음 | ✅ PASS | hooks 이중 로드 버그 수정 확인 |
| T0-4 | 훅 파일 4개 존재 | ✅ PASS | session-start, pre-read, post-read, stop |
| T0-5 | skills 디렉토리 구조 | ✅ PASS | ingest/query/lint 3개 스킬 |
| T0-6 | rules 파일 3분할 확인 | ✅ PASS | format.md, operations.md, templates.md |

**결론**: v1.1.0에서 발생한 /doctor E-1~4 오류 전부 수정 확인.

---

## T1: wiki-init

**목적**: `/wiki-init` 명령으로 .wiki/ 구조 초기화 검증

| ID | 테스트 | 결과 | 비고 |
|----|--------|------|------|
| T1-1 | .wiki/ 디렉토리 구조 생성 | ✅ PASS | concept/, pattern/, domain/ 생성 |
| T1-2 | index.md, log.md 초기화 | ✅ PASS | 올바른 초기 구조로 생성 |
| T1-3 | settings.local.json 훅 등록 | ✅ PASS | 4개 훅 등록 확인 |

**결론**: 전항목 정상.

---

## T2: 훅 동작 검증

**목적**: SessionStart/PreToolUse/PostToolUse/Stop 훅의 실제 동작 확인

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T2-1 | SessionStart — 세션 시작 시 index.md 로드 | ✅ PASS | 위키 인덱스 자동 컨텍스트 주입 확인 |
| T2-2 | PreToolUse(Glob) — src/.java 접근 시 block 결정 | ✅ PASS | `hookDecision: "block"` 반환 확인 |
| T2-3 | PreToolUse(Read) — .wiki/ 접근 시 pass | ✅ PASS | 위키 파일 Read 정상 허용 |
| T2-4 | PostToolUse(Read) — 위키 파일 Read 후 suggest | ✅ PASS | 관련 노트 제안 메시지 출력 |
| T2-5 | Stop — 세션 종료 시 미인식 소스 감지 | ✅ PASS | InvOnhandsController 변경 감지 후 ingest 제안 |
| T2-6 | Stop — config.json extractor 도메인 매칭 | ❌ FAIL | extractor가 `equipment/eq-equipment-repair` 생성, index는 `eq-equipment-repair` → includes() 불일치 |

**T2-6 상세 분석:**

```
config.json 도메인 추출 결과: "equipment/eq-equipment-repair"
index.md 등록 경로:           "eq-equipment-repair"
includes() 검사:              "eq-equipment-repair".includes("equipment/eq-equipment-repair") → false
결과: 인식 실패, 불필요한 ingest 제안 발생
```

**원인**: `config.json` domainExtractors의 `$1/$2` 템플릿이 중간 디렉토리를 포함한 경로를 생성하지만, `stop-suggest-ingest.js`의 `overlapsWithDomain()` 비교 로직이 평탄화된 경로(`eq-equipment-repair`)와 매칭하지 못함.

**영향**: 이미 등록된 도메인에 대해 불필요한 ingest 제안이 반복 발생.

---

## T3: /wiki-ingest

**목적**: 다양한 인풋으로 ingest 정확성 및 에러 핸들링 검증

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T3-1 | `/wiki-ingest eq-equipment-repair` | ✅ PASS | domain/ 노트 생성, YAML frontmatter 5필드, API 목록·코드 정확 |
| T3-2 | `/wiki-ingest BaseDTO` | ✅ PASS | concept/ 분류 정확, 필드 테이블·코드 예시 포함 |
| T3-3 | `/wiki-ingest pattern:BaseDTO상속패턴` | ✅ PASS | pattern/ 분류 정확, 구현 코드·적용 기준 테이블 포함 |
| T3-4 | 기존 노트 업데이트 (역링크 추가) | ✅ PASS | eq-equipment-repair.md에 pattern 역링크 추가 |
| T3-5 | `/wiki-ingest nonexistent-domain` | ✅ PASS | 소스 없음 감지, graceful 종료, 가이드 메시지 출력 |

**생성된 노트 품질 평가:**

- `.wiki/domain/eq-equipment-repair.md`: API 목록 3개, Java 코드 블록 3개, 특이사항 3개 — 고품질
- `.wiki/concept/base-dto.md`: 필드 테이블, 코드 정의, 특이사항 포함 — 적절
- `.wiki/pattern/base-dto-inheritance.md`: 구현 코드, 적용 기준 테이블, 미적용 현황 포함 — 고품질

---

## T4: /wiki-query

**목적**: 위키 검색 및 답변 생성 정확성 검증

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T4-1 | `BaseDTO errorCode 처리 방법` | ✅ PASS | concept+pattern 2개 출처 활용, 코드 예시 포함한 완전한 답변 |
| T4-2 | `eq 장비 수리 완료 처리 API` | ✅ PASS | domain 노트에서 PUT `/repair/{id}/complete` 즉시 답변 |
| T4-3 | `결제 도메인 환불 처리 로직` | ✅ PASS | 위키·소스 모두 없음 → "없음" 명시 + `/wiki-ingest` 제안 |

**크로스 도메인 쿼리**: concept/base-dto와 pattern/base-dto-inheritance를 동시 참조하여 종합 답변 생성 — 정상.

---

## T5: /wiki-lint

**목적**: 위키 무결성 점검 및 자동 수정 기능 검증

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T5-1 | `/wiki-lint` 기본 점검 | ✅ PASS | 3개 이슈 정확 감지 (역링크 누락 1, 깨진 링크 2) |
| T5-2 | 고아 노트 감지 | ✅ PASS | 고아 노트 없음 정확 확인 |
| T5-3 | 깨진 링크 감지 | ✅ PASS | `rules/` 예시 링크 2건 감지, 컨텍스트(예시용) 함께 보고 |
| T5-4 | `/wiki-lint --fix` | ✅ PASS | WARN-1 역링크 누락 자동 수정, WARN-2/3 수동 처리 분리 |
| T5-5 | `/wiki-lint --deep` | ✅ PASS | git log 분석으로 미등록 소스 2개 탐지 (InvOnhands, InvInventory) |

**--deep 탐지 결과:**
- `inv/onhands/InvOnhandsController.java` (2026-05-04 변경) → 위키 미등록
- `inv/inventory/InvInventoryService.java` (2026-04-29 변경) → 위키 미등록

---

## T6: CLI 기능

**목적**: `wiki-optimizer` CLI 명령어 동작 검증

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T6-1 | `wiki-optimizer --version` | ❌ FAIL | `unknown command '--version'` — `--version` 플래그 미구현 |
| T6-2 | `wiki-optimizer help` 출력 내용 | ❌ FAIL | `ingest` 명령이 help 출력에서 누락 |
| T6-3 | `wiki-optimizer status` 결과 | ❌ FAIL | `rules/docs.md` 존재 여부 체크 (v1.1.0 경로) — v1.2.2 분할 파일 미인식 |
| T6-4 | 미초기화 프로젝트 안내 | ❌ FAIL | "not initialized" 명시 메시지 및 init 제안 없음 |

**T6 상세:**

**T6-1 (--version 미구현)**:
```bash
$ wiki-optimizer --version
Error: unknown command '--version'
```
`bin/wiki-optimizer.js`에서 `VERSION = '1.2.2'`로 정의되어 있으나 `--version` 플래그 처리 코드 없음.

**T6-2 (ingest help 누락)**:
```
현재 help 출력:
  Commands:
    init    Initialize wiki structure
    status  Show wiki status
    help    Show help

누락: ingest <target>  Ingest source files into wiki
```

**T6-3 (status 경로 오류)**:
```javascript
// bin/wiki-optimizer.js cmdStatus()
const docsExists = fs.existsSync(path.join(wikiDir, 'rules/docs.md'));  // ← v1.1.0 경로
// 실제 v1.2.2: rules/format.md, rules/operations.md, rules/templates.md
```
결과: 항상 `rules not found` 또는 잘못된 상태 보고.

**T6-4 (미초기화 안내 없음)**:
초기화되지 않은 프로젝트에서 `wiki-optimizer status` 실행 시 일반 에러만 출력, init 유도 메시지 없음.

**수정 권고 (3개 항목)**:
```javascript
// T6-1: --version 추가
program.version(VERSION);

// T6-2: ingest 명령 help 등록
program
  .command('ingest [target]')
  .description('Ingest source files into wiki')
  ...

// T6-3: status 경로 수정
const formatExists = fs.existsSync(path.join(wikiDir, 'rules/format.md'));
const opsExists    = fs.existsSync(path.join(wikiDir, 'rules/operations.md'));
const tplExists    = fs.existsSync(path.join(wikiDir, 'rules/templates.md'));
```

---

## T7: rules 포맷 준수

**목적**: 생성된 노트가 format.md 및 templates.md 기준을 충족하는지 검증

| 검증 항목 | concept/base-dto | pattern/base-dto-inheritance | domain/eq-equipment-repair |
|-----------|-----------------|------------------------------|---------------------------|
| YAML 5개 필드 완비 | ✅ | ✅ | ✅ |
| id 형식 `{분류}_{kebab}` | ✅ | ✅ | ✅ |
| last_updated 갱신 | ✅ | ✅ | ✅ |
| 분류 태그 포함 | ✅ `#concept` | ✅ `#pattern` | ✅ `#domain` |
| wikilink `.md` 미포함 | ✅ | ✅ | ✅ |
| 링크 설명 (`— 이유`) | ✅ | ✅ | ✅ |
| `## 연결` 최소 2개 링크 | ✅ 3개 | ✅ 3개 | ✅ 3개 |
| 임의 디렉토리 없음 | ✅ | ✅ | ✅ |
| 파일명 kebab-case | ✅ | ✅ | ✅ |

**경미한 이슈:**
- concept 노트: `## 정의/상세` 대신 `## 개요/정의/필드` 사용 — 의미상 동등, 일관성 개선 권장
- pattern 노트: `## 문제/해법` 대신 `## 개요/구현 방법` 사용 — 의미상 동등

**결론**: 필수 규칙 9/9 PASS. 템플릿 섹션명 2건 경미한 차이.

---

## T8: 훅 중복 등록 방지

**목적**: 훅 재등록 시 중복 방지 로직 동작 확인

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T8-1 | v1.2.1 훅 존재 상태에서 v1.2.2 등록 시도 | ✅ PASS | basename 비교로 중복 감지, 기존 훅 유지 |
| T8-2 | 재등록 후 훅 개수 변화 없음 | ✅ PASS | 4개 → 4개 유지 |

**동작 원리**: `register-hooks.js`의 `alreadyRegistered()` 함수가 basename 비교로 버전 경로 무관하게 동일 훅 감지. 등록 전략은 "keep old" (기존 훅 우선 유지).

---

## T9: npm test

**목적**: 플러그인 단위 테스트 통과 확인

| ID | 테스트 | 결과 | 상세 |
|----|--------|------|------|
| T9-1 | `npm test` in plugin directory | ✅ PASS | extractDomainDirectories 정규식 수정 포함 전 테스트 통과 |

---

## 발견된 버그 및 개선 항목

### 버그 (수정 필요)

| 번호 | 심각도 | 위치 | 내용 |
|------|--------|------|------|
| BUG-3 | HIGH | `bin/wiki-optimizer.js` cmdStatus() | `rules/docs.md` 하드코딩 → v1.2.2 분할 파일 미인식 |
| BUG-4 | MEDIUM | `hooks/stop-suggest-ingest.js` | config.json extractor `$1/$2` 경로와 index 평탄 경로 매칭 불일치 |
| BUG-5 | LOW | `bin/wiki-optimizer.js` | `--version` 플래그 미구현 |
| BUG-6 | LOW | `bin/wiki-optimizer.js` | `ingest` 명령 help 출력 누락 |
| BUG-7 | LOW | `bin/wiki-optimizer.js` cmdStatus() | 미초기화 프로젝트 명시적 안내 없음 |

### 개선 권고

| 번호 | 분류 | 내용 |
|------|------|------|
| IMP-1 | UX | pattern 노트 템플릿 `## 문제/해법` 섹션명을 더 명확히 guide |
| IMP-2 | 일관성 | `errorCode` 값 체계 Enum화 — `#concept` 노트 특이사항에 기록됨 |
| IMP-3 | 확장 | `inv-onhands`, `inv-inventory` 미등록 도메인 ingest 권장 |

---

## v1.1.0 → v1.2.2 개선 비교

| 항목 | v1.1.0 | v1.2.2 |
|------|--------|--------|
| /doctor 오류 | 4건 (E-1~E-4) | 0건 ✅ |
| extractDomainDirectories 정규식 | `[^/]+` (중첩 경로만 매칭) | `[^/\]]+` (flat+중첩 모두 매칭) ✅ |
| hook 이중 로드 | plugin.json hooks 필드로 발생 | hooks 필드 제거로 수정 ✅ |
| rules 파일 | 단일 docs.md | format/operations/templates 3분할 ✅ |
| 훅 중복 방지 | 버전 경로 변경 시 중복 등록 | basename 비교로 방지 ✅ |
| CLI status 경로 | — | docs.md 하드코딩 버그 잔존 ❌ |
| CLI --version | — | 미구현 ❌ |

---

## 결론

**wiki-optimizer v1.2.2는 핵심 기능(ingest/query/lint)에서 안정적으로 동작**하며 v1.1.0의 주요 버그 4건이 모두 수정되었다.

- **합격 영역**: T0(설치), T1(init), T3(ingest), T4(query), T5(lint), T7(포맷), T8(중복방지), T9(테스트)
- **개선 필요**: T6 CLI 4개 항목 — 실제 사용에서 `wiki-optimizer` CLI를 직접 호출하는 경우 영향
- **주의**: T2-6 config.json extractor 경로 불일치 — stop 훅에서 불필요한 ingest 제안 발생

**권장 조치**: BUG-3(cmdStatus 경로), BUG-4(extractor 매칭) 수정 후 v1.2.3 릴리즈 권장.
