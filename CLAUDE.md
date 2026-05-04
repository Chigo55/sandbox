# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# userEmail
The user's email address is jungih456@gmail.com.
# currentDate
Today's date is 2026-05-04.

## 프로젝트 개요

Spring Boot 기반의 ERP 스타일 백엔드 애플리케이션. 설비(EQ) 및 재고(INV) 도메인을 다룬다.

## 패키지 구조

```
com.example
├── common/         # BaseDTO 등 공통 클래스
├── eq/
│   └── equipment/  # 설비 수리 (EqEquipmentRepair*)
└── inv/
    ├── inventory/  # 재고 항목 (InvInventory*)
    └── onhands/    # 재고 현황 (InvOnhands*)
```

도메인별 패키지 명명 규칙: `com.example.{domain}.{subdomain}`, 파일명 접두사는 `{Domain}{Subdomain}` (예: `EqEquipmentRepair`, `InvOnhands`).

## 아키텍처 패턴

**Controller → Service → Repository** 3계층 구조를 따른다.

**BaseDTO 상속 패턴**: 모든 응답 DTO는 `com.example.common.BaseDTO`를 extends해야 한다. `BaseDTO`는 `errorCode`와 `errorMsg` 필드를 제공하여 API 응답 일관성을 보장한다.

```java
public class SomeDomainDTO extends BaseDTO {
    // 도메인별 필드 추가
}
```

**두 가지 컨트롤러 스타일**:
- REST API: `@RestController` + `/api/{domain}/{subdomain}` 경로 (JSON 반환)
- MVC: `@RestController` + `/{domain}/{subdomain}` 경로 (`ModelAndView` 반환, 뷰 템플릿 사용)

## 수리 상태 생명주기

설비 수리 레코드의 상태는 `PENDING` → `COMPLETED` 순서로 전이된다. Service 계층에서 상태를 직접 문자열로 관리한다.

## Wiki

`.wiki/` 디렉토리에 도메인 지식, 개념, 패턴 문서가 있다. 새 기능 작성 시 관련 wiki 노트를 참고하고, 새 패턴이나 도메인 로직이 추가되면 wiki에도 반영한다.
