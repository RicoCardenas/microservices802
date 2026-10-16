package com.ecommerce.auth.domain.model;

public class UserResult {

    private final boolean success;
    private final String message;
    private final User user;

    private UserResult(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public static UserResult success(String message, User user) {
        return new UserResult(true, message, user);
    }

    public static UserResult failure(String message) {
        return new UserResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }
}
