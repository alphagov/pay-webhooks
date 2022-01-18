package uk.gov.pay.webhooks.publicapi.model;

import static java.util.Arrays.stream;

// Based on Connector enum: https://github.com/alphagov/pay-connector/blob/master/src/main/java/uk/gov/pay/connector/common/model/api/ExternalChargeState.java 
public enum ExternalChargeState {
    EXTERNAL_CREATED("created", false),
    EXTERNAL_STARTED("started", false),
    EXTERNAL_SUBMITTED("submitted", false),
    EXTERNAL_CAPTURABLE("capturable", false),
    EXTERNAL_SUCCESS("success", true),
    EXTERNAL_FAILED_REJECTED( "declined", true, "P0010", "Payment method rejected"),
    EXTERNAL_FAILED_EXPIRED("timedout", true, "P0020", "Payment expired"),
    EXTERNAL_FAILED_CANCELLED( "cancelled", true, "P0030", "Payment was cancelled by the user"),
    EXTERNAL_CANCELLED("cancelled", true, "P0040", "Payment was cancelled by the service"),
    EXTERNAL_ERROR_GATEWAY("error", true, "P0050", "Payment provider returned an error");

    private final String status;
    private final boolean finished;
    private final String code;
    private final String message;

    ExternalChargeState(String status, boolean finished) {
        this.status = status;
        this.finished = finished;
        this.code = null;
        this.message = null;
    }

    ExternalChargeState(String status, boolean finished, String code, String message) {
        this.status = status;
        this.finished = finished;
        this.code = code;
        this.message = message;
    }
    

    public String getStatus() {
        return status;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ExternalChargeState fromStatusString(String status) {
        return stream(values()).filter(v -> v.getStatus().equals(status)).findFirst().orElseThrow(() -> new IllegalArgumentException("External charge state not recognized: " + status));
    }
}
