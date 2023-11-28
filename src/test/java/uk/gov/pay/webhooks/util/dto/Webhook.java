package uk.gov.pay.webhooks.util.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Webhook {
    private final int webhookId;
    private String webhookExternalId;
    private String serviceExternalId;
    private String endpointUrl;
    private String live;
    private String gatewayAccountId;
}
