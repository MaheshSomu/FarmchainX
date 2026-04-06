package com.farmchainx.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/qr")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerQrController {

    private static final String QR_BATCH_PREFIX = "farmchainx:batch:";

    private final JdbcTemplate jdbcTemplate;

    public CustomerQrController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String batchId) {
        String normalizedBatchId = normalizeBatchId(batchId);
        if (normalizedBatchId.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "verified", false,
                    "message", "Batch not found"
            ));
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                select b.batch_code as batchCode, b.crop_name as cropName, b.seed_type as seedType, b.location,
                       b.status as batchStatus, br.trace_hash as traceHash, br.timestamp as blockchainTimestamp,
                       br.verified, fp.farm_name as farmName
                from batches b
                left join blockchain_records br on br.batch_id = b.id
                left join farmer_profiles fp on b.farmer_id = fp.id
                where lower(b.batch_code) = lower(?)
                order by br.timestamp desc
                limit 1
                """,
                normalizedBatchId
        );

        if (rows.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "verified", false,
                    "message", "Batch not found"
            ));
        }

        Map<String, Object> result = rows.get(0);
        result.put("verified", Boolean.TRUE.equals(result.get("verified")));
        result.put("message", Boolean.TRUE.equals(result.get("verified"))
                ? "Authentic batch verified on blockchain"
                : "Batch found but blockchain verification is pending");
        return ResponseEntity.ok(result);
    }

    private String normalizeBatchId(String batchId) {
        if (batchId == null) {
            return "";
        }
        String normalized = batchId.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.regionMatches(true, 0, QR_BATCH_PREFIX, 0, QR_BATCH_PREFIX.length())) {
            return normalized.substring(QR_BATCH_PREFIX.length()).trim();
        }

        return normalized;
    }
}

