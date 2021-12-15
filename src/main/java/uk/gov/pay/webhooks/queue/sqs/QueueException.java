package uk.gov.pay.webhooks.queue.sqs;

public class QueueException extends Exception {

    public QueueException(String message, Exception e) {
        super(message, e);
    }

}
