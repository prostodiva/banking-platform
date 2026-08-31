
package com.bankapp.auth.application.registeruser;

public record RegisterUserCommand(String email, String fullName, String password) {
    @Override
    public String toString() {
        return "RegisterUserCommand[email=" + email + "]";
    }
}
