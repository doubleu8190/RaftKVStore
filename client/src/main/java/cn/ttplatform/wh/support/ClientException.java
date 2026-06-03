package cn.ttplatform.wh.support;

/**
 * Client-side exception wrapping server errors and connectivity issues.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
public class ClientException extends RuntimeException {

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
