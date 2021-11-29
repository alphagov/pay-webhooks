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

    public InternalEvent getEvent() {
        return new InternalEvent(
                eventDto.getEventType(),
                eventDto.getServiceId(),
                eventDto.isLive(),
                eventDto.getExternalId(),
                eventDto.getEventDate(),
                eventDto.getEventData()
        );
    }

    public String getQueueMessageReceiptHandle() {
        return queueMessage.getReceiptHandle();
    }
}
