package com.ssafy.lab.eddy1219.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lab.eddy1219.server.model.LogEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map; // Map 임포트 추가

/**
 * 중앙 로그 서버의 로그 수신 엔드포인트를 담당하는 컨트롤러입니다.
 * POST /api/logs 로 로그 배치를 받아 콘솔에 형식화하여 출력합니다.
 * (수정됨: 모든 LogEntry 필드 출력하도록 보강)
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    /**
     * JSON 변환을 위한 ObjectMapper (Spring Boot가 자동으로 주입)
     */
    private final ObjectMapper objectMapper;

    /**
     * 생성자를 통해 ObjectMapper를 주입받습니다.
     *
     * @param objectMapper Spring 컨텍스트에 등록된 ObjectMapper 빈
     */
    public LogController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 로그 배치를 수신하여 콘솔에 상세 정보를 출력합니다.
     *
     * @param logEntries 로그 엔트리 객체 리스트
     * @return 처리 결과 (성공 시 200 OK)
     */
    @PostMapping
    public ResponseEntity<Void> receiveLogBatch(@RequestBody List<LogEntry> logEntries) {

        if (logEntries == null || logEntries.isEmpty()) {
            System.out.println("Received empty log batch.");
            return ResponseEntity.ok().build(); // 빈 배치는 정상 처리
        }

        System.out.println("Received log batch with " + logEntries.size() + " entries.");
        try {
            // ObjectMapper를 사용하여 List<LogEntry>를 보기 좋게 포맷팅된 JSON 문자열로 변환
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(logEntries);
            System.out.println("\n--- Received Log Batch (Formatted JSON) ---");
            System.out.println(jsonOutput); // 변환된 JSON 출력
            System.out.println("-------------------------------------------\n");
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시 에러 메시지 출력
            System.err.println("Error converting received log batch to JSON: " + e.getMessage());
            System.out.println("Raw data (toString): " + logEntries);
        }

        // --- List를 순회하며 각 로그 상세 정보 출력 ---
        for (LogEntry logEntry : logEntries) {
            if (logEntry == null) continue; // 혹시 모를 null 요소 방지

            // 로그 레벨에 따른 색상 설정
            String color = getColorForLevel(logEntry.getLevel());
            String reset = "\u001B[0m";

            // 구분선 및 헤더 출력
            printSeparator(color, reset);
            System.out.printf("%s%s%s\n", color, centerText("LOG ENTRY", 100), reset);
            printSeparator(color, reset);

            // --- 각 섹션별 정보 출력 ---
            printSection("Basic Info", color, reset);
            printField("Timestamp", logEntry.getTimestamp(), color, reset);
            printField("Level", logEntry.getLevel(), color, reset);
            printField("Logger", logEntry.getLogger(), color, reset);
            printField("Message", logEntry.getMessage(), color, reset);
            printField("Thread", logEntry.getThread(), color, reset);

            printSection("Application Info", color, reset);
            printField("  Name", logEntry.getApplicationName(), color, reset);
            printField("  Environment", logEntry.getEnvironment(), color, reset);
            printField("  Version", logEntry.getVersion(), color, reset);
            printField("  Instance ID", logEntry.getInstanceId(), color, reset);

            printSection("Server Info", color, reset);
            printField("  Host Name", logEntry.getHostName(), color, reset);
            printField("  IP Address", logEntry.getIpAddress(), color, reset);
            printField("  Server Port", logEntry.getServerPort(), color, reset);

            // HTTP 요청 정보 (있는 경우)
            if (logEntry.getRequestId() != null || logEntry.getRequestMethod() != null) {
                printSection("HTTP Request Info", color, reset);
                printField("  Request ID", logEntry.getRequestId(), color, reset);
                printField("  Method", logEntry.getRequestMethod(), color, reset);
                printField("  URI", logEntry.getRequestUri(), color, reset);
                printField("  Query", logEntry.getRequestQuery(), color, reset);
                printField("  Client IP", logEntry.getRequestClientIp(), color, reset);
                printField("  User Agent", logEntry.getRequestUserAgent(), color, reset);
                printField("  Status Code", logEntry.getHttpStatus(), color, reset);
            }

            // Spring 관련 정보 (있는 경우)
            if (logEntry.getFramework() != null || (logEntry.getSpringContext() != null && !logEntry.getSpringContext().isEmpty())) {
                printSection("Framework Info", color, reset);
                printField("  Framework", logEntry.getFramework(), color, reset);
                if (logEntry.getSpringContext() != null && !logEntry.getSpringContext().isEmpty()) {
                    System.out.printf("%s%s%-18s%s\n", color, "  ", "Spring Context:", reset);
                    logEntry.getSpringContext().forEach((key, value) ->
                            System.out.printf("%s%s  %-18s%s%s\n", color, "  ", key + ":", reset, value));
                }
            }

            // MDC 정보 (있는 경우)
            if (logEntry.getMdc() != null && !logEntry.getMdc().isEmpty()) {
                printSection("MDC Info", color, reset);
                logEntry.getMdc().forEach((key, value) ->
                        printField("  " + key, value, color, reset)); // 들여쓰기 및 키 직접 사용
            }

            // 성능 메트릭 (있는 경우)
            if (logEntry.getPerformanceMetrics() != null) {
                printSection("Performance Metrics", color, reset);
                LogEntry.PerformanceMetrics metrics = logEntry.getPerformanceMetrics();
                printField("  Memory Usage", metrics.getMemoryUsage() != null ? metrics.getMemoryUsage() + " MB" : "N/A", color, reset);
                printField("  CPU Usage", metrics.getCpuUsage() != null ? metrics.getCpuUsage() + "%" : "N/A", color, reset);
                Long responseTime = metrics.getResponseTime();
                printField("  Response Time", (responseTime != null && responseTime >= 0) ? responseTime + "ms" : "N/A", color, reset);
                printField("  Active Threads", (metrics.getActiveThreads() != null && metrics.getTotalThreads() != null)
                        ? metrics.getActiveThreads() + "/" + metrics.getTotalThreads() : "N/A", color, reset);
            }

            // 예외 정보 (있는 경우)
            if (logEntry.getThrowable() != null) {
                printSection("Exception Info", color, reset);
                LogEntry.ThrowableInfo throwable = logEntry.getThrowable();
                printField("  Class", throwable.getClassName(), color, reset);
                printField("  Message", throwable.getMessage(), color, reset);

                // 원인 예외 정보 출력 (있는 경우)
                if (throwable.getCause() != null) {
                    System.out.printf("%s%s%-18s%s\n", color, "  ", "Cause:", reset);
                    printField("    Class", throwable.getCause().getClassName(), color, reset);
                    printField("    Message", throwable.getCause().getMessage(), color, reset);
                }

                // 스택 트레이스 출력 (있는 경우)
                if (throwable.getStackTrace() != null && throwable.getStackTrace().length > 0) {
                    System.out.printf("%s%s%-18s%s\n", color, "  ", "Stack Trace:", reset);
                    for (Object stackTraceElement : throwable.getStackTrace()) {
                        // 스택 트레이스 각 라인 출력 (들여쓰기 추가)
                        System.out.printf("%s%s%s%s\n", color, "    ", stackTraceElement, reset);
                    }
                }
            }

            // 로그 항목 마무리
            printSeparator(color, reset);
            System.out.println(); // 로그 항목 사이에 빈 줄 추가

        } // for loop 끝
        // -----------------------------------

        return ResponseEntity.ok().build();
    }

    /**
     * 로그 레벨에 따른 ANSI 색상 코드를 반환합니다.
     *
     * @param level 로그 레벨 문자열
     * @return ANSI 색상 코드
     */
    private String getColorForLevel(String level) {
        if (level == null) return "\u001B[0m"; // 기본색
        return switch (level.toUpperCase()) {
            case "ERROR" -> "\u001B[31m"; // 빨간색
            case "WARN" -> "\u001B[33m";  // 노란색
            case "INFO" -> "\u001B[32m";  // 초록색
            case "DEBUG" -> "\u001B[36m"; // 청록색
            case "TRACE" -> "\u001B[37m"; // 흰색
            default -> "\u001B[0m";       // 기본색
        };
    }

    /**
     * 구분선을 출력하는 헬퍼 메소드
     */
    private void printSeparator(String color, String reset) {
        System.out.printf("%s%s%s\n", color, "=".repeat(100), reset);
    }

    /**
     * 섹션 제목을 출력하는 헬퍼 메소드
     */
    private void printSection(String title, String color, String reset) {
        System.out.printf("%s%-20s%s\n", color, title + ":", reset);
    }

    /**
     * 필드 이름과 값을 형식에 맞게 출력하는 헬퍼 메소드 (null 처리 포함)
     */
    private void printField(String fieldName, Object value, String color, String reset) {
        // 값 포맷팅 (null일 경우 "N/A" 또는 빈 문자열)
        String formattedValue = (value != null) ? value.toString() : "N/A";
        // 필드 이름이 들여쓰기 포함 여부에 따라 포맷 결정
        if (fieldName.startsWith("  ")) { // 들여쓰기가 있는 경우
            System.out.printf("%s%s%-18s%s%s\n", color, fieldName.substring(0, 2), fieldName.substring(2).trim() + ":", reset, formattedValue);
        } else { // 들여쓰기 없는 경우
            System.out.printf("%s%-20s%s%s\n", color, fieldName + ":", reset, formattedValue);
        }
    }

    /**
     * 텍스트를 가운데 정렬하는 헬퍼 메소드
     */
    private String centerText(String text, int width) {
        if (text == null) text = "";
        int padding = width - text.length();
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
}