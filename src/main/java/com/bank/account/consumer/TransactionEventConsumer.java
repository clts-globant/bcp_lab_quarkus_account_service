package com.bank.account.consumer;

import com.bank.account.service.AccountService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

@ApplicationScoped
public class TransactionEventConsumer {

    private static final Logger LOG = Logger.getLogger(TransactionEventConsumer.class);
    
    @Inject
    AccountService accountService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Incoming("transactionss")
    public void processTransactionEvent(String eventJson) {
        LOG.infof("Received transaction event: %s", eventJson);
        
        try {
            JsonNode eventNode = objectMapper.readTree(eventJson);
            String eventType = eventNode.get("eventType").asText();

            switch (eventType) {
                case "TRANSACTION_COMPLETED":
                    handleTransactionCompleted(eventNode);
                    break;
                case "TRANSACTION_FAILED":
                    handleTransactionFailed(eventNode);
                    break;
                default:
                    LOG.warnf("Unknown event type: %s", eventType);
            }
            
        } catch (Exception e) {
            LOG.errorf("Error processing transaction event: %s", e.getMessage());
        }
    }
    
    private void handleTransactionCompleted(JsonNode eventNode) {
        try {
            String originAccount = eventNode.get("originAccount").asText();
            String destinationAccount = eventNode.get("destinationAccount").asText();
            BigDecimal amount = new BigDecimal(eventNode.get("amount").asText());
            String transactionId = eventNode.get("transactionId").asText();
            
            LOG.infof("Processing completed transaction %s: %s -> %s, amount: %s", 
                     transactionId, originAccount, destinationAccount, amount);
            
            accountService.updateBalance(originAccount, amount.negate());

            accountService.updateBalance(destinationAccount, amount);
            
            LOG.infof("Successfully updated balances for transaction %s", transactionId);
            
        } catch (Exception e) {
            LOG.errorf("Error processing completed transaction: %s", e.getMessage());
        }
    }
    
    private void handleTransactionFailed(JsonNode eventNode) {
        try {
            String transactionId = eventNode.get("transactionId").asText();
            String reason = eventNode.get("reason").asText();
            
            LOG.infof("Transaction %s failed: %s", transactionId, reason);
            
        } catch (Exception e) {
            LOG.errorf("Error processing failed transaction: %s", e.getMessage());
        }
    }
}
