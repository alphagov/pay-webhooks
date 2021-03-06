package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;

import static org.junit.jupiter.api.Assertions.*;

class PaymentApiRepresentationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    @Test
    void serialisesWebhookMessage() throws JsonProcessingException {

        var ledgerTransactionJson = """
                            {
                                 	"amount": 1000,
                                 	"state": {
                                 		"finished": false,
                                 		"status": "created"
                                 	},
                                 	"description": "Test description",
                                 	"reference": "aReference",
                                 	"language": "en",
                                 	"transaction_id": "3rke415aam1pl1u3hvaljbcll3",
                                 	"return_url": "https://example.org",
                                 	"email": "someone@example.org",
                                 	"payment_provider": "sandbox",
                                 	"created_date": "2018-09-22T10:13:16.067Z",
                                 	"card_details": {
                                 		"card_type": "debit",
                                 		"card_brand": "Visa",
                                 		"expiry_date": "10/24",
                                 		"cardholder_name": "j.doe@example.org",
                                 		"last_digits_card_number": "1234",
                                 		"first_digits_card_number": "123456",
                                 		"billing_address": {
                                 			"line1": "line1",
                                 			"line2": "line2",
                                 			"postcode": "AB1 2CD",
                                 			"city": "London",
                                 			"country": "GB"
                                 		}
                                 	},
                                 	"delayed_capture": false,
                                 	"moto": false,
                                 	"authorisation_summary": {
                                 		"three_d_secure": {
                                 			"required": true,
                                 			"version": "2.1.0"
                                 		}
                                 	}
                                 }
                """;

        var expectedJson = """
                {
                    "payment_id": "3rke415aam1pl1u3hvaljbcll3",
                    "amount": 1000,
                    "state": {
                        "status": "created",
                        "finished": false
                    },
                    "return_url": "https://example.org",
                    "description": "Test description",
                    "reference": "aReference",
                    "email": "someone@example.org",
                    "payment_provider": "sandbox",
                    "created_date": "2018-09-22T10:13:16.067Z",
                    "language": "en",
                    "delayed_capture": false,
                    "moto": false,
                    "card_details": {
                        "last_digits_card_number": "1234",
                        "first_digits_card_number": "123456",
                        "cardholder_name": "j.doe@example.org",
                        "expiry_date": "10/24",
                        "billing_address": {
                            "line1": "line1",
                            "line2": "line2",
                            "postcode": "AB1 2CD",
                            "city": "London",
                            "country": "GB"
                        },
                        "card_brand": "Visa",
                        "card_type": "debit"
                    },
                    "authorisation_summary": {
                        "three_d_secure": {
                            "required": true
                        }
                    }
                }
                        """;

        LedgerTransaction ledgerTransaction = objectMapper.readValue(ledgerTransactionJson, LedgerTransaction.class);
        PaymentApiRepresentation transformed = PaymentApiRepresentation.of(ledgerTransaction);
        assertEquals(objectMapper.readTree(expectedJson).toString(), objectMapper.valueToTree(transformed).toString());

    }

    @Test
    void getsErrorCodeFromStatus() throws JsonProcessingException {
        var ledgerTransactionJson = """
                            {
                                 	"amount": 1000,
                                 	"state": {
                                 		"finished": true,
                                 		"status": "error"
                                 	},
                                 	"description": "Test description",
                                 	"reference": "aReference",
                                 	"language": "en",
                                 	"transaction_id": "3rke415aam1pl1u3hvaljbcll3",
                                 	"return_url": "https://example.org",
                                 	"email": "someone@example.org",
                                 	"payment_provider": "sandbox",
                                 	"created_date": "2018-09-22T10:13:16.067Z",
                                 	"card_details": {
                                 		"card_type": "debit",
                                 		"card_brand": "Visa",
                                 		"expiry_date": "10/24",
                                 		"cardholder_name": "j.doe@example.org",
                                 		"last_digits_card_number": "1234",
                                 		"first_digits_card_number": "123456",
                                 		"billing_address": {
                                 			"line1": "line1",
                                 			"line2": "line2",
                                 			"postcode": "AB1 2CD",
                                 			"city": "London",
                                 			"country": "GB"
                                 		}
                                 	},
                                 	"delayed_capture": false,
                                 	"moto": false,
                                 	"authorisation_summary": {
                                 		"three_d_secure": {
                                 			"required": true,
                                 			"version": "2.1.0"
                                 		}
                                 	}
                                 }
                """;

        var expectedState = """ 
                        {
                        "status": "error",
                        "finished": true,
                        "message": "Payment provider returned an error",
                        "code": "P0050"
                         }
                """;

        LedgerTransaction ledgerTransaction = objectMapper.readValue(ledgerTransactionJson, LedgerTransaction.class);
        var transformed = PaymentApiRepresentation.of(ledgerTransaction);
        assertEquals(objectMapper.readTree(expectedState), objectMapper.valueToTree(transformed).get("state"));
    }
    
    @Test
    void payloadWithoutOptionalFieldsDoesNotThrow() throws JsonProcessingException {
        var ledgerTransactionJsonWithoutOptionalFields = """
                            {
                                 	"amount": 1000,
                                 	"state": {
                                 		"finished": true,
                                 		"status": "error"
                                 	},
                                 	"description": "Test description",
                                 	"reference": "aReference",
                                 	"language": "en",
                                 	"transaction_id": "3rke415aam1pl1u3hvaljbcll3",
                                 	"payment_provider": "sandbox",
                                 	"created_date": "2018-09-22T10:13:16.067Z",
                                 	"delayed_capture": false,
                                 	"moto": false
                                 	}
                                 }
                """;

        LedgerTransaction ledgerTransaction = objectMapper.readValue(ledgerTransactionJsonWithoutOptionalFields, LedgerTransaction.class);
        assertDoesNotThrow(() -> PaymentApiRepresentation.of(ledgerTransaction));
        
    }
}
