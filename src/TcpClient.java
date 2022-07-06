import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.function.Consumer;

public class TcpClient {
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    final InetAddress localhost = InetAddress.getLocalHost();
    System.out.println(
        "クライアントを起動しました. これから " + localhost + " のポート番号 " + TcpServer.portNumber + "に接続します");
    final Socket socket = new Socket(localhost, TcpServer.portNumber);

    final ObjectInputStream serverToClientStream = new ObjectInputStream(socket.getInputStream());
    final ServerToClientData response = (ServerToClientData) serverToClientStream.readObject();
    System.out.println("サーバーからメッセージがきた " + response);

    System.out.println("これから入力した文字をサーバーに送ります");

    setServerMessageCallback(
        serverToClientStream,
        (messageFromServer) -> {
          System.out.println("サーバーからメッセージがきた " + messageFromServer);
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
}

class InputThread implements Runnable {

  @Override
  public void run() {}
}
