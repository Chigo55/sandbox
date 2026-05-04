package com.example.eq.equipment;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EqEquipmentRepairService {

    private final EqEquipmentRepairRepository repairRepository;

    public EqEquipmentRepairService(EqEquipmentRepairRepository repairRepository) {
        this.repairRepository = repairRepository;
    }

    public List<RepairRecord> findRepairRecords(String equipmentId) {
        if (equipmentId != null) {
            return repairRepository.findByEquipmentId(equipmentId);
        }
        return repairRepository.findAll();
    }

    public RepairRecord createRepairRecord(RepairRequest request) {
        RepairRecord record = new RepairRecord();
        record.setEquipmentId(request.getEquipmentId());
        record.setDescription(request.getDescription());
        record.setStatus("PENDING");
        return repairRepository.save(record);
    }

    public RepairRecord completeRepair(Long repairId) {
        RepairRecord record = repairRepository.findById(repairId)
            .orElseThrow(() -> new RuntimeException("Repair record not found: " + repairId));
        record.setStatus("COMPLETED");
        return repairRepository.save(record);
    }
}
  // v1.2.3 test comment
