package uk.gov.pay.webhooks.webhook.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import uk.gov.pay.webhooks.webhook.WebhookDTO;
import uk.gov.pay.webhooks.webhook.converter.InstantToUtcTimestampWithoutTimeZoneJpaConverter;

import java.time.Instant;

@Entity
@SequenceGenerator(name="webhooks_id_seq", sequenceName = "webhooks_id_seq", allocationSize = 1)
@Table(name = "webhooks")
public class WebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhooks_id_seq")
    private Long id;

    // The converter from Pay Java commons imports javax.persistence classes,
    // which have been moved to javax.persistence with EclipseLink 3 (so it
    // won’t work)
    // So currently we’re not importing the converter from Pay Java commons and
    // have a copy in this project
    // But we might want to just use EclipseLink 2.7 instead
    @Column(name = "created_at")
    @Convert(converter = InstantToUtcTimestampWithoutTimeZoneJpaConverter.class)
    private Instant createdAt; // should this be created_date to match equivalent columns in other microservices?

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "service_id")
    private String serviceId;

    private boolean live;

    @Column(name = "callback_url")
    private String callbackUrl;
    
    private String description;

    @Enumerated(EnumType.STRING)
    private WebhookStatus status;
    
    public static WebhookEntity from(WebhookDTO webhookDTO) {
        var entity = new WebhookEntity();
        entity.setDescription(webhookDTO.getDescription());
        entity.setCallbackUrl(webhookDTO.getCallbackUrl());
        entity.setServiceId(webhookDTO.getServiceId());
        entity.setLive(webhookDTO.isLive());
        return entity;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
