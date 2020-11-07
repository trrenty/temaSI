package httpcomunication;

import com.sun.net.httpserver.HttpExchange;
import crypto.AES;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientNotifier extends ClientConnectionEstablisher {


    public ClientNotifier(KeyManager keyManager) {
        super(keyManager);

    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String msg = null;
        if("GET".equals(httpExchange.getRequestMethod())) {

        }else if("POST".equals(httpExchange.getRequestMethod())) {
                msg = handlePostRequest(httpExchange);
        }
        super.handleResponse(httpExchange, msg);
        DebugUtil.info("km.done = " + km.done.get());
        if (km.done.get() == 2) {
            sendMessage(AES.encryptECB("CONTINUE", km.key), km.clients);
            km.done.set(0);
        }
    }

    @Override
    protected String handlePostRequest(HttpExchange httpExchange) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
        StringBuilder message = new StringBuilder();
        String partialMessage;
        try {
            while ((partialMessage = reader.readLine()) != null) {
                message.append(partialMessage);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DebugUtil.info("Am primit: " + message.toString());
        String decrypt = AES.decryptECB(message.toString(), km.key).trim().toUpperCase();
        DebugUtil.info("Decrypt: " + decrypt);
        if (decrypt.equals("SENT") || decrypt.equals("RECEIVED")) {
            km.done.incrementAndGet();
        }
        else if (decrypt.equals("DONE"))
        {
            km.done.set(0);
            DebugUtil.info("The transfer is done!");
        }
        return "OK";
    }
}
