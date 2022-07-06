import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.LongStream;

public class TcpServer {
  public static int portNumber = 1234;

  public static void main(String[] args) throws IOException {

    final ServerThreadManager serverThreadManager = new ServerThreadManager();
    serverThreadManager.listen(
        portNumber,
        new ServerThreadManagerHandler() {
          @Override
          public void onConnect(long connectedId) {
            final long[] ids = serverThreadManager.getIds().toArray();
            for (long id : ids) {
              try {
                if (id == connectedId) {
                  serverThreadManager.sendMessage(id, new Welcome(connectedId, ids));

                } else {
                  serverThreadManager.sendMessage(id, new NewClient(connectedId));
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          }

          @Override
          public void onMessage(ClientToServerData message, long clientId) {
            serverThreadManager
                .getIds()
                .forEach(
                    id -> {
                      try {
                        serverThreadManager.sendMessage(
                            id, new NewMessage(message.message(), new Date()));
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                    });
          }
        });
  }
}

class ServerThreadManager {
  final ArrayList<ServerThread> serverThreadArrayList = new ArrayList<>();

  /** 接続を受け付ける */
  public void listen(final int portNumber, final ServerThreadManagerHandler handler)
      throws IOException {
    final ServerSocket serverSocket = new ServerSocket(portNumber);
    System.out.println("TCPサーバーを起動しました. ポート番号 " + portNumber + " で接続を受け付けています");

    new Thread(
            () -> {
              while (true) {
                // 接続待ちのスレッド
                final ServerThread lastServerThread =
                    new ServerThread(serverSocket, handler::onMessage);
                lastServerThread.start();

                serverThreadArrayList.add(lastServerThread);
                while (true) {
                  if (lastServerThread.isConnected) {
                    handler.onConnect(lastServerThread.getId());
                    break;
                  }
                }
              }
            })
        .start();
  }

  public LongStream getIds() {
    return serverThreadArrayList.stream().filter(Thread::isAlive).mapToLong(Thread::getId);
  }

  public void sendMessage(long id, ServerToClientData message) throws Exception {
    for (final ServerThread serverThread : serverThreadArrayList) {
      if (serverThread.getId() == id) {
        serverThread.sendDataToClient(message);
        return;
      }
    }
    throw new Exception("指定したID(" + id + ")のスレッドに送信できなかった");
  }
}

interface ServerThreadManagerHandler {
  /** 新たにクライアントと接続できた */
  void onConnect(final long id);

  /** クライアントからメッセージがきた */
  void onMessage(final ClientToServerData message, final long id);
}

// 継承よりも, Runnable を実装したほうが良さそう
class ServerThread extends Thread {
  final ServerSocket serverSocket;
  final BiConsumer<ClientToServerData, Long> handler;
  boolean isConnected = false;

  ObjectOutputStream serverToClientStream = null;

  ServerThread(
      final ServerSocket serverSocket, final BiConsumer<ClientToServerData, Long> handler) {
    System.out.println("ServerThreadを起動します");
    this.serverSocket = serverSocket;
    this.handler = handler;
  }

  @Override
  public void run() {
    try {
      logWithId(" クライアントとの接続を待機します");
      final Socket socket = serverSocket.accept();
      logWithId("クライアントと接続しました!");
      isConnected = true;
      serverToClientStream = new ObjectOutputStream(socket.getOutputStream());

      final ObjectInputStream clientToServerStream = new ObjectInputStream(socket.getInputStream());
      while (true) {
        final ClientToServerData clientToServerData =
            (ClientToServerData) clientToServerStream.readObject();
        logWithId("クライアントから " + clientToServerData + "を受け取りました");
        handler.accept(clientToServerData, getId());
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void sendDataToClient(final ServerToClientData serverToClientData) throws IOException {
    // まだ接続していないときは, 送信しない
    if (serverToClientStream == null) {
      return;
    }
    serverToClientStream.writeObject(serverToClientData);
    serverToClientStream.flush();
  }

  private void logWithId(final String message) {
    System.out.println("[ServerThread id: " + getId() + "] " + message);
  }
}
