package br.com.uebiescola.core.application.service;

import br.com.uebiescola.core.domain.model.School;
import br.com.uebiescola.core.domain.repository.SchoolRepository;
import br.com.uebiescola.core.infrastructure.client.AcademicClient;
import br.com.uebiescola.core.infrastructure.client.FinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolDataExportService {

    private final SchoolRepository schoolRepository;
    private final AcademicClient academicClient;
    private final FinanceClient financeClient;

    private static final String[] STUDENT_HEADERS = {"Nome", "CPF", "Email", "Nascimento", "Genero", "Telefone", "Status", "Turma"};
    private static final String[] CLASS_HEADERS = {"Nome", "Turno", "Ano Letivo", "Capacidade", "Professor Titular"};
    private static final String[] TEACHER_HEADERS = {"Nome", "CPF", "Email", "Telefone", "Especialidade", "Status"};
    private static final String[] GUARDIAN_HEADERS = {"Nome", "CPF", "Email", "Telefone", "Parentesco"};
    private static final String[] INVOICE_HEADERS = {"Descricao", "Valor", "Vencimento", "Status", "Aluno", "Responsavel"};
    private static final String[] EXPENSE_HEADERS = {"Descricao", "Valor", "Data", "Categoria", "Fornecedor", "Status"};
    private static final String[] SUPPLIER_HEADERS = {"Nome", "CNPJ/CPF", "Email", "Telefone", "Categoria"};

    private static final String[] STUDENT_FIELDS = {"name", "cpf", "email", "birthDate", "gender", "phone", "status", "className"};
    private static final String[] CLASS_FIELDS = {"name", "shift", "schoolYear", "capacity", "headTeacherName"};
    private static final String[] TEACHER_FIELDS = {"name", "cpf", "email", "phone", "specialty", "status"};
    private static final String[] GUARDIAN_FIELDS = {"name", "cpf", "email", "phone", "relationship"};
    private static final String[] INVOICE_FIELDS = {"description", "amount", "dueDate", "status", "studentName", "guardianName"};
    private static final String[] EXPENSE_FIELDS = {"description", "amount", "date", "category", "supplierName", "status"};
    private static final String[] SUPPLIER_FIELDS = {"name", "document", "email", "phone", "category"};

    public byte[] exportSchoolData(Long schoolId, String authToken) throws IOException {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("Escola nao encontrada com id: " + schoolId));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Sheet: Escola
            createSchoolSheet(workbook, headerStyle, school);

            // Sheet: Alunos
            List<Map<String, Object>> students = fetchSafely(() -> academicClient.getStudentsBySchool(schoolId, authToken), "Alunos");
            if (students != null) {
                createDataSheet(workbook, headerStyle, "Alunos", STUDENT_HEADERS, STUDENT_FIELDS, students);
            }

            // Sheet: Turmas
            List<Map<String, Object>> classes = fetchSafely(() -> academicClient.getClassesBySchool(schoolId, authToken), "Turmas");
            if (classes != null) {
                createDataSheet(workbook, headerStyle, "Turmas", CLASS_HEADERS, CLASS_FIELDS, classes);
            }

            // Sheet: Professores
            List<Map<String, Object>> teachers = fetchSafely(() -> academicClient.getTeachersBySchool(schoolId, authToken), "Professores");
            if (teachers != null) {
                createDataSheet(workbook, headerStyle, "Professores", TEACHER_HEADERS, TEACHER_FIELDS, teachers);
            }

            // Sheet: Responsaveis
            List<Map<String, Object>> guardians = fetchSafely(() -> financeClient.getGuardians(authToken, schoolId), "Responsaveis");
            if (guardians != null) {
                createDataSheet(workbook, headerStyle, "Responsaveis", GUARDIAN_HEADERS, GUARDIAN_FIELDS, guardians);
            }

            // Sheet: Faturas
            List<Map<String, Object>> invoices = fetchSafely(() -> financeClient.getInvoices(authToken, schoolId), "Faturas");
            if (invoices != null) {
                createDataSheet(workbook, headerStyle, "Faturas", INVOICE_HEADERS, INVOICE_FIELDS, invoices);
            }

            // Sheet: Despesas
            List<Map<String, Object>> expenses = fetchSafely(() -> financeClient.getExpenses(authToken, schoolId), "Despesas");
            if (expenses != null) {
                createDataSheet(workbook, headerStyle, "Despesas", EXPENSE_HEADERS, EXPENSE_FIELDS, expenses);
            }

            // Sheet: Fornecedores
            List<Map<String, Object>> suppliers = fetchSafely(() -> financeClient.getSuppliers(authToken, schoolId), "Fornecedores");
            if (suppliers != null) {
                createDataSheet(workbook, headerStyle, "Fornecedores", SUPPLIER_HEADERS, SUPPLIER_FIELDS, suppliers);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateTemplate() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);

            createHeaderOnlySheet(workbook, headerStyle, "Alunos", STUDENT_HEADERS);
            createHeaderOnlySheet(workbook, headerStyle, "Turmas", CLASS_HEADERS);
            createHeaderOnlySheet(workbook, headerStyle, "Professores", TEACHER_HEADERS);
            createHeaderOnlySheet(workbook, headerStyle, "Responsaveis", GUARDIAN_HEADERS);
            createHeaderOnlySheet(workbook, headerStyle, "Faturas", INVOICE_HEADERS);
            createHeaderOnlySheet(workbook, headerStyle, "Despesas", EXPENSE_HEADERS);
            createHeaderOnlySheet(workbook, headerStyle, "Fornecedores", SUPPLIER_HEADERS);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createSchoolSheet(Workbook workbook, CellStyle headerStyle, School school) {
        Sheet sheet = workbook.createSheet("Escola");

        String[][] schoolData = {
                {"Nome", school.getName()},
                {"Razao Social", school.getLegalName()},
                {"CNPJ", school.getCnpj()},
                {"Inscricao Estadual", school.getStateRegistration()},
                {"Subdominio", school.getSubdomain()},
                {"Cor Primaria", school.getPrimaryColor()},
                {"Chave PIX", school.getPixKey()},
                {"Ativa", school.getActive() != null ? school.getActive().toString() : ""}
        };

        for (int i = 0; i < schoolData.length; i++) {
            Row row = sheet.createRow(i);
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(schoolData[i][0]);
            labelCell.setCellStyle(headerStyle);
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(schoolData[i][1] != null ? schoolData[i][1] : "");
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createDataSheet(Workbook workbook, CellStyle headerStyle, String sheetName,
                                 String[] headers, String[] fields, List<Map<String, Object>> data) {
        Sheet sheet = workbook.createSheet(sheetName);

        // Header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (int rowIdx = 0; rowIdx < data.size(); rowIdx++) {
            Row row = sheet.createRow(rowIdx + 1);
            Map<String, Object> record = data.get(rowIdx);
            for (int colIdx = 0; colIdx < fields.length; colIdx++) {
                Cell cell = row.createCell(colIdx);
                Object value = record.get(fields[colIdx]);
                cell.setCellValue(value != null ? value.toString() : "");
            }
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createHeaderOnlySheet(Workbook workbook, CellStyle headerStyle, String sheetName, String[] headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private List<Map<String, Object>> fetchSafely(FeignCall call, String sheetName) {
        try {
            return call.execute();
        } catch (Exception e) {
            log.warn("Nao foi possivel obter dados para a aba '{}': {}", sheetName, e.getMessage());
            return null;
        }
    }

    @FunctionalInterface
    private interface FeignCall {
        List<Map<String, Object>> execute();
    }
}
