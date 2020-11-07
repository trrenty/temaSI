package httpcomunication;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class KeyManager {

    protected volatile List<URL> clients = new ArrayList<>();
    protected volatile String mode = null;
    protected volatile String key;
    protected volatile AtomicInteger done = new AtomicInteger(0);
    protected volatile boolean connectionSet = false;

    public void init() {
        DebugUtil.info("Introduceti K3: ");
        key = new Scanner(System.in).nextLine();

        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
            server.createContext("/connect", new ClientConnectionEstablisher(this));
            server.createContext("/notify", new ClientNotifier(this));

            ExecutorService executor = Executors.newSingleThreadExecutor();

            server.setExecutor(executor);

            server.start();

            DebugUtil.info(" Server started on port 8001");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        KeyManager km = new KeyManager();
        km.init();
    }
}
