import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class TcpClient {
  public static void main(String[] args) throws IOException {
    final InetAddress localhost = InetAddress.getLocalHost();
    System.out.println(
        "クライアントを起動しました. これから " + localhost + " のポート番号 " + TcpServer.portNumber + "に接続します");
    final Socket socket = new Socket(localhost, TcpServer.portNumber);

    final State state = new State();
    setServerMessageCallback(
            new ObjectInputStream(socket.getInputStream()),
        (messageFromServer) -> {
          if (messageFromServer instanceof Welcome welcome) {
              logWithDate(welcome.date(), "チャットにようこそ");
              final long myId = welcome.yourId();
              state.myId = myId;
              System.out.println("あなたのIDは " + myId + " です");
              logParticipant(welcome.clientList());
              System.out.println("メッセージを入力して Enter キーで 全員に送信");
              System.out.println("/tell 12 message で 12 に対してプライベートメッセージを送信");
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
              return;
          }
          if(messageFromServer instanceof  NewPrivateMessage newPrivateMessage) {
              logWithDate(newPrivateMessage.date(), newPrivateMessage.id() + "からプライベートメッセージを受け取りました " + newPrivateMessage.message());
              return;
          }
          if(messageFromServer instanceof  InvalidPrivateId invalidPrivateId) {
              logWithDate(invalidPrivateId.date(), invalidPrivateId.id() + "は現在このチャットにはいません");
              logParticipant(invalidPrivateId.clientList());
          }
        });

    final ObjectOutputStream clientToServerStream =
        new ObjectOutputStream(socket.getOutputStream());

    setConsoleInputCallback(
        (inputtedMessage) -> {
            try {
              final var matcher = Pattern.compile("/tell (.+) ([\\s\\S]+)").matcher(inputtedMessage);
              if(matcher.matches()) {
                  sendMessageToServer(clientToServerStream, new PrivateMessage(Long.parseLong(matcher.group(1))
                          , matcher.group(2))
                  );
              } else {
                  sendMessageToServer(clientToServerStream, new GlobalMessage(inputtedMessage));
              }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  static void logParticipant(final long[] clientList) {
      System.out.println("このチャットルームには");
      for (long id : clientList) {
          System.out.println("- " +id);
      }
      System.out.println("の" + clientList.length + "名が参加しています");
  }

  static void sendMessageToServer(final ObjectOutputStream clientToServerStream, final ClientToServerData message) throws IOException {
      System.out.println("サーバーに " + message + " を送ります");
    clientToServerStream.writeObject(message);
          clientToServerStream.flush();
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
