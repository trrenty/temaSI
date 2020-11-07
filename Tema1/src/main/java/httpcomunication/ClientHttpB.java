package httpcomunication;

import com.sun.net.httpserver.HttpServer;
import org.checkerframework.checker.units.qual.C;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHttpB extends ClientHttp{
    public ClientHttpB() {
        super();
    }
    @Override
    protected void startServer(String port) {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", Integer.parseInt(port)), 0);
            server.createContext("/", new ServerRequestHandler(this));
            server.createContext("/exchange", new FileTransferReceiver(this));

            ExecutorService executor = Executors.newFixedThreadPool(1);

            server.setExecutor(executor);

            server.start();

            DebugUtil.info(" Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClientHttpB b = new ClientHttpB();
    }

}
