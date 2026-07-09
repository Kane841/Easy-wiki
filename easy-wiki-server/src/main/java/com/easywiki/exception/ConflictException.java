package com.easywiki.exception;

public class ConflictException extends BusinessException {
    public ConflictException(String message) { super(409, message); }
}
