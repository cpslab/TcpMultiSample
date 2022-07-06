import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class TcpServer {
  public static int portNumber = 1234;

  public static void main(String[] args) throws IOException {
    final ServerSocket serverSocket = new ServerSocket(portNumber);
    System.out.println("TCPサーバーを起動しました. ポート番号 " + portNumber + " で接続を受け付けています");

    ArrayList<ServerThread> serverThreadArrayList = new ArrayList<ServerThread>();

    while (true) {
      final ServerThread lastServerThread =
          new ServerThread(
              serverSocket,
              new ServerThreadHandler() {
                @Override
                public void onMessage(final String message) {
                  try {
                    for (ServerThread serverThread : serverThreadArrayList) {
                      serverThread.sendMessageToClient(message);
                    }
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                }
              });
      lastServerThread.start();
      serverThreadArrayList.add(lastServerThread);

      while (!lastServerThread.isConnected) {}
    }
  }
}

// 継承よりも, Runnable を実装したほうが良さそう
class ServerThread extends Thread {
  final ServerSocket serverSocket;
  final ServerThreadHandler handler;

  boolean isConnected = false;

  ObjectOutputStream serverToClientStream = null;

  ServerThread(final ServerSocket serverSocket, final ServerThreadHandler handler) {
    System.out.println("ServerThreadを起動します");
    this.serverSocket = serverSocket;
    this.handler = handler;
  }

  @Override
  public void run() {
    try {
      logWithId(" 受付待ちしています");
      final Socket socket = serverSocket.accept();
      logWithId("クライアントと接続しました!");
      isConnected = true;

      serverToClientStream = new ObjectOutputStream(socket.getOutputStream());

      logWithId("クライアントにスレッドIDを送ります");
      sendResponseDataToClient(new AssignedId(getId()));

      final ObjectInputStream clientToServerStream = new ObjectInputStream(socket.getInputStream());
      while (true) {
        final ClientToServerData clientToServerData = (ClientToServerData) clientToServerStream.readObject();
        logWithId("クライアントから " + clientToServerData + "を受け取りました");
        handler.onMessage(clientToServerData.message());
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void sendResponseDataToClient(final ServerToClientData serverToClientData) throws IOException {
    // まだ接続していないときは, 送信しない
    if(serverToClientStream == null) {
      return;
    }
    serverToClientStream.writeObject(serverToClientData);
    serverToClientStream.flush();
  }

  public void sendMessageToClient(final String message) throws IOException {
    sendResponseDataToClient(new NewMessage(message));
  }

  private void logWithId(final String message) {
    System.out.println("[ServerThread id: " + getId() + "] " + message);
  }
}

interface ServerThreadHandler {
  /** メッセージが来たときに呼ばれる */
  void onMessage(final String message);
}
