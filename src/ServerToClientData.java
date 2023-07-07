import java.io.Serializable;
import java.util.Date;

/**
 * サーバーからクライアントに送るデータ
 */
public sealed interface ServerToClientData extends Serializable {
}

/**
 * 接続が完了した
 * 
 * @param yourId     あなたのID
 * @param clientList 他にチャットに参加しているメンバー
 */
record Welcome(long yourId, long[] clientList, Date date) implements ServerToClientData {
}

/**
 * あらたなクライアントが接続された
 */
record NewClient(long id, Date date) implements ServerToClientData {
}

/**
 * クライアントが退出した
 */
record LeaveClient(long id, Date date) implements ServerToClientData {
}

/**
 * だれかが発言した
 */
record NewMessage(long id, String message, Date date) implements ServerToClientData {
}

/**
 * プライベートメッセージを受信した
 */
record NewPrivateMessage(long id, String message, Date date) implements ServerToClientData {
}

/**
 * プライベートメッセージの送信先のIDの人がチャットにいない
 */
record InvalidPrivateId(long id, long[] clientList, Date date) implements ServerToClientData {
}
