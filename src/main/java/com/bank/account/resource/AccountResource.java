package com.bank.account.resource;

import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.DebitCreditRequest;
import com.bank.account.service.AccountService;
import com.bank.account.entity.enums.AccountStatus;
import com.bank.account.exception.InactiveAccountException;
import com.bank.account.exception.InsufficientFundsException;
import com.bank.account.entity.Account;
import com.bank.account.exception.AccountNotFoundException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Path("/api/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    private static final Logger LOG = Logger.getLogger(AccountResource.class);

    @Inject
    AccountService accountService;

    @GET
    @Path("/{accountNumber}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getAccount(@PathParam("accountNumber") String accountNumber) {
        LOG.infof("Getting account: %s", accountNumber);
        
        Optional<Account> account = accountService.findByAccountNumber(accountNumber);
        if (account.isPresent()) {
            return Response.ok(account.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Account not found: " + accountNumber + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/customer/{customerId}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getCustomerAccounts(@PathParam("customerId") Long customerId) {
        LOG.infof("Getting accounts for customer: %s", customerId);
        
        List<Account> accounts = accountService.findByCustomerId(customerId);
        return Response.ok(accounts).build();
    }

    @POST
    @RolesAllowed({"ROLE_ADMIN"})
    public Response createAccount(@Valid CreateAccountRequest request) {
        LOG.infof("Creating account for customer: %s", request.customerId);
        
        try {
            Account account = accountService.createAccount(request.customerId, request.accountType, request.initialBalance);
            return Response.status(Response.Status.CREATED).entity(account).build();
        } catch (Exception e) {
            LOG.error("Error creating account", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error creating account: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/{accountNumber}/balance")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response checkBalance(@PathParam("accountNumber") String accountNumber) {
        LOG.infof("Checking balance for account: %s", accountNumber);
        
        Optional<Account> account = accountService.findByAccountNumber(accountNumber);
        if (account.isPresent()) {
            return Response.ok()
                    .entity("{\"accountNumber\": \"" + accountNumber + 
                           "\", \"balance\": " + account.get().balance + 
                           ", \"status\": \"" + account.get().status + "\"}")
                    .build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Account not found: " + accountNumber + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/{accountNumber}/validate-balance")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response validateBalance(@PathParam("accountNumber") String accountNumber, 
                                   @QueryParam("amount") BigDecimal amount) {
        LOG.infof("Validating balance for account: %s, amount: %s", accountNumber, amount);
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Amount must be positive\"}")
                    .build();
        }
        
        try {
            Optional<Account> account = accountService.findByAccountNumber(accountNumber);
            if (account.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Account not found\"}")
                        .build();
            }
            
            Account validAccount = account.get();
            System.out.println(validAccount.status);
            System.out.println(AccountStatus.ACTIVE);
            System.out.println("Back to it");
            if (validAccount.status != AccountStatus.ACTIVE) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Account is not active\"}")
                        .build();
            }
            
            boolean hasBalance = accountService.validateBalance(accountNumber, amount);
            return Response.ok()
                    .entity("{\"hasBalance\": " + hasBalance + 
                           ", \"accountNumber\": \"" + accountNumber + 
                           "\", \"currentBalance\": " + validAccount.balance + "}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/{accountNumber}/debit")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response debitAccount(@PathParam("accountNumber") String accountNumber, 
                                @Valid DebitCreditRequest request) {
        LOG.infof("Debiting account: %s, amount: %s", accountNumber, request.amount);
        
        if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Amount must be positive\"}")
                    .build();
        }
        
        try {
            Account account = accountService.debitAccount(accountNumber, request.amount, request.transactionId);
            return Response.ok()
                    .entity("{\"newBalance\": " + account.balance + 
                           ", \"transactionId\": \"" + request.transactionId + 
                           "\", \"accountNumber\": \"" + accountNumber + "\"}")
                    .build();
        } catch (AccountNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (InsufficientFundsException | InactiveAccountException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOG.error("Error debiting account", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error debiting account: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/{accountNumber}/credit")
    @RolesAllowed({"ROLE_ADMIN"})
    public Response creditAccount(@PathParam("accountNumber") String accountNumber, 
                                 @Valid DebitCreditRequest request) {
        LOG.infof("Crediting account: %s, amount: %s", accountNumber, request.amount);
        
        if (request.amount == null || request.amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Amount must be positive\"}")
                    .build();
        }
        
        try {
            Account account = accountService.creditAccount(accountNumber, request.amount, request.transactionId);
            return Response.ok()
                    .entity("{\"newBalance\": " + account.balance + 
                           ", \"transactionId\": \"" + request.transactionId + 
                           "\", \"accountNumber\": \"" + accountNumber + "\"}")
                    .build();
        } catch (AccountNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (InactiveAccountException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOG.error("Error crediting account", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Error crediting account: " + e.getMessage() + "\"}")
                    .build();
        }
    }

}
