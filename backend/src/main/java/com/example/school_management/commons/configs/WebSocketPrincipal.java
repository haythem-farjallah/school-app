package com.example.school_management.commons.configs;

import java.security.Principal;

/**
 * The account a STOMP session authenticated as, and the token version its CONNECT was accepted with.
 * Its name is the account id, so nothing that prints the principal exposes the email.
 */
public record WebSocketPrincipal(long accountId, int tokenVersion) implements Principal {

    @Override
    public String getName() {
        return Long.toString(accountId);
    }
}
