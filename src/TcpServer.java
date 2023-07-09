import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * チャット コンソールアプリ の サーバー
 */
public class TcpServer {
  public static int portNumber = 1234;

  public static void main(String[] args) throws IOException {
    final ArrayList<Server1ClientThread> serverThreadArrayList = new ArrayList<>();
    final ServerSocket serverSocket = new ServerSocket(portNumber);

    try {
      while (true) {
        System.out.println("新たなクライアントとの接続を待機しています");
        final Socket socket = serverSocket.accept();
        System.out.println("新たにクライアントと接続しました!");
        // クライアントからメッセージを受け取るスレッド
        final Server1ClientThread lastServerThread = new Server1ClientThread(socket,
            // 1つのクライアントからメッセージが来た
            (message, clientId) -> {
              try {
                System.out.print("クライアント(" + clientId + ")から " + message + " を受け取りました");
                sendMessageToAllClient(
                    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()) + ": <" + clientId + "> "
                        + message,
                    serverThreadArrayList);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            },
            // 1つのクライアントとの接続が切れた
            (disconnected) -> {
              try {
                sendMessageToAllClient(disconnected + "さんが退出しました", serverThreadArrayList);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

        serverThreadArrayList.add(lastServerThread);
        lastServerThread.start();
        try {
          sendMessageToAllClient(lastServerThread.threadId() + "さんが参加しました", serverThreadArrayList);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * サーバーに接続しているすべてのクライアントにメッセージを送信する
   */
  static public void sendMessageToAllClient(String message, ArrayList<Server1ClientThread> serverThreadArrayList)
      throws IOException {
    for (final Server1ClientThread serverThread : serverThreadArrayList) {
      if (!serverThread.isDisconnected) {
        serverThread.sendDataToClient(message);
      }
    }
  }
}

/**
 * 1つのクライアントからメッセージを受け取り, 送信するためのスレッド
 */
class Server1ClientThread extends Thread {
  final Socket socket;
  final BiConsumer<String, Long> handler;
  final Consumer<Long> onDisconnect;
  boolean isDisconnected = false;

  ObjectOutputStream serverToClientStream = null;

  Server1ClientThread(
      final Socket socket,
      final BiConsumer<String, Long> handler,
      final Consumer<Long> onDisconnect) {
    System.out.println("Server1ClientThreadを起動します");
    this.socket = socket;
    this.handler = handler;
    this.onDisconnect = onDisconnect;
  }

  @Override
  public void run() {
    try {
      serverToClientStream = new ObjectOutputStream(socket.getOutputStream());

      final ObjectInputStream clientToServerStream = new ObjectInputStream(socket.getInputStream());
      while (true) {
        final String clientToServerData = clientToServerStream.readUTF();
        logWithId("クライアントから " + clientToServerData + "を受け取りました");
        handler.accept(clientToServerData, threadId());
      }
    } catch (IOException e) {
      isDisconnected = true;
      onDisconnect.accept(threadId());
    }
  }

  /**
   * クライアントにデータを送信する
   */
  public void sendDataToClient(final String message) throws IOException {
    // まだ接続していないときは, 送信しない
    if (serverToClientStream == null) {
      return;
    }
    serverToClientStream.writeUTF(message);
    serverToClientStream.flush();
  }

  private void logWithId(final String message) {
    System.out.println("[Server1ClientThread id: " + threadId() + "] " + message);
  }
}
