package httpcomunication;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import crypto.AES;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileTransferReceiver implements HttpHandler {
    private final ClientHttp parent;
    private boolean ok = false;

    public FileTransferReceiver(ClientHttp parent) {
       this.parent = parent;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
            String msg = null;
            if("GET".equals(httpExchange.getRequestMethod())) {

            }else if("POST".equals(httpExchange.getRequestMethod())) {
                msg = handlePostRequest(httpExchange);
            }
            handleResponse(httpExchange, msg);
            if (ok)  {
                ok = false;
                sendMessage(AES.encryptECB("RECEIVED", parent.serverKey), new URL("http://127.0.0.1:8001/notify"));
            }
    }

    private void handleResponse(HttpExchange httpExchange, String msg) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpExchange.getResponseBody()));

        try {
            writer.write(msg);
            httpExchange.sendResponseHeaders(200, msg.length());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message, URL url) {
        DebugUtil.info("Trimit lui " + url.toString() + " urmatorul mesaj: " + message);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)  url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            PrintWriter wr = new PrintWriter(connection.getOutputStream());
            wr.print(message);
            wr.flush();
            wr.close();
            connection.getInputStream().close();
            connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String handlePostRequest(HttpExchange httpExchange)  {
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
        DebugUtil.info("Mesaj primit de la " + httpExchange.getRemoteAddress().toString() + ": " + message.toString());
        String possibleServer =AES.decryptECB(message.toString(), parent.serverKey).trim().toUpperCase();
        if (possibleServer.equals("START") || possibleServer.equals("CONTINUE")) {
            DebugUtil.info("Decriptat: " + possibleServer);
        }
        else if (parent.mode.equals("ECB")) {
            DebugUtil.info("Decriptat: " + AES.decryptECB(message.toString(), parent.clientKey));
            ok = true;
        }
        else {
            DebugUtil.info("Decriptat: " + AES.decryptOFB(message.toString(), parent.clientKey, parent.iv));
            ok = true;
        }
        return "OK";
    }
}
