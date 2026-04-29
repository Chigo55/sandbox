# 문서화 규칙 (LLM Wiki + Zettelkasten)

> **관련 문서**: [[.wiki/rules/workflow]] `@.wiki/rules/workflow.md` | [[.wiki/rules/backend]] `@.wiki/rules/backend.md` | [[.wiki/rules/frontend]] `@.wiki/rules/frontend.md` | [[.wiki/index]] `@.wiki/index.md` | `@CLAUDE.md`

## 개요: 3계층 아키텍처

Karpathy의 LLM Wiki 패턴을 이 프로젝트에 적용한 문서화 시스템.

```
Raw Sources  ──ingest──▶  Wiki (.wiki/)  ◀──schema──  Rules (.wiki/rules/)
(소스코드, SP, output)      (Claude 유지보수)                  (Claude 행동 규칙)
```

| 계층 | 경로 | 역할 | 관리 주체 |
|------|------|------|----------|
| **Raw Sources** | `src/`, `sql/`, `docs/output/` | 원본 소스 — 불변 | 사용자 |
| **Wiki** | `.wiki/` | 축적형 지식 기반 — 영속 산출물 | **Claude** |
| **Schema** | `.wiki/rules/`, `CLAUDE.md` | 행동 규칙 및 설정 | 사용자 + Claude |

**핵심 원칙**: Claude가 위키를 작성·유지보수한다. 사용자는 소스 큐레이션·질문·방향 제시에 집중한다.

---

## 위키 디렉토리 구조

```
.wiki/
  index.md          ← 전체 카탈로그 (항상 최신 유지)
  log.md            ← append-only 변경 기록
  concept/          ← 원자 개념 노트 (BaseDTO, SP 오류 처리 패턴 등)
  pattern/          ← 재사용 코딩 패턴 (CUD 서비스, 팝업→그리드 연동 등)
  domain/           ← 도메인 지식 (화면별 SP 목록, 비즈니스 규칙)
  moc/              ← Map of Content (주제별 탐색 허브)
  summary/          ← 세션 답변 중 Filing 가치 있는 지식
```

기존 디렉토리와의 관계:
- `docs/adr/` → 유지. 불변 아키텍처 결정. 위키에서 링크만.
- `docs/CODEMAPS/` → 유지. 코드 스냅샷(자동 생성). 위키와 목적이 다름.
- `docs/output/` → 유지. 세션 산출물 → **Ingest 대상**.
- **구분**: CODEMAPS = "코드에 지금 뭐가 있는지" / Wiki = "우리가 뭘 알고 왜 그런지"

---

## 위키 페이지 포맷

모든 위키 페이지는 아래 구조를 따른다. YAML frontmatter 사용 금지 — 기존 ADR/rules 스타일과 일관성 유지.

```markdown
# {페이지 제목}

> **관련 문서**: [[.wiki/moc/xxx]] | [[.wiki/rules/backend]]

**생성**: YYYY-MM-DD  **갱신**: YYYY-MM-DD
**분류**: concept | pattern | domain | moc | summary
**태그**: `#태그1` `#태그2`

## {섹션 — 분류별 상이, 아래 템플릿 참고}

## 연결
- [[.wiki/xxx/yyy]] — {왜 연결되는지 한 줄 설명}
- [[.wiki/xxx/zzz]] — {관계 설명}
```

### Wikilink 형식 [필수]

```
[[.wiki/category/page-name]]   ✅ 루트 상대경로, .md 확장자 생략
[[page-name]]                          ❌ 파일명만 — Obsidian Graph에서 연결 끊김
```

---

## 분류별 템플릿

### concept — 원자 개념 노트

한 개념 = 한 페이지. 두 개념이 섞이면 분리.

```markdown
# {개념명}

> **관련 문서**: [[links]]

**생성**: YYYY-MM-DD  **갱신**: YYYY-MM-DD
**분류**: concept
**태그**: `#태그`

## 정의
{한 문단. 이 개념이 무엇인지.}

## 상세
{구체적 설명. 코드 예시 포함 가능.}

## Why Not
{흔한 잘못된 접근과 이유 — LLM 혼동 방지. 생략 가능.}

## 연결
- [[link]] — {관계}
```

### pattern — 코딩 패턴

반복적으로 등장하는 구현 패턴. 문제-해법 구조.

```markdown
# {패턴명}

> **관련 문서**: [[links]]

**생성**: YYYY-MM-DD  **갱신**: YYYY-MM-DD
**분류**: pattern
**태그**: `#태그`

## 문제
{이 패턴이 해결하는 구체적 상황.}

## 해법
{패턴 설명 + 코드 예시.}

## 적용 사례
- {도메인A에서 어떻게 사용됐는지}
- {도메인B에서 어떻게 사용됐는지}

## Why Not
{대안 접근과 왜 이 패턴이 더 나은지.}

## 연결
- [[link]] — {관계}
```

### domain — 도메인 지식

화면 단위 비즈니스·기술 지식. SP 목록, 화면 구조, 특이사항.

```markdown
# {도메인명} ({모듈 약어})

> **관련 문서**: [[links]]

**생성**: YYYY-MM-DD  **갱신**: YYYY-MM-DD
**분류**: domain
**태그**: `#모듈약어` `#화면유형`

## 개요
{도메인 비즈니스 설명. 1-3문장.}

## 화면 구조
{화면 유형 (단순그리드/Master-Detail/캘린더+그리드 등), 컬럼 구성 특이사항.}

## 주요 로직

### {이벤트/함수명} — {한줄 요약}
{이벤트 또는 함수가 언제 발생하고, 무엇을 하는지 설명. 핵심 분기·계산 로직 포함.}

```javascript
// 핵심 코드 스니펫 (필요 시)
```

> 반복되는 패턴이면 [[.wiki/pattern/xxx]] 링크로 대체 가능.
> 단순 CRUD 또는 공통 패턴과 동일한 경우 생략 가능.

## SP 목록
| SP | 용도 | 파라미터 특이사항 |
|----|------|-----------------|
| SP_XXX_SELECT | 조회 | |
| SP_XXX_INSERT | 등록 | |

## 특이사항
{이 도메인 고유의 패턴, 버그 이력, 주의점.}

## 연결
- [[link]] — {관계}
```

### moc — Map of Content

주제별 탐색 허브. 직접 지식을 담지 않고 관련 페이지들을 모아 인덱싱.

```markdown
# {주제} MOC

> **관련 문서**: [[.wiki/index]]

**생성**: YYYY-MM-DD  **갱신**: YYYY-MM-DD
**분류**: moc
**태그**: `#moc` `#주제`

## 개요
{이 MOC가 다루는 범위.}

## 페이지 목록

### {하위 주제 1}
- [[.wiki/category/page]] — {한줄 설명}

### {하위 주제 2}
- [[.wiki/category/page]] — {한줄 설명}
```

### summary — 세션 Filing

세션 중 나온 좋은 답변·발견 사항. 재사용 가능하면 위키에 영속화.

```markdown
# {요약 제목}

> **관련 문서**: [[links]]

**생성**: YYYY-MM-DD  **갱신**: YYYY-MM-DD
**분류**: summary
**출처**: {세션 날짜 또는 질문 맥락}
**태그**: `#summary` `#태그`

## 맥락
{어떤 질문/작업에서 나온 지식인지.}

## 핵심 내용
{정리된 답변 또는 발견 사항.}

## 연결
- [[link]] — {관계}
```

---

## Operations

### Ingest — 소스 → 위키 통합

새로운 지식이 생겼을 때 위키에 반영한다.

**트리거 시점:**
| 시점 | 예시 |
|------|------|
| 도메인 개발 완료 (workflow 8~9단계) | 새 화면의 SP 목록, 화면 구조, 특이사항 |
| 코드 탐색 중 미문서화 패턴 발견 | MultiViewCalendar 날짜 강조 Date 객체 정규화 |
| 좋은 답변 → Filing 가치 있을 때 | SP 디버깅 순서, 파라미터 불일치 사례 |
| 세션 output에 아키텍처 인사이트 포함 시 | Security 리뷰 결과 패턴화 |

**Ingest 절차:**
1. `index.md`에서 해당 개념 존재 여부 확인
2. **존재 시**: 기존 페이지 업데이트, `**갱신**` 날짜 변경
3. **없을 시**: 적절한 `wiki/` 하위 디렉토리에 새 페이지 생성
4. `index.md` 카탈로그에 항목 추가/갱신
5. `log.md`에 Ingest 기록 append (상단에 추가)
6. 기존 관련 페이지에 양방향 `[[wikilink]]` 추가

### Query — 위키 기반 답변

1. `index.md` 먼저 확인 → 관련 페이지 탐색
2. 위키 페이지 읽어 답변
3. 위키가 불충분하면 소스 탐색 후 → Ingest 고려

### Lint — 위키 건강 점검

주기적 또는 대규모 변경 후 실행:
- 고아 페이지: `wiki/`에 있으나 `index.md`에 없는 것
- 깨진 링크: 존재하지 않는 페이지 참조
- 오래된 페이지: 30일 이상 갱신 없는데 관련 코드 변경된 경우
- 연결 없는 페이지: `연결` 섹션이 비어 있는 것

---

## index.md 포맷

```markdown
# Wiki Index

> 전체 위키 카탈로그. 카테고리별 분류, 한줄 요약 포함. [갱신: YYYY-MM-DD]

## Concept
| 페이지 | 요약 | 갱신 |
|--------|------|------|
| [[.wiki/concept/xxx]] | 한줄 요약 | 날짜 |

## Pattern
| 페이지 | 요약 | 갱신 |
|--------|------|------|

## Domain
| 페이지 | 요약 | 갱신 |
|--------|------|------|

## MOC
| 페이지 | 요약 | 갱신 |
|--------|------|------|

## Summary
| 페이지 | 요약 | 갱신 |
|--------|------|------|
```

---

## log.md 포맷

최신 항목이 상단. 한 항목 = 하나의 Ingest/Query/Lint 작업.

```markdown
# Wiki Log

> append-only. 최신이 상단. grep: `## \[`

## [YYYY-MM-DD] ingest | {제목}
- **출처**: {소스 경로 또는 맥락}
- **생성**: [[.wiki/xxx/yyy]]
- **갱신**: [[.wiki/xxx/zzz]] (링크 추가)
- **index.md**: 카탈로그 항목 추가

## [YYYY-MM-DD] query | {질문 제목}
- **질문**: {무엇을 물어봤는지}
- **결과**: Filing 여부 (filed: [[링크]] / not filed: 이유)

## [YYYY-MM-DD] lint | {점검 범위}
- **발견**: {고아 페이지 수, 깨진 링크 수}
- **조치**: {수행한 수정}
```

---

## Zettelkasten 핵심 규칙

1. **원자성**: 1 페이지 = 1 개념. 두 개념이 섞이면 분리.
2. **연결 필수**: 모든 새 페이지는 최소 2개 기존 페이지와 `[[wikilink]]`로 연결.
3. **고아 금지**: `index.md`에 등록되지 않은 페이지 불허.
4. **파일명**: `kebab-case`, 영문 또는 한국어 발음 영문 표기.
5. **링크 설명**: `[[link]]` 뒤에 항상 `— {왜 연결되는지}` 한 줄 설명.
6. **계층보다 연결**: 폴더는 분류용, 실제 구조는 링크로 만들어진다.

---

## 로컬 스킬 (Slash Commands)

Operations를 슬래시 커맨드로 호출 가능. 에이전트도 동일하게 사용.

| 커맨드 | 역할 | 파일 |
|--------|------|------|
| `/wiki-ingest [대상]` | 소스 → 위키 통합 | `skills/wiki/ingest/SKILL.md` |
| `/wiki-query <질문>` | 위키 기반 답변 + Filing | `skills/wiki/query/SKILL.md` |
| `/wiki-lint [--fix] [--deep]` | 위키 건강 점검 | `skills/wiki/lint/SKILL.md` |
