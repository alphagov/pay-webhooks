package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.publicapi.model.PaymentForSearchResult;


import static org.junit.jupiter.api.Assertions.*;

class WebhookMessageServiceTest {

    @Test
    void paymentForSearchResultFromSerialisesWebhookMessage() throws JsonProcessingException {
        
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
                	"amount": 1000,
                	"description": "Test description",
                	"reference": "aReference",
                	"language": "en",
                	"email": "someone@example.org",
                	"state": {
                		"status": "created",
                		"finished": false
                	},
                	"payment_id": "3rke415aam1pl1u3hvaljbcll3",
                	"payment_provider": "sandbox",
                	"created_date": "2018-09-22T10:13:16.067Z",
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
                	"delayed_capture": false,
                	"moto": false,
                	"provider_id": "sandbox",
                	"return_url": "https://example.org",
                	"authorisation_summary": {
                		"three_d_secure": {
                			"required": true
                		}
                	},
                	"card_brand": "Visa"
                }
                """;

        var objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        LedgerTransaction ledgerTransaction = objectMapper.readValue(ledgerTransactionJson, LedgerTransaction.class);
        PaymentForSearchResult transformed = WebhookMessageService.paymentForSearchResultFrom(ledgerTransaction);
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

        var objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        LedgerTransaction ledgerTransaction = objectMapper.readValue(ledgerTransactionJson, LedgerTransaction.class);
        PaymentForSearchResult transformed = WebhookMessageService.paymentForSearchResultFrom(ledgerTransaction);
        assertEquals(objectMapper.readTree(expectedState), objectMapper.valueToTree(transformed).get("state"));
    }
    
    
}
