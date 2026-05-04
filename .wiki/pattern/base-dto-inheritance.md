---
id: pattern_base-dto-inheritance
last_updated: 2026-05-04
source_files: ["src/main/java/com/example/common/BaseDTO.java", "src/main/java/com/example/inv/onhands/InvOnhandsController.java"]
dependencies: ["concept_base-dto"]
tags: ["#pattern"]
---

# BaseDTO 상속 패턴

> **관련**: [[.wiki/index]]

## 문제

모든 API 응답 DTO가 에러 처리 필드(`errorCode`, `errorMsg`)를 개별적으로 선언하면 코드 중복이 발생하고, 필드명·타입 변경 시 모든 DTO를 수정해야 하므로 일관성 훼손 위험이 높다.

## 해법

각 도메인 DTO가 `com.example.common.BaseDTO`를 extends하면 공통 응답 필드와 getter/setter를 자동으로 상속받는다. 이를 통해 중복을 제거하고 에러 처리를 표준화할 수 있다.

### 기본 구조

```java
// 공통 응답 DTO - BaseDTO
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

### 도메인별 DTO 예시

```java
// 재고 현황 응답 DTO - InvOnhandsDTO
package com.example.inv.onhands;

import com.example.common.BaseDTO;

public class InvOnhandsDTO extends BaseDTO {
    private String itemCode;
    private int quantity;
    private String warehouseCode;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }
}
```

### 컨트롤러에서의 사용

```java
// InvOnhandsController
package com.example.inv.onhands;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvOnhandsController {
    
    @GetMapping("/api/inv/onhands")
    public InvOnhandsDTO getOnhands() {
        InvOnhandsDTO response = new InvOnhandsDTO();
        response.setItemCode("ITEM001");
        response.setQuantity(100);
        response.setWarehouseCode("WH01");
        
        // BaseDTO에서 상속받은 필드
        response.setErrorCode("0000");
        response.setErrorMsg("SUCCESS");
        
        return response;
    }
}
```

## 연결

- [[.wiki/index]] — 모든 노트의 진입점
- [[.wiki/concept/base-dto]] — BaseDTO의 개념 및 설계 의도
