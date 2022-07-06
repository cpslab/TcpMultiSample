import java.io.Serializable;

/**
 * クライアントからサーバーに送るデータ
 */
public record ClientToServerData(String message) implements Serializable { }
