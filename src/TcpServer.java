import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {
  public static int portNumber = 1234;

  public static void main(String[] args) throws IOException {
    final ServerSocket serverSocket = new ServerSocket(portNumber);
    System.out.println("TCPサーバーを起動しました. ポート番号 " + portNumber + " で接続を受け付けています");
    while (true) {
      final ServerThread serverThread = new ServerThread(serverSocket);
      serverThread.start();
      while (true) {
        if (serverThread.isConnected) {
          break;
        }
      }
    }
  }
}

class ServerThread extends Thread {
  final ServerSocket serverSocket;

  boolean isConnected = false;

  ServerThread(final ServerSocket serverSocket) {
    System.out.println("ServerThreadを起動します");
    this.serverSocket = serverSocket;
  }

  @Override
  public void run() {
    try {
      logWithId(" 受付待ちしています");
      final Socket socket = serverSocket.accept();
      logWithId("クライアントと接続しました!");
      isConnected = true;

      final ObjectOutputStream serverToClientStream =
          new ObjectOutputStream(socket.getOutputStream());

      logWithId("クライアントにスレッドIDを送ります");
      serverToClientStream.writeObject(new ResponseData(getId()));
      serverToClientStream.flush();

      final ObjectInputStream clientToServerStream = new ObjectInputStream(socket.getInputStream());
      while (true) {
        final RequestData requestData = (RequestData) clientToServerStream.readObject();
        logWithId("クライアントから " + requestData + "を受け取りました");
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void logWithId(final String message) {
    System.out.println("[ServerThread id: " + getId() + "] " + message);
  }
}
