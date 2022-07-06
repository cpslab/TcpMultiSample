import java.io.Serializable;

public interface ServerToClientData extends Serializable {
}

/**
 * 接続が完了し ID が割り振られた
 */
record AssignedId(long yourId) implements ServerToClientData { }

/**
 * だれかが発言した
 */
record NewMessage(String message) implements ServerToClientData { }
