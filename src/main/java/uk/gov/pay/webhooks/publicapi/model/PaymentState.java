package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class PaymentState {
    @JsonProperty("status")
    private String status;

    @JsonProperty("finished")
    private boolean finished;

    @JsonProperty("message")
    private String message;

    @JsonProperty("code")
    private String code;


    public static PaymentState createPaymentState(JsonNode node) {
        return new PaymentState(
                node.get("status").asText(),
                node.get("finished").asBoolean(),
                node.has("message") ? node.get("message").asText() : null,
                node.has("code") ? node.get("code").asText() : null
        );
    }

    public PaymentState() {
    }

    public PaymentState(String status, boolean finished) {
        this(status, finished, null, null);
    }

    public PaymentState(String status, boolean finished, String message, String code) {
        this.status = status;
        this.finished = finished;
        this.message = message;
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public boolean isFinished() {
        return finished;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "PaymentState{" +
                "status='" + status + '\'' +
                ", finished='" + finished + '\'' +
                ", message=" + message +
                ", code=" + code +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentState that = (PaymentState) o;
        return finished == that.finished &&
                Objects.equals(status, that.status) &&
                Objects.equals(message, that.message) &&
                Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, finished, message, code);
    }
}
