---
id: domain_eq-equipment-repair
last_updated: 2026-05-06
source_files: ["src/main/java/com/example/eq/equipment/EqEquipmentRepairController.java", "src/main/java/com/example/eq/equipment/EqEquipmentRepairService.java"]
dependencies: ["concept_base-dto"]
tags: ["#domain", "#eq", "#equipment", "#repair"]
---

# EQ 설비 수리

> **관련**: [[.wiki/index]]

## 개요

설비 수리 이력 조회 및 수리 등록·완료 처리를 담당하는 EQ 도메인 기능. 수리 요청부터 완료까지의 전체 생명주기(PENDING → COMPLETED)를 관리하며, REST API를 통해 설비별 수리 이력 조회, 신규 수리 등록, 수리 완료 처리를 제공한다.

## 화면 구조

```
EqEquipmentRepairController
├── GET  /api/eq/equipment/repair (Query: equipmentId)
├── POST /api/eq/equipment/repair
└── PUT  /api/eq/equipment/repair/{repairId}/complete
```

## 주요 로직

### 수리 이력 조회

설비ID를 기반으로 해당 설비의 수리 이력을 조회합니다. `equipmentId` 미전달 시 전체 수리 이력을 조회합니다.

```java
@GetMapping
public ResponseEntity<?> findRepairRecords(
    @RequestParam(required = false) String equipmentId) {
    return ResponseEntity.ok(repairService.findRepairRecords(equipmentId));
}
```

### 수리 등록

새로운 수리 요청을 등록하고 상태를 PENDING으로 초기화합니다.

```java
@PostMapping
public ResponseEntity<?> createRepairRecord(
    @RequestBody RepairRequest request) {
    return ResponseEntity.ok(repairService.createRepairRecord(request));
}
```

### 수리 완료

지정된 수리 기록의 상태를 PENDING에서 COMPLETED로 업데이트합니다.

```java
@PutMapping("/{repairId}/complete")
public ResponseEntity<?> completeRepair(
    @PathVariable Long repairId) {
    return ResponseEntity.ok(repairService.completeRepair(repairId));
}
```

## API 목록

| 메서드 | 엔드포인트 | 설명 | 파라미터 |
|--------|-----------|------|---------|
| GET | `/api/eq/equipment/repair` | 설비별 수리 이력 조회 (미전달 시 전체 조회) | `equipmentId` (query, optional) |
| POST | `/api/eq/equipment/repair` | 신규 수리 기록 등록 | Request Body (DTO) |
| PUT | `/api/eq/equipment/repair/{repairId}/complete` | 수리 완료 처리 | `repairId` (path) |

## 연결

### 소스 파일
- [[src/main/java/com/example/eq/equipment/EqEquipmentRepairController.java]] — 수리 API 엔드포인트 정의
- [[src/main/java/com/example/eq/equipment/EqEquipmentRepairService.java]] — 수리 비즈니스 로직

### 관련 노트
- [[.wiki/index]] — 전체 시스템 구조 개요
- [[.wiki/concept/base-dto]] — 요청/응답 DTO 기본 구조 상속
- [[.wiki/pattern/base-dto-inheritance]] — 수리 요청 DTO의 BaseDTO 상속 구조
