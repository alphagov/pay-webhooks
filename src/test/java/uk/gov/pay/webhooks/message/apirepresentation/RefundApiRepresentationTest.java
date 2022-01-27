package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;

import static org.junit.jupiter.api.Assertions.*;

class RefundApiRepresentationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    @Test
    void serialisesWebhookResource() throws JsonProcessingException {
        var ledgerRefundJson = """
                 {
                	"gateway_account_id": "526",
                	"service_id": "123",
                	"amount": 2000,
                	"state": {
                		"finished": true,
                		"status": "success"
                	},
                	"created_date": "2022-01-26T16:52:41.178Z",
                	"gateway_transaction_id": "baz",
                	"settlement_summary": {},
                	"refunded_by": "1234",
                	"refunded_by_user_email": "jane@example.com",
                	"transaction_type": "REFUND",
                	"live": false,
                	"payment_details": {
                		"description": "An example payment description",
                		"reference": "MG2ZYR4BLV",
                		"email": "jill@example.com",
                		"card_details": {
                			"cardholder_name": "jane",
                			"card_brand": "Visa",
                			"last_digits_card_number": "1234",
                			"first_digits_card_number": "123456",
                			"expiry_date": "10/24",
                			"card_type": "debit"
                		},
                		"transaction_type": "PAYMENT"
                	},
                	"transaction_id": "345",
                	"parent_transaction_id": "789",
                	"parent": {
                		"gateway_account_id": "123",
                		"service_id": "123",
                		"credential_external_id": "456",
                		"amount": 2000,
                		"state": {
                			"finished": true,
                			"status": "success"
                		},
                		"description": "An example payment description",
                		"reference": "foo",
                		"language": "en",
                		"return_url": "https://example.com/payment-complete/",
                		"email": "jane@example.com",
                		"payment_provider": "sandbox",
                		"created_date": "2022-01-26T16:51:49.557Z",
                		"card_details": {
                			"cardholder_name": "jane",
                			"billing_address": {
                				"line1": "line1",
                				"line2": "line2",
                				"postcode": "N1 111",
                				"city": "London",
                				"country": "GB"
                			},
                			"card_brand": "Visa",
                			"last_digits_card_number": "5556",
                			"first_digits_card_number": "400005",
                			"expiry_date": "10/24",
                			"card_type": "debit"
                		},
                		"delayed_capture": false,
                		"gateway_transaction_id": "1234",
                		"refund_summary": {
                			"status": "full",
                			"user_external_id": null,
                			"amount_available": 0,
                			"amount_submitted": 2000,
                			"amount_refunded": 2000
                		},
                		"settlement_summary": {
                			"capture_submit_time": "2022-01-26T16:52:09.039Z",
                			"captured_date": "2022-01-26"
                		},
                		"transaction_type": "PAYMENT",
                		"moto": false,
                		"live": false,
                		"source": "CARD_PAYMENT_LINK",
                		"transaction_id": "9999"
                	}
                }
                """;
        
        var expectedJson = """
                {
                	"refund_id": "345",
                	"created_date": "2022-01-26T16:52:41.178Z",
                	"amount": 2000,
                	"status": "success",
                	"settlement_summary": {
                		"capture_submit_time": null,
                		"captured_date": null
                	},
                	"payment_id": "789"
                }
                """;
        
        

        LedgerTransaction ledgerTransaction = objectMapper.readValue(ledgerRefundJson, LedgerTransaction.class);
        assertDoesNotThrow(() -> RefundApiRepresentation.of(ledgerTransaction));
        var refundApiRepresentation = RefundApiRepresentation.of(ledgerTransaction);
        assertEquals(objectMapper.valueToTree(refundApiRepresentation).toString(), objectMapper.readTree(expectedJson).toString());
        
        
        
        
        
    }
}
