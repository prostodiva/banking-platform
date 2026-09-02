
package com.bankapp.auth.application.login;

public record LoginCommand(String email, String password) {
    @Override
    public String toString() {
        return "LoginCommand[email=" + email + "]";
    }
}
