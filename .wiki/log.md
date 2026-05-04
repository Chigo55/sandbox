# Wiki Log

Append-only record of all wiki actions.
Format: `## [YYYY-MM-DD] type | description`

Quick check: `grep "^## \[" log.md | tail -10`

## [2026-05-04] lint | --fix 실행
- **점검**: 노트 3개 전수 확인
- **수정**: 깨진 링크 2건 수정 (domain/eq-equipment-repair.md)
  - [[.wiki/pattern/cud-service]] → [[.wiki/pattern/base-dto-inheritance]]
  - [[.wiki/concept/repair-status]] 제거 (대응 노트 없음)

## [2026-05-04] update | domain/eq-equipment-repair
- **갱신**: .wiki/domain/eq-equipment-repair.md
- **변경**: equipmentId String/optional 수정, findAll 분기 추가, RepairRequest 타입 수정

## [2026-05-04] ingest | pattern/base-dto-inheritance
- **생성**: .wiki/pattern/base-dto-inheritance.md
- **소스**: BaseDTO.java, InvOnhandsController.java
- **갱신**: index.md Pattern 섹션

## [2026-05-04] ingest | concept/base-dto
- **생성**: .wiki/concept/base-dto.md
- **소스**: BaseDTO.java
- **갱신**: index.md Concept 섹션

## [2026-05-04] ingest | domain/eq-equipment-repair
- **생성**: .wiki/domain/eq-equipment-repair.md
- **소스**: EqEquipmentRepairController.java, EqEquipmentRepairService.java
- **갱신**: index.md Domain 섹션

## [2026-05-04] init | 위키 초기화
- **출처**: wiki-optimizer v1.2.1 /wiki-init
- **생성**: .wiki/index.md, .wiki/log.md
- **index.md**: 빈 카탈로그로 초기화
