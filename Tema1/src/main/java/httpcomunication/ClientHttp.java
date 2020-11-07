package httpcomunication;

import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHttp {
    protected volatile String serverKey;
    protected volatile String clientKey;
    protected volatile String mode = null;
    protected volatile String iv;
    protected volatile URL otherClient;

    public ClientHttp() {
        init();
    }

    protected void init() {
        Scanner sc = new Scanner(System.in);
        DebugUtil.info("Introduceti portul serverului vostru: ");
        String port = sc.nextLine();
        DebugUtil.info("Introduceti K3:");
        serverKey = sc.nextLine();
        DebugUtil.info("Introduceti modul de criptare dorit: ");
        String desiredMode = sc.nextLine();

        startServer(port);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(new URL("http://localhost:8001/"), "connect").openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            String msg = "{ \"ip\": \"127.0.0.1\"," +
                    " \"port\": \"" + port + "\"," +
                    " \"mode\": \""+ desiredMode.trim().toUpperCase()+"\" }";
            DebugUtil.info("Trimit serverului informatiile: " + msg);
//            connection.setFixedLengthStreamingMode(msg.getBytes().length);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            PrintWriter wr = new PrintWriter(connection.getOutputStream());

            wr.print(msg);
            wr.flush();
            wr.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            DebugUtil.info("Am primit de la server: " + reader.readLine());

            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void startServer(String port) {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", Integer.parseInt(port)), 0);
            server.createContext("/", new ServerRequestHandler(this));
            server.createContext("/exchange", new FileTransfer(this));

            ExecutorService executor = Executors.newFixedThreadPool(1);

            server.setExecutor(executor);

            server.start();

            DebugUtil.info(" Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientHttp clientHttp = new ClientHttp();
    }
}
