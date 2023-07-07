import java.io.Serializable;

/**
 * クライアントからサーバーに送るデータ
 */
public sealed interface ClientToServerData extends Serializable {}

record GlobalMessage(String message) implements ClientToServerData {}

record PrivateMessage(long id, String message) implements  ClientToServerData {}
