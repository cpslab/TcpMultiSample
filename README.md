# TCP で複数のクライアントと1つのサーバーがやりとりするチャット コンソールアプリ

## サーバーの起動

```sh
cd ./src
javac --enable-preview --source 20 TcpServer.java
java --enable-preview TcpServer
```

## クライアントの起動

```sh
cd ./src
javac --enable-preview --source 20 TcpClient.java
java --enable-preview TcpClient
```
