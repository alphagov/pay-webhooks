package uk.gov.pay.webhooks.queue;


public class EventMessage {
    private EventMessageDto eventDto;
    private QueueMessage queueMessage;

    public EventMessage(EventMessageDto eventDto, QueueMessage queueMessage) {
        this.eventDto = eventDto;
        this.queueMessage = queueMessage;
    }

    public static EventMessage of(EventMessageDto eventDto, QueueMessage queueMessage) {
        return new EventMessage(eventDto, queueMessage);
    }

    public String getQueueMessageId() {
        return queueMessage.getMessageId();
    }

    public Event getEvent() {
        return new Event(
                getQueueMessageId(),
                eventDto.getServiceId(),
                eventDto.isLive(),
                eventDto.getExternalId(),
                eventDto.getParentExternalId(),
                eventDto.getEventDate(),
                eventDto.getEventType(),
                eventDto.getEventData(),
                eventDto.isReprojectDomainObject()
        );
    }

    public String getQueueMessageReceiptHandle() {
        return queueMessage.getReceiptHandle();
    }
}
