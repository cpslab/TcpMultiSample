import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class TcpClient {
    public static void main(String[] args) throws IOException {
        final InetAddress localhost = InetAddress.getLocalHost();
        System.out.println(
                "クライアントを起動しました. これから " + localhost + " のポート番号 " + TcpServer.portNumber + "に接続します");
        final Socket socket = new Socket(localhost, TcpServer.portNumber);

        final ObjectInputStream serverToClientStream = new ObjectInputStream(socket.getInputStream());
        final ObjectOutputStream clientToServerStream = new ObjectOutputStream(socket.getOutputStream());

        new Thread(
                () -> {
                    try {
                        while (true) {
                            System.out.println(serverToClientStream.readUTF());
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .start();

        final Scanner consoleInputScanner = new Scanner(System.in);

        while (true) {
            // コンソールから入力を受け付ける
            final String message = consoleInputScanner.nextLine();
            // サーバーにメッセージを送る
            clientToServerStream.writeUTF(message);
            clientToServerStream.flush();
        }
    }
}
