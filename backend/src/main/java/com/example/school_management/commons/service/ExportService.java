package com.example.school_management.commons.service;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.Staff;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExportService {
    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export users to CSV format
     */
    public String exportUsersToCSV(List<? extends BaseUser> users, String userType) throws IOException {
        log.debug("Exporting {} {} users to CSV", users.size(), userType);
        
        StringWriter out = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(getUserHeaders(userType))
                .build();
        
        try (CSVPrinter printer = new CSVPrinter(out, format)) {
            for (BaseUser user : users) {
                printer.printRecord(getUserRowData(user, userType));
            }
        }
        
        log.info("Successfully exported {} {} users to CSV", users.size(), userType);
        return out.toString();
    }

    /**
     * Export users to Excel format
     */
    public byte[] exportUsersToExcel(List<? extends BaseUser> users, String userType) throws IOException {
        log.debug("Exporting {} {} users to Excel", users.size(), userType);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(userType.substring(0, 1).toUpperCase() + userType.substring(1));
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = getUserHeaders(userType);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Create data rows
            int rowNum = 1;
            for (BaseUser user : users) {
                Row row = sheet.createRow(rowNum++);
                Object[] rowData = getUserRowData(user, userType);
                for (int i = 0; i < rowData.length; i++) {
                    Cell cell = row.createCell(i);
                    if (rowData[i] != null) {
                        cell.setCellValue(rowData[i].toString());
                    }
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            
            log.info("Successfully exported {} {} users to Excel", users.size(), userType);
            return out.toByteArray();
        }
    }

    /**
     * Get headers based on user type
     */
    private String[] getUserHeaders(String userType) {
        String[] baseHeaders = {
            "ID", "First Name", "Last Name", "Email", "Telephone", "Status", 
            "Created At", "Updated At"
        };
        
        return switch (userType.toLowerCase()) {
            case "teachers" -> concatArrays(baseHeaders, new String[]{
                "Qualifications", "Subjects Taught", "Weekly Capacity", "Schedule Preferences"
            });
            case "students" -> concatArrays(baseHeaders, new String[]{
                "Grade Level", "Enrolled At", "Birthday", "Gender", "Address"
            });
            case "staff" -> concatArrays(baseHeaders, new String[]{
                "Staff Type", "Department", "Birthday", "Gender", "Address"
            });
            default -> baseHeaders;
        };
    }

    /**
     * Get row data based on user type
     */
    private Object[] getUserRowData(BaseUser user, String userType) {
        Object[] baseData = {
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getTelephone(),
            user.getStatus(),
            user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : "",
            user.getUpdatedAt() != null ? user.getUpdatedAt().format(DATE_FORMATTER) : ""
        };
        
        return switch (userType.toLowerCase()) {
            case "teachers" -> {
                Teacher teacher = (Teacher) user;
                yield concatArrays(baseData, new Object[]{
                    teacher.getQualifications(),
                    teacher.getSubjectsTaught(),
                    teacher.getWeeklyCapacity(),
                    teacher.getSchedulePreferences()
                });
            }
            case "students" -> {
                Student student = (Student) user;
                yield concatArrays(baseData, new Object[]{
                    student.getGradeLevel(),
                    student.getEnrolledAt() != null ? student.getEnrolledAt().format(DATE_FORMATTER) : "",
                    student.getBirthday() != null ? student.getBirthday().format(DATE_FORMATTER) : "",
                    student.getGender(),
                    student.getAddress()
                });
            }
            case "staff" -> {
                Staff staff = (Staff) user;
                yield concatArrays(baseData, new Object[]{
                    staff.getStaffType(),
                    staff.getDepartment(),
                    staff.getBirthday() != null ? staff.getBirthday().format(DATE_FORMATTER) : "",
                    staff.getGender(),
                    staff.getAddress()
                });
            }
            default -> baseData;
        };
    }

    /**
     * Utility method to concatenate arrays
     */
    private String[] concatArrays(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Utility method to concatenate arrays
     */
    private Object[] concatArrays(Object[] first, Object[] second) {
        Object[] result = new Object[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Generate filename for export
     */
    public String generateExportFilename(String userType, String format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_export_%s.%s", userType, timestamp, format.toLowerCase());
    }
}
