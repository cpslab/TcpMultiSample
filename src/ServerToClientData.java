import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * サーバーからクライアントに送るデータ
 */
public interface ServerToClientData extends Serializable { }

/**
 * 接続が完了した
 * @param yourId あなたのID
 * @param clientList 他にチャットに参加しているメンバー
 */
record Welcome(long yourId, long[] clientList) implements ServerToClientData { }


/**
 * あらたなクライアントが接続された
 */
record NewClient(long id) implements ServerToClientData { }

/**
 * だれかが発言した
 */
record NewMessage(String message, Date date) implements ServerToClientData { }
