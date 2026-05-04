---
id: concept_base-dto
last_updated: 2026-05-04
source_files: ["src/main/java/com/example/common/BaseDTO.java"]
dependencies: []
tags: ["#concept"]
---

# BaseDTO

> **관련**: [[.wiki/index]]

## 정의

BaseDTO는 프로젝트 내 모든 DTO(Data Transfer Object)의 공통 응답 필드를 정의하는 최상위 베이스 클래스입니다. 에러 처리 관련 필드인 `errorCode`와 `errorMsg`를 표준화하여 API 응답 일관성을 보장합니다.

## 상세

### 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| errorCode | String | 에러 코드 |
| errorMsg | String | 에러 메시지 |

### 메서드

- `getErrorCode()` — errorCode 조회
- `setErrorCode(String)` — errorCode 설정
- `getErrorMsg()` — errorMsg 조회
- `setErrorMsg(String)` — errorMsg 설정

### 코드 예시

```java
package com.example.common;

public class BaseDTO {
    private String errorCode;
    private String errorMsg;
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMsg() {
        return errorMsg;
    }
    
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
```

## 연결

- [[.wiki/index]] — 프로젝트 핵심 개념 목록
- [[.wiki/domain/eq-equipment-repair]] — BaseDTO를 상속받는 도메인 DTO 클래스
