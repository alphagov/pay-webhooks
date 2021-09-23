package uk.gov.pay.webhooks.webhook.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import uk.gov.pay.webhooks.webhook.jpa.converter.InstantToUtcTimestampWithoutTimeZoneJpaConverter;

import java.time.Instant;

@Entity
@SequenceGenerator(name="webhooks_id_seq", sequenceName = "webhooks_id_seq", allocationSize = 1)
@Table(name = "webhooks")
public class WebhookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhooks_id_seq")
    private Long id;

    // The converter from Pay Java commons imports javax.persistence classes,
    // which have been moved to jakarta.persistence with EclipseLink 3 (so it
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

}
