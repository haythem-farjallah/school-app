package com.example.school_management.commons.configs;

import java.security.Principal;

/**
 * The account a STOMP session authenticated as, loaded from the database when its CONNECT frame was
 * accepted. Its name is the account id, so nothing that prints the principal exposes the email.
 */
public record WebSocketPrincipal(long accountId, String email) implements Principal {

    @Override
    public String getName() {
        return Long.toString(accountId);
    }

    @Override
    public String toString() {
        return "WebSocketPrincipal[accountId=" + accountId + "]";
    }
}
