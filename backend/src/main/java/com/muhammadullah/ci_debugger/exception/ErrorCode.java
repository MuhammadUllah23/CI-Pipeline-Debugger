package com.muhammadullah.ci_debugger.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ── Webhook / Ingestion ──────────────────────────────────────────────────
    WEBHOOK_SIGNATURE_INVALID("HMAC signature mismatch for provider", HttpStatus.UNAUTHORIZED),
    WEBHOOK_PAYLOAD_MISSING_FIELD("Required field missing in webhook payload", HttpStatus.UNPROCESSABLE_ENTITY),
    WEBHOOK_EVENT_TYPE_UNSUPPORTED("Event type is not handled for this provider", HttpStatus.BAD_REQUEST),
    WEBHOOK_DUPLICATE_EVENT("This event has already been processed", HttpStatus.CONFLICT),

    // ── Provider ─────────────────────────────────────────────────────────────
    PROVIDER_NOT_SUPPORTED("Provider is not registered", HttpStatus.BAD_REQUEST),
    PROVIDER_MAPPING_FAILED("Failed to map provider payload field", HttpStatus.UNPROCESSABLE_ENTITY),
    PROVIDER_STATUS_UNKNOWN("Cannot map unknown status from provider", HttpStatus.UNPROCESSABLE_ENTITY),
    PROVIDER_API_CLIENT_ERROR("Invalid request to provider API", HttpStatus.BAD_REQUEST),
    PROVIDER_API_UNAVAILABLE("Provider API is unavailable or timed out", HttpStatus.SERVICE_UNAVAILABLE),
    
    // ── Pipeline runs ────────────────────────────────────────────────────────
    PIPELINE_RUN_NOT_FOUND("Pipeline run not found", HttpStatus.NOT_FOUND),
    PIPELINE_STEP_NOT_FOUND("Pipeline step not found", HttpStatus.NOT_FOUND),

    // ── ErrorCluster ────────────────────────────────────────────────────────
    ERROR_CLUSTER_NOT_FOUND("Error cluster not found", HttpStatus.NOT_FOUND),

    // ── Storage ──────────────────────────────────────────────────────────────
    DB_UPSERT_FAILED("Failed to upsert pipeline run", HttpStatus.INTERNAL_SERVER_ERROR),
    DB_CONNECTION_FAILED("Database connection failed", HttpStatus.SERVICE_UNAVAILABLE),
    DB_RECORD_NOT_FOUND("Record not found", HttpStatus.NOT_FOUND),
    DB_UPSERT_CONFLICT("Upsert conflict on record", HttpStatus.CONFLICT),


    ;

    private final String message;
    private final HttpStatus status;

    ErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status  = status;
    }

    public String getMessage()   { return message; }
    public HttpStatus getStatus() { return status; }
}
