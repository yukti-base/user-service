package org.yuktisetu.userservice.exception;

public final class UserServiceExceptions {
    private UserServiceExceptions() {}

    public static class ProfileNotFoundException extends RuntimeException {
        public ProfileNotFoundException() { super("Student profile not found for this user"); }
    }
}
