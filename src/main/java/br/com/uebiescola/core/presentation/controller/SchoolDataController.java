package br.com.uebiescola.core.presentation.controller;

import br.com.uebiescola.core.application.service.SchoolDataExportService;
import br.com.uebiescola.core.application.service.SchoolDataImportService;
import br.com.uebiescola.core.domain.exception.ResourceNotFoundException;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schools/{schoolUuid}/data")
@RequiredArgsConstructor
@Slf4j
public class SchoolDataController {

    private final SchoolDataExportService exportService;
    private final SchoolDataImportService importService;
    private final SchoolRepository schoolRepository;

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private Long resolveSchoolId(String idOrUuid) {
        if (idOrUuid == null || idOrUuid.isBlank()) {
            throw new ResourceNotFoundException("Identificador ausente");
        }
        // Try UUID first
        try {
            UUID uuid = UUID.fromString(idOrUuid);
            return schoolRepository.findByUuid(uuid)
                    .map(s -> s.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));
        } catch (IllegalArgumentException ignored) {
            // fallback: numeric Long
        }
        try {
            Long id = Long.parseLong(idOrUuid);
            return schoolRepository.findById(id)
                    .map(s -> s.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Escola não encontrada"));
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException("Identificador inválido: " + idOrUuid);
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<byte[]> exportSchoolData(@PathVariable("schoolUuid") String schoolIdOrUuid,
                                                   @RequestHeader("Authorization") String authToken) {
        Long schoolId = resolveSchoolId(schoolIdOrUuid);
        try {
            byte[] excelBytes = exportService.exportSchoolData(schoolId, authToken);

            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filename = String.format("escola_%d_%s.xlsx", schoolId, date);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                    .contentLength(excelBytes.length)
                    .body(excelBytes);
        } catch (RuntimeException e) {
            log.error("Erro ao exportar dados da escola {}: {}", schoolId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            log.error("Erro de IO ao exportar dados da escola {}: {}", schoolId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('CEO')")
    public ResponseEntity<?> importSchoolData(@PathVariable("schoolUuid") String schoolIdOrUuid,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestHeader("Authorization") String authToken) {
        Long schoolId = resolveSchoolId(schoolIdOrUuid);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arquivo vazio"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato invalido. Envie um arquivo .xlsx"));
        }

        try {
            Map<String, Object> result = importService.importSchoolData(schoolId, file, authToken);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Erro ao importar dados para escola {}: {}", schoolId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao processar arquivo: " + e.getMessage()));
        }
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('CEO', 'ADMIN')")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] templateBytes = exportService.generateTemplate();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"template_importacao.xlsx\"")
                    .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
                    .contentLength(templateBytes.length)
                    .body(templateBytes);
        } catch (IOException e) {
            log.error("Erro ao gerar template: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
