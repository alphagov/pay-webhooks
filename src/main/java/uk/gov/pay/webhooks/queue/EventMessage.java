package uk.gov.pay.webhooks.queue;

import uk.gov.pay.webhooks.queue.sqs.QueueMessage;

public record EventMessage(EventMessageDto eventMessageDto, QueueMessage queueMessage) {

    public static EventMessage of(EventMessageDto eventMessageDto, QueueMessage queueMessage) {
        return new EventMessage(eventMessageDto, queueMessage);
    }

    public InternalEvent toInternalEvent() {
        return new InternalEvent(
                eventMessageDto.eventType(),
                eventMessageDto.serviceId(),
                eventMessageDto.live(),
                eventMessageDto.resourceExternalId(),
                eventMessageDto.parentResourceExternalId(),
                eventMessageDto.eventData(),
                eventMessageDto.eventDate(),
                eventMessageDto.resourceType()
                );
    }

}
