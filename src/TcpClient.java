import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.function.Consumer;

public class TcpClient {
  public static void main(String[] args) throws IOException {
    final InetAddress localhost = InetAddress.getLocalHost();
    System.out.println(
        "クライアントを起動しました. これから " + localhost + " のポート番号 " + TcpServer.portNumber + "に接続します");
    final Socket socket = new Socket(localhost, TcpServer.portNumber);

    final ObjectInputStream serverToClientStream = new ObjectInputStream(socket.getInputStream());

    final State state = new State();
    setServerMessageCallback(
        serverToClientStream,
        (messageFromServer) -> {
          if (messageFromServer instanceof Welcome welcome) {
              logWithDate(welcome.date(), "チャットにようこそ");
              final long myId = welcome.yourId();
              state.myId = myId;
              System.out.println("あなたのIDは " + myId + " です");
              System.out.println("このチャットルームには");
              for (long id : welcome.clientList()) {
                  System.out.println("- " +id);
              }
              System.out.println("の" + welcome.clientList().length + "名が参加しています");
              System.out.println("メッセージを入力して Enter キーで送信");
              return;
          }
          if(messageFromServer instanceof NewClient newClient) {
              logWithDate(newClient.date(), newClient.id() + "さんが参加しました");
              return;
          }
          if(messageFromServer instanceof  LeaveClient leaveClient) {
              logWithDate(leaveClient.date(), leaveClient.id() + "さんが退出しました");
              return;
          }
          if(messageFromServer instanceof NewMessage newMessage) {
              logWithDate(newMessage.date(),   "<"  + newMessage.id() +
                      (newMessage.id() == state.myId ? "(自分)" : "") +"> " + newMessage.message()
              );
          }
        });

    final ObjectOutputStream clientToServerStream =
        new ObjectOutputStream(socket.getOutputStream());

    setConsoleInputCallback(
        (inputtedMessage) -> {
          try {
            final ClientToServerData clientToServerData = new ClientToServerData(inputtedMessage);
            System.out.println("サーバーに " + clientToServerData + " を送ります");
            clientToServerStream.writeObject(clientToServerData);
            clientToServerStream.flush();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  static void setServerMessageCallback(
      final ObjectInputStream serverToClientStream, final Consumer<ServerToClientData> onMessage) {

    new Thread(
            () -> {
              try {
                while (true) {
                  onMessage.accept((ServerToClientData) serverToClientStream.readObject());
                }

              } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
              }
            })
        .start();
  }

  /**
   * コンソールから入力を受け付ける
   *
   * @param onInput 文字を1行入力されたときに毎回呼ばれる
   */
  static void setConsoleInputCallback(final Consumer<String> onInput) {
    final Scanner consoleInputScanner = new Scanner(System.in);

    new Thread(
            () -> {
              while (true) {
                onInput.accept(consoleInputScanner.nextLine());
              }
            })
        .start();
  }

  static void logWithDate(final Date date, final String message) {
      System.out.println(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date)  + ": " + message);
  }
}

/**
 * クライアントの状態 (自分自身のIDのみ)
 */
class State {
    /**
     * 自分自身のID. null になることがあるので注意!
     */
    long myId;
}
