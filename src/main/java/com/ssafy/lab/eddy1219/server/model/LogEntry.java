package com.ssafy.lab.eddy1219.server.model;

import lombok.Data;
import java.util.Map;

/**
 * 중앙 로그 서버에서 수신하는 로그 데이터의 구조를 정의하는 DTO 클래스입니다.
 * Lombok의 @Data 어노테이션을 사용하여 Getter, Setter, toString 등을 자동 생성합니다.
 * (수정됨: requestProcessingTime 제거, ThrowableInfo에 cause 추가)
 */
@Data
public class LogEntry {
    // 기본 로그 정보
    private String timestamp;
    private String level;
    private String logger;
    private String message;
    private String thread;

    // 애플리케이션 정보
    private String applicationName;
    private String environment;
    private String version;
    private String instanceId;

    // 서버 정보
    private String hostName;
    private String ipAddress;
    private String serverPort;

    // 요청 정보 (HTTP 요청인 경우)
    private String requestId;
    private String requestMethod;
    private String requestUri;
    private String requestQuery; // 쿼리 스트링 (있는 경우)
    private String requestClientIp;
    private String requestUserAgent;
    private Integer httpStatus;

    // Spring 관련 정보 (있는 경우)
    private String framework;
    private Map<String, String> springContext;

    // MDC 정보
    private Map<String, String> mdc;

    // 예외 정보
    private ThrowableInfo throwable;

    // 성능 메트릭
    private PerformanceMetrics performanceMetrics;

    /**
     * 예외 정보를 담는 내부 클래스입니다.
     * (수정됨: 원인 예외 정보 필드 추가)
     */
    @Data
    public static class ThrowableInfo {
        private String message;
        private String className;
        private Object[] stackTrace; // 스택 트레이스 (문자열 배열 등으로 변환되어 올 수 있음)
        private CauseInfo cause;      // 원인 예외 정보 (있을 경우)
    }

    /**
     * 원인 예외(Cause)의 간략한 정보를 담는 내부 클래스입니다.
     */
    @Data
    public static class CauseInfo {
        private String message;
        private String className;
        // 원인 예외의 스택 트레이스는 보통 생략
    }

    /**
     * 성능 메트릭 정보를 담는 내부 클래스입니다.
     */
    @Data
    public static class PerformanceMetrics {
        private Long memoryUsage; // MB 단위
        private Long cpuUsage;    // % 단위
        private Long responseTime; // ms 단위
        private Integer activeThreads;
        private Integer totalThreads;
    }
}