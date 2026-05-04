# 위키 노트 템플릿

> 분류별 노트 작성 시 이 템플릿을 사용하라.  
> YAML frontmatter와 섹션 구조는 필수. 내용은 채워 넣어라.

---

## concept 노트

**"이것이 무엇인가?"** — 시스템에 존재하는 엔티티·규칙·용어.  
한 페이지 = 한 개념. 두 개념이 섞이면 분리하라.

```markdown
---
id: concept_{kebab-case}
last_updated: YYYY-MM-DD
source_files: []
dependencies: []
tags: ["#concept"]
---

# {개념명}

> **관련**: [[.wiki/index]]

## 정의
{이 개념이 무엇인지. 한 문단.}

## 상세
{구체적 설명. 코드 예시 포함 가능.}

## Why Not
{흔한 잘못된 접근과 이유 — LLM 혼동 방지. 생략 가능.}

## 연결
- [[.wiki/index]] — 전체 카탈로그
- [[.wiki/xxx/yyy]] — {관계}
```

**예시 id**: `concept_base-dto`, `concept_errorcode-rule`, `concept_sp-response-format`

---

## pattern 노트

**"이것을 어떻게 구현하는가?"** — 반복되는 구현 방법·해결책.  
문제-해법 구조로 작성하라.

```markdown
---
id: pattern_{kebab-case}
last_updated: YYYY-MM-DD
source_files: []
dependencies: []
tags: ["#pattern"]
---

# {패턴명}

> **관련**: [[.wiki/index]]

## 문제
{이 패턴이 해결하는 구체적 상황.}

## 해법
{패턴 설명. 코드 예시 포함.}

## 적용 사례
- {도메인A에서 어떻게 사용됐는지}
- {도메인B에서 어떻게 사용됐는지}

## Why Not
{대안 접근과 왜 이 패턴이 더 나은지. 생략 가능.}

## 연결
- [[.wiki/index]] — 전체 카탈로그
- [[.wiki/xxx/yyy]] — {관계}
```

**예시 id**: `pattern_cud-service`, `pattern_popup-grid-sync`, `pattern_base-dto-inheritance`

---

## domain 노트

**"이 화면/기능은 어디서 어떻게 동작하는가?"** — 화면·비즈니스 단위 지식.  
SP 목록, 화면 구조, 특이사항을 포함하라.

```markdown
---
id: domain_{kebab-case}
last_updated: YYYY-MM-DD
source_files: ["path/to/service.ext"]
dependencies: []
tags: ["#domain"]
---

# {도메인명}

> **관련**: [[.wiki/index]]

## 개요
{도메인 비즈니스 설명. 1-3문장.}

## 화면 구조
{화면 유형, 컬럼 구성 특이사항.}

## 주요 로직
{핵심 이벤트·함수 설명.}

## SP 목록
| SP | 용도 | 파라미터 특이사항 |
|----|------|-----------------|

## 특이사항
{이 도메인 고유의 패턴, 버그 이력, 주의점.}

## 연결
- [[.wiki/index]] — 전체 카탈로그
- [[.wiki/xxx/yyy]] — {관계}
```

**예시 id**: `domain_eq-equipment-repair`, `domain_inv-onhands`, `domain_payment-refund`

---

## tree-sitter 자동 생성 노트 (초안)

초기화 시 CLI가 자동 생성하는 노트 형식.  
`/wiki-ingest`로 보강하기 전까지 이 상태를 유지한다.

```markdown
---
id: domain_{kebab-case}
last_updated: YYYY-MM-DD
source_files: ["path/to/file.ext"]
dependencies: []
tags: ["#domain", "#auto-generated"]
---

# {FileName}

> tree-sitter 자동 생성. `/wiki-ingest`로 보강 필요.  
> **관련**: [[.wiki/index]]

## 개요
<!-- TODO: 이 파일의 역할을 한 문단으로 설명 -->

## 감지된 클래스·함수
{tree-sitter 추출 결과 목록}

## 주요 로직
<!-- TODO: 핵심 이벤트·함수 설명 -->

## 연결
- [[.wiki/index]] — 전체 카탈로그
```
