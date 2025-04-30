package com.ssafy.lab.eddy1219.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.lab.eddy1219.server.model.LogEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 중앙 로그 서버의 로그 수신 및 조회를 담당하는 컨트롤러입니다.
 * POST /api/logs 로 로그 배치를 받아 저장하고 콘솔에 출력합니다.
 * GET /api/logs 로 최근 수신된 로그 목록을 조회합니다.
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final ObjectMapper objectMapper;

    // 최근 로그를 저장할 메모리 내 큐 (동시성 지원)
    private static final int MAX_LOG_ENTRIES = 100; // 메모리에 보관할 최대 로그 수
    private final Queue<LogEntry> recentLogs = new ConcurrentLinkedQueue<>();

    /**
     * 생성자를 통해 ObjectMapper를 주입받습니다.
     *
     * @param objectMapper Spring 컨텍스트에 등록된 ObjectMapper 빈
     */
    public LogController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 로그 배치를 수신하여 저장하고 콘솔에 상세 정보를 출력합니다.
     * (POST /api/logs)
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

        // 새 로그 추가 및 오래된 로그 제거 (큐 크기 제한)
        for (LogEntry entry : logEntries) {
            if (entry != null) { // 리스트 내 null 요소 방지
                // 큐가 꽉 찼으면 가장 오래된 것 제거
                while (recentLogs.size() >= MAX_LOG_ENTRIES) {
                    recentLogs.poll();
                }
                // 새 로그 추가
                recentLogs.offer(entry);
            }
        }

        // --- 콘솔 출력 로직 ---
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

        // --- List를 순회하며 각 로그 상세 정보 콘솔 출력 ---
        for (LogEntry logEntry : logEntries) {
            if (logEntry == null) continue; // 혹시 모를 null 요소 방지

            String color = getColorForLevel(logEntry.getLevel());
            String reset = "\u001B[0m";

            printSeparator(color, reset);
            System.out.printf("%s%s%s\n", color, centerText("LOG ENTRY", 100), reset);
            printSeparator(color, reset);

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

                if (throwable.getCause() != null) {
                    System.out.printf("%s%s%-18s%s\n", color, "  ", "Cause:", reset);
                    printField("    Class", throwable.getCause().getClassName(), color, reset);
                    printField("    Message", throwable.getCause().getMessage(), color, reset);
                }

                if (throwable.getStackTrace() != null && throwable.getStackTrace().length > 0) {
                    System.out.printf("%s%s%-18s%s\n", color, "  ", "Stack Trace:", reset);
                    for (Object stackTraceElement : throwable.getStackTrace()) {
                        System.out.printf("%s%s%s%s\n", color, "    ", stackTraceElement, reset);
                    }
                }
            }

            printSeparator(color, reset);
            System.out.println(); // 로그 항목 사이에 빈 줄 추가

        } // for loop 끝
        // -----------------------------------

        return ResponseEntity.ok().build();
    }

    /**
     * 최근 수신된 로그 목록을 조회합니다.
     * (GET /api/logs)
     *
     * @return 최근 로그 엔트리 리스트 (JSON 형식)
     */
    @GetMapping
    public ResponseEntity<List<LogEntry>> getRecentLogs() {
        // 현재 큐에 있는 로그를 리스트로 복사하여 반환 (순서는 들어온 순서)
        List<LogEntry> logsToReturn = new LinkedList<>(recentLogs);
        return ResponseEntity.ok(logsToReturn);
    }

    // --- Helper Methods ---

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

    private void printSeparator(String color, String reset) {
        System.out.printf("%s%s%s\n", color, "=".repeat(100), reset);
    }

    private void printSection(String title, String color, String reset) {
        System.out.printf("%s%-20s%s\n", color, title + ":", reset);
    }

    private void printField(String fieldName, Object value, String color, String reset) {
        String formattedValue = (value != null) ? value.toString() : "N/A";
        if (fieldName.startsWith("  ")) {
            System.out.printf("%s%s%-18s%s%s\n", color, fieldName.substring(0, 2), fieldName.substring(2).trim() + ":", reset, formattedValue);
        } else {
            System.out.printf("%s%-20s%s%s\n", color, fieldName + ":", reset, formattedValue);
        }
    }

    private String centerText(String text, int width) {
        if (text == null) text = "";
        int padding = width - text.length();
        if (padding < 0) padding = 0; // 텍스트가 너비보다 길 경우
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
}