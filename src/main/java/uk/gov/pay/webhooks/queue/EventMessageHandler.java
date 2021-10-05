package uk.gov.pay.webhooks.queue;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;
    private final MetricRegistry metricRegistry;

    @Inject
    public EventMessageHandler(EventQueue eventQueue,
                               MetricRegistry metricRegistry) {
        this.eventQueue = eventQueue;
        this.metricRegistry = metricRegistry;
    }

    public void handle() throws QueueException {
        List<EventMessage> eventMessages = eventQueue.retrieveEvents();

        for (EventMessage message : eventMessages) {
            try {
                processSingleMessage(message);
            } catch (Exception e) {
                LOGGER.warn("Error during handling the event message");
            }
        }
    }

    private void processSingleMessage(EventMessage message) throws QueueException {
        LOGGER.warn(message.getEvent().toString());
        


    }
}
