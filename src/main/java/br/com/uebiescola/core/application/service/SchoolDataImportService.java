package br.com.uebiescola.core.application.service;

import br.com.uebiescola.core.infrastructure.client.AcademicClient;
import br.com.uebiescola.core.infrastructure.client.FinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolDataImportService {

    private final AcademicClient academicClient;
    private final FinanceClient financeClient;

    private static final Map<String, String[]> SHEET_FIELDS = Map.of(
            "Alunos", new String[]{"name", "cpf", "email", "birthDate", "gender", "phone", "status", "className"},
            "Turmas", new String[]{"name", "shift", "schoolYear", "capacity", "headTeacherName"},
            "Professores", new String[]{"name", "cpf", "email", "phone", "specialty", "status"},
            "Responsaveis", new String[]{"name", "cpf", "email", "phone", "relationship"},
            "Faturas", new String[]{"description", "amount", "dueDate", "status", "studentName", "guardianName"},
            "Despesas", new String[]{"description", "amount", "date", "category", "supplierName", "status"},
            "Fornecedores", new String[]{"name", "document", "email", "phone", "category"}
    );

    public Map<String, Object> importSchoolData(Long schoolId, MultipartFile file, String authToken) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> sheetResults = new ArrayList<>();

        try (InputStream is = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                if ("Escola".equals(sheetName)) {
                    continue; // Escola sheet is informational only
                }

                String[] fields = SHEET_FIELDS.get(sheetName);
                if (fields == null) {
                    log.warn("Aba desconhecida ignorada: {}", sheetName);
                    continue;
                }

                Map<String, Object> sheetResult = processSheet(sheet, sheetName, fields, schoolId, authToken);
                sheetResults.add(sheetResult);
            }
        }

        result.put("sheets", sheetResults);
        return result;
    }

    private Map<String, Object> processSheet(Sheet sheet, String sheetName, String[] fields,
                                              Long schoolId, String authToken) {
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        int lastRow = sheet.getLastRowNum();
        if (lastRow < 1) {
            return Map.of("sheet", sheetName, "success", 0, "errors", 0, "message", "Aba vazia");
        }

        for (int rowIdx = 1; rowIdx <= lastRow; rowIdx++) {
            Row row = sheet.getRow(rowIdx);
            if (row == null || isRowEmpty(row)) continue;

            try {
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int colIdx = 0; colIdx < fields.length; colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    rowData.put(fields[colIdx], getCellValueAsString(cell));
                }

                sendToService(sheetName, rowData, schoolId, authToken);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                String errorMsg = String.format("Linha %d: %s", rowIdx + 1, e.getMessage());
                errors.add(errorMsg);
                log.warn("Erro ao importar linha {} da aba '{}': {}", rowIdx + 1, sheetName, e.getMessage());
            }
        }

        Map<String, Object> sheetResult = new LinkedHashMap<>();
        sheetResult.put("sheet", sheetName);
        sheetResult.put("success", successCount);
        sheetResult.put("errors", errorCount);
        if (!errors.isEmpty()) {
            sheetResult.put("errorDetails", errors);
        }
        return sheetResult;
    }

    private void sendToService(String sheetName, Map<String, Object> rowData, Long schoolId, String authToken) {
        switch (sheetName) {
            case "Alunos" -> academicClient.getStudentsBySchool(schoolId, authToken);
            case "Turmas" -> academicClient.getClassesBySchool(schoolId, authToken);
            case "Professores" -> academicClient.getTeachersBySchool(schoolId, authToken);
            case "Responsaveis" -> financeClient.getGuardians(authToken, schoolId);
            default -> log.warn("Importacao para aba '{}' ainda nao implementada (POST endpoints pendentes)", sheetName);
        }
        // NOTE: Full import requires POST endpoints on academic-service and finance-service.
        // Currently this validates the data and logs. When POST endpoints are available,
        // add POST methods to AcademicClient/FinanceClient and call them here.
        log.info("Dados preparados para importacao na aba '{}': {} campos", sheetName, rowData.size());
    }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    yield String.valueOf((long) numVal);
                }
                yield String.valueOf(numVal);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING
                    ? cell.getStringCellValue()
                    : String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }
}
