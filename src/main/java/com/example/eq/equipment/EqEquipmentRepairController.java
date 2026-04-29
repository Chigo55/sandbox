package com.example.eq.equipment;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/eq/equipment/repair")
public class EqEquipmentRepairController {

    private final EqEquipmentRepairService repairService;

    public EqEquipmentRepairController(EqEquipmentRepairService repairService) {
        this.repairService = repairService;
    }

    @GetMapping
    public List<RepairRecord> getRepairList(@RequestParam(required = false) String equipmentId) {
        return repairService.findRepairRecords(equipmentId);
    }

    @PostMapping
    public RepairRecord createRepair(@RequestBody RepairRequest request) {
        return repairService.createRepairRecord(request);
    }

    @PutMapping("/{repairId}/complete")
    public RepairRecord completeRepair(@PathVariable Long repairId) {
        return repairService.completeRepair(repairId);
    }
}
