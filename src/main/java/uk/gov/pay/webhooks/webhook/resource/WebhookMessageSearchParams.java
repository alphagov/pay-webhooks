package uk.gov.pay.webhooks.webhook.resource;

import io.swagger.v3.oas.annotations.Parameter;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;

import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class WebhookMessageSearchParams {

    private static final String DELIVERY_STATUS_FIELD = "lastDeliveryStatus";
    private static final String RESOURCE_EXTERNAL_ID_FIELD = "resourceExternalId";
    
    private Integer page;

    @Valid
    private DeliveryStatus deliveryStatus;
    
    private String resourceId;

    private Map<String, Object> queryMap;

    // For Jackson
    public WebhookMessageSearchParams() {
    }

    public WebhookMessageSearchParams(Integer page, DeliveryStatus deliveryStatus, String resourceId) {
        this.page = page;
        this.deliveryStatus = deliveryStatus;
        this.resourceId = resourceId;
    }

    @QueryParam("page")
    @Parameter(example = "1")
    public void setPage(Integer page) {
        this.page = Objects.requireNonNullElse(page, 1);
    }

    @QueryParam("status")
    @Parameter(example = "SUCCESSFUL", description = "If supplied, will only list messages with this delivery status")
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    @QueryParam("resource_id")
    @Parameter(example = "2qmfui4aklat22u0jab98m0ou4", description = "The external ID of the resource (e.g. the payment) to get messages for")
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Integer getPage() {
        return page;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }
    
    public List<String> getFilterTemplates() {
        List<String> filters = new ArrayList<>();

        if (deliveryStatus != null) {
            filters.add(" lastDeliveryStatus = :" + DELIVERY_STATUS_FIELD);
        }

        if (isNotBlank(resourceId)) {
            filters.add(" resourceExternalId = :" + RESOURCE_EXTERNAL_ID_FIELD);
        }

        return List.copyOf(filters);
    }

    public Map<String, Object> getQueryMap() {
        if (queryMap == null) {
            queryMap = new HashMap<>();

            if (deliveryStatus != null) {
                queryMap.put(DELIVERY_STATUS_FIELD, deliveryStatus);
            }

            if (isNotBlank(resourceId)) {
                queryMap.put(RESOURCE_EXTERNAL_ID_FIELD, resourceId);
            }
        }

        return queryMap;
    }
}
