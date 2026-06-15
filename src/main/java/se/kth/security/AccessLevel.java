package se.kth.security;

public enum AccessLevel {
    READ,
    WRITE;

    public boolean allows(AccessLevel required) {
        if (this == WRITE) {
            return true;
        }

        return this == required;
    }
}