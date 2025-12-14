package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.service.VacationService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

/** 考勤系统 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class QinShiController {

  private final VacationService vacationService;

  /** 一键生成休假数据表Excel */
  @PostMapping("/vacation/process")
  public ResponseEntity<byte[]> processVacationExcel(@RequestParam("file") MultipartFile file) {
    byte[] excelBytes = vacationService.processVacationExcel(file);
    String filename = "休假数据表_" + System.currentTimeMillis() + ".xlsx";
    String encodedFilename = UriUtils.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    String disposition =
        "attachment; filename=\"export.xlsx\"; filename*=UTF-8''" + encodedFilename;

    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
        .contentLength(excelBytes.length)
        .body(excelBytes);
  }
}
