# 위키 포맷 규칙

> 모든 위키 노트 작성 시 이 규칙을 따른다. 예외 없음.

---

## YAML Frontmatter

**모든 위키 노트는 아래 5개 필드를 포함한 YAML frontmatter로 시작하라.**

```markdown
---
id: {분류}_{kebab-case}
last_updated: YYYY-MM-DD
source_files: ["path/to/source.ext"]
dependencies: ["{분류}_{관련페이지}"]
tags: ["#태그1", "#태그2"]
---
```

**규칙**:
- `id`: `concept_base-dto`, `pattern_cud-service`, `domain_eq-equipment-repair` 형식
- `last_updated`: 오늘 날짜 (ISO 8601, YYYY-MM-DD)
- `source_files`: 이 노트의 근거가 되는 소스 파일 경로 목록. 없으면 `[]`
- `dependencies`: 이 노트가 참조하는 다른 위키 노트 id 목록. 없으면 `[]`
- `tags`: `#` 접두사 포함. 분류 태그 필수 (`#concept`, `#pattern`, `#domain`)

❌ **금지**: frontmatter 없는 노트, 필드 누락, `last_updated` 갱신 안 함

---

## Wikilink 형식

**내부 링크는 반드시 루트 상대경로 + `.md` 생략 형식을 사용하라.**

✅ 올바른 형식: *(아래는 형식 예시 — 실제 파일이 없을 수 있음)*
```
[[.wiki/concept/base-dto]]
[[.wiki/pattern/example-pattern]]
[[.wiki/domain/eq-equipment-repair]]
[[.wiki/index]]
```

❌ 잘못된 형식:
```
[[base-dto]]                     ← 파일명만 — Obsidian Graph 연결 끊김
[[.wiki/concept/base-dto.md]]    ← .md 확장자 포함 금지
[BaseDTO](.wiki/concept/base-dto.md)  ← 마크다운 링크 형식 금지
```

**링크 설명 필수**: `[[link]]` 뒤에 항상 `— {왜 연결되는지}` 한 줄을 붙여라.

✅ 올바른 예:
```
- [[.wiki/concept/base-dto]] — errorCode 필드 상속 기반
- [[.wiki/pattern/example-pattern]] — 이 도메인의 CUD 구현에 적용
```

❌ 잘못된 예:
```
- [[.wiki/concept/base-dto]]
- [[.wiki/pattern/example-pattern]]
```

---

## 분류 기준

**.wiki/ 하위 디렉토리는 3개만 존재한다: `concept/`, `pattern/`, `domain/`**

| 분류 | 핵심 질문 | 예시 |
|------|----------|------|
| `concept/` | "이것이 **무엇**인가?" | BaseDTO, errorCode 규칙, SP 응답 포맷 |
| `pattern/` | "이것을 **어떻게** 구현하는가?" | BaseDTO 상속 패턴, 팝업→그리드 연동 |
| `domain/` | "이 화면/기능은 **어디서** 어떻게 동작하는가?" | EQ 장비 수리, 재고 현황 |

**판단이 애매한 경우**:
- "BaseDTO errorCode 처리" → concept (errorCode 규칙이 **무엇**인지) vs pattern (어떻게 **쓰는지**)?
  - 규칙·정의 중심이면 `concept/`, 구현 방법 중심이면 `pattern/`
- 하나의 개념에 "무엇"과 "어떻게"가 모두 필요하면 → 두 노트로 분리

❌ **금지**: `moc/`, `summary/`, 기타 임의 디렉토리 생성

---

## 연결 섹션

**모든 노트의 마지막 섹션은 `## 연결`이어야 하며 최소 2개 wikilink를 포함하라.**

```markdown
## 연결
- [[.wiki/index]] — 전체 카탈로그
- [[.wiki/concept/base-dto]] — errorCode 필드 상속 기반
```

❌ **금지**: `## 연결` 섹션 없는 노트, wikilink 1개 이하인 노트

---

## 파일명 규칙

- `kebab-case` 사용: `eq-equipment-repair.md`, `cud-service-pattern.md`
- 영문 또는 한국어 발음 영문 표기
- 공백, 특수문자, 한글 파일명 금지
