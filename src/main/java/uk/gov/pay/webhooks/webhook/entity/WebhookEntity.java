package uk.gov.pay.webhooks.webhook.entity;

import uk.gov.pay.webhooks.webhook.WebhookDTO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;

@Entity
@SequenceGenerator(name="webhooks_id_seq", sequenceName = "webhooks_id_seq", allocationSize = 1)
@Table(name = "webhooks")
public class WebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhooks_id_seq")
    private Long id;

    @Column(name = "created_date")
    private Date createdDate;

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
        entity.setCreatedDate(Date.from(Instant.now()));
        entity.setStatus(WebhookStatus.ACTIVE);
        entity.setExternalId(new BigInteger(130, new SecureRandom()).toString(32));
        return entity;
    }

    private void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    private void setStatus(WebhookStatus status) {
        this.status = status;
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

    public void setCreatedDate(Date instant) {
        this.createdDate = instant;
    }

    public Long getId() {
        return id;
    }

}
