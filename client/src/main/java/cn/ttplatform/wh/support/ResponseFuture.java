package cn.ttplatform.wh.support;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A {@link CompletableFuture} wrapper that holds metadata about a pending Raft command.
 *
 * Supports both blocking (sync) and callback (async) result retrieval:
 * - Sync:  {@code future.get(timeout, unit)}
 * - Async: {@code future.thenAccept(callback)}
 *
 * @param <T> the result type
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
public class ResponseFuture<T> extends CompletableFuture<T> {

    private final String commandId;
    private final long createTimeNanos;

    private ResponseFuture(String commandId) {
        this.commandId = commandId;
        this.createTimeNanos = System.nanoTime();
    }

    /**
     * Create a new ResponseFuture with an auto-timeout.
     *
     * @param commandId  the command ID for correlation
     * @param timeoutMs  timeout in milliseconds; &lt;= 0 means no timeout
     * @param <T>        the expected result type
     * @return a new ResponseFuture
     */
    public static <T> ResponseFuture<T> create(String commandId, long timeoutMs) {
        ResponseFuture<T> future = new ResponseFuture<>(commandId);
        if (timeoutMs > 0) {
            future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        }
        return future;
    }

    public String getCommandId() {
        return commandId;
    }

    public long getCreateTimeNanos() {
        return createTimeNanos;
    }

    /**
     * Convenience method for blocking with a default-style get.
     *
     * @param timeout  timeout duration
     * @param unit     time unit
     * @return the result
     * @throws ClientException on timeout or failure
     */
    public T getSync(long timeout, TimeUnit unit) throws ClientException {
        try {
            return get(timeout, unit);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new ClientException("Request timed out after " + timeout + " " + unit, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClientException("Request interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ClientException) {
                throw (ClientException) cause;
            }
            throw new ClientException("Request failed: " + cause.getMessage(), cause);
        }
    }
}
