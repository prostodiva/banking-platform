package com.bankapp.auth.application.refreshsession;

public record RefreshSessionCommand(String refreshToken) {
    @Override
    public String toString() {
        return "RefreshSessionCommand[REDACTED]";
    }
}
