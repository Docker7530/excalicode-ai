package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.vacation.VacationCorrectRequest;
import com.excalicode.platform.core.api.vacation.VacationDetailRequest;
import com.excalicode.platform.core.api.vacation.VacationRecordRequest;
import com.excalicode.platform.core.api.vacation.VacationSplitResponse;
import com.excalicode.platform.core.service.VacationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 考勤系统控制器 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class QinShiController {

  private final VacationService vacationService;

  /** 解析导出的请假 Excel 文件，拆分为单条请假记录 */
  @PostMapping("/vacation/split")
  public ResponseEntity<VacationSplitResponse> splitVacationRecords(
      @RequestParam("file") MultipartFile file) {
    VacationSplitResponse result = vacationService.parseVacationExcel(file);
    return ResponseEntity.ok(result);
  }

  /** 批量纠正请假记录的备注信息 */
  @PostMapping("/vacation/correct")
  public ResponseEntity<List<VacationRecordRequest>> correctVacationRemarks(
      @RequestBody VacationCorrectRequest request) {
    List<VacationRecordRequest> correctedRecords =
        vacationService.correctRemarks(request.getRecords());
    return ResponseEntity.ok(correctedRecords);
  }

  /** 生成请假明细表 */
  @PostMapping("/vacation/generate-table")
  public ResponseEntity<List<VacationDetailRequest>> generateVacationDetailTable(
      @RequestBody VacationCorrectRequest request) {
    List<VacationDetailRequest> detailRecords =
        vacationService.generateVacationDetailTable(request.getRecords());
    return ResponseEntity.ok(detailRecords);
  }
}
