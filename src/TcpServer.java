import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TcpServer {
  public static int portNumber = 1234;

  public static void main(String[] args) throws IOException {

    final ServerThreadManager serverThreadManager = new ServerThreadManager();
    serverThreadManager.listen(
        portNumber,
        new ServerThreadManagerHandler() {
          @Override
          public void onConnect(long connectedId) {
            final long[] ids = serverThreadManager.getIdsAndRemoveNotAlive();
            for (long id : ids) {
              try {
                if (id == connectedId) {
                  serverThreadManager.sendMessage(id, new Welcome(connectedId, ids, new Date()));
                } else {
                  serverThreadManager.sendMessage(id, new NewClient(connectedId, new Date()));
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
          }

          @Override
          public void onDisconnect(long disconnected) {
            try {
              for (final long id : serverThreadManager.getIdsAndRemoveNotAlive()) {
                serverThreadManager.sendMessage(id, new LeaveClient(disconnected, new Date()));
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }

          @Override
          public void onMessage(ClientToServerData message, long clientId) {
            try {
              final long[] ids = serverThreadManager.getIdsAndRemoveNotAlive();
              if(message instanceof PrivateMessage privateMessage) {
                System.out.println(Arrays.toString(ids) + "  " + privateMessage.id());
                try {
                  serverThreadManager.sendMessage(privateMessage.id(), new NewPrivateMessage(clientId, privateMessage.message(), new Date()));
                } catch (ClientNotFound e) {
                  serverThreadManager.sendMessage(clientId, new InvalidPrivateId(privateMessage.id(), ids, new Date()));
                }
                return;
              }
              if(message instanceof GlobalMessage globalMessage) {
                for (final long id : serverThreadManager.getIdsAndRemoveNotAlive()) {
                  serverThreadManager.sendMessage(
                          id, new NewMessage(clientId, globalMessage.message(), new Date()));
                }
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
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
              try {
                while (true) {
                  System.out.println("新たなクライアントとの接続を待機しています");
                  final Socket socket = serverSocket.accept();
                  System.out.println("新たにクライアントと接続しました!");
                  // クライアントからメッセージを受け取るスレッド
                  final ServerThread lastServerThread =
                      new ServerThread(socket, handler::onMessage, handler::onDisconnect);

                  serverThreadArrayList.add(lastServerThread);
                  lastServerThread.start();
                  handler.onConnect(lastServerThread.threadId());
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .start();
  }

  /** 接続がきれたクライアントのスレッドを取り除き, 今も接続中のクライアントのIDを返す */
  public long[] getIdsAndRemoveNotAlive() {
    serverThreadArrayList.removeIf(
        serverThread -> serverThread.isDisconnected);
    return serverThreadArrayList.stream().mapToLong(Thread::threadId).toArray();
  }

  public void sendMessage(long id, ServerToClientData message) throws ClientNotFound, IOException {
    for (final ServerThread serverThread : serverThreadArrayList) {
      if (serverThread.threadId() == id) {
        serverThread.sendDataToClient(message);
        return;
      }
    }
    throw new ClientNotFound(id );
  }
}

class ClientNotFound extends Exception {
  ClientNotFound(final long id) {
    super("指定したID(" + id + ")のスレッドに送信できなかった");
  }
}

interface ServerThreadManagerHandler {
  /** 新たにクライアントと接続できた */
  void onConnect(final long id);

  /** クライアントとの接続が切れた */
  void onDisconnect(final long id);

  /** クライアントからメッセージがきた */
  void onMessage(final ClientToServerData message, final long id);
}

/** クライアントからメッセージを受け取り, 送信するためのスレッド */
class ServerThread extends Thread {
  final Socket socket;
  final BiConsumer<ClientToServerData, Long> handler;

  final Consumer<Long> onDisconnect;
  boolean isDisconnected = false;

  ObjectOutputStream serverToClientStream = null;

  ServerThread(
      final Socket socket,
      final BiConsumer<ClientToServerData, Long> handler,
      final Consumer<Long> onDisconnect) {
    System.out.println("ServerThreadを起動します");
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
        final ClientToServerData clientToServerData =
            (ClientToServerData) clientToServerStream.readObject();
        logWithId("クライアントから " + clientToServerData + "を受け取りました");
        handler.accept(clientToServerData, threadId());
      }
    } catch (IOException | ClassNotFoundException e) {
      isDisconnected = true;
      onDisconnect.accept(threadId());
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
    System.out.println("[ServerThread id: " + threadId() + "] " + message);
  }
}

