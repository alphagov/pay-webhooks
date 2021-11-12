package uk.gov.pay.webhooks.ledger.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundSummary {

    @JsonProperty("status")
    private String status;
    @JsonProperty("user_external_id")
    private String userExternalId;
    @JsonProperty("amount_available")
    private Long amountAvailable;
    @JsonProperty("amount_submitted")
    private Long amountSubmitted;

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAmountAvailable(Long amountAvailable) {
        this.amountAvailable = amountAvailable;
    }

    public void setAmountSubmitted(Long amountSubmitted) {
        this.amountSubmitted = amountSubmitted;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public void setUserExternalId(String userExternalId) {
        this.userExternalId = userExternalId;
    }

    public Long getAmountAvailable() {
        return amountAvailable;
    }

    public Long getAmountSubmitted() {
        return amountSubmitted;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RefundSummary that = (RefundSummary) o;

        if (!status.equals(that.status)) {
            return false;
        }
        if (userExternalId != null ? !userExternalId.equals(that.userExternalId)
                : that.userExternalId != null) {
            return false;
        }
        if (amountAvailable != null ? !amountAvailable.equals(that.amountAvailable)
                : that.amountAvailable != null) {
            return false;
        }
        return amountSubmitted != null ? amountSubmitted.equals(that.amountSubmitted)
                : that.amountSubmitted == null;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (userExternalId != null ? userExternalId.hashCode() : 0);
        result = 31 * result + (amountAvailable != null ? amountAvailable.hashCode() : 0);
        result = 31 * result + (amountSubmitted != null ? amountSubmitted.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RefundSummary{" +
                "status='" + status + '\'' +
                "userExternalId='" + userExternalId + '\'' +
                ", amountAvailable=" + amountAvailable +
                ", amountSubmitted=" + amountSubmitted +
                '}';
    }

}
