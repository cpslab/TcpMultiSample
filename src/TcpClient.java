import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

public class TcpClient {
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    final InetAddress localhost = InetAddress.getLocalHost();
    System.out.println(
        "クライアントを起動しました. これから " + localhost + " のポート番号 " + TcpServer.portNumber + "に接続します");
    final Socket socket = new Socket(localhost, TcpServer.portNumber);

    final ObjectInputStream serverToClientStream = new ObjectInputStream(socket.getInputStream());
    final ResponseData response = (ResponseData) serverToClientStream.readObject();
    System.out.println("サーバーからメッセージがきた " + response);

    System.out.println("これから入力した文字をサーバーに送ります");

    final Scanner consoleInputScanner = new Scanner(System.in);

    final ObjectOutputStream clientToServerStream =
        new ObjectOutputStream(socket.getOutputStream());
    while (true) {
      final String inputtedMessage = consoleInputScanner.nextLine();
      final RequestData requestData = new RequestData(inputtedMessage, new Date());
      System.out.println("サーバーに " + requestData +  " を送ります");
      clientToServerStream.writeObject(requestData);
      clientToServerStream.flush();
    }
  }
}
