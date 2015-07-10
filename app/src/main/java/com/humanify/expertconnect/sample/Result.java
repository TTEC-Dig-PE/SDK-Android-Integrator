package com.humanify.expertconnect.sample;

public final class Result<T> {
    private final T success;
    private final Exception error;
    private final boolean isSuccess;

    public static <T> Result<T> success(T value) {
        return new Result<>(value);
    }

    public static <T> Result<T> error(Exception value) {
        return new Result<>(value);
    }

    private Result(T success) {
        this.success = success;
        this.error = null;
        this.isSuccess = true;
    }

    private Result(Exception error) {
        this.success = null;
        this.error = error;
        this.isSuccess = false;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isError() {
        return !isSuccess;
    }

    public T getSuccess() {
        if (isSuccess) {
            return success;
        } else {
            throw new IllegalStateException("Result is error");
        }
    }

    public Exception getError() {
        if (!isSuccess) {
            return error;
        } else {
            throw new IllegalStateException("Result is success");
        }
    }
}
