package com.example.lms.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<HealthStatus>> health() {
        String databaseStatus = databaseStatus();
        String overallStatus = "UP".equals(databaseStatus) ? "UP" : "DOWN";
        HealthStatus status = new HealthStatus(
                overallStatus,
                Instant.now(),
                Map.of("database", databaseStatus));
        return ResponseEntity.status(
                        "UP".equals(overallStatus) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(status));
    }

    private String databaseStatus() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (SQLException exception) {
            return "DOWN";
        }
    }
}
