package httpcomunication;

import com.google.common.io.Resources;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import crypto.AES;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class FileTransfer implements HttpHandler {

    ClientHttp parent;
    private byte[] fileArray;
    private int lastByte = 0;
    private boolean start = false;
    private boolean continuu = false;
    private boolean done = false;

    public FileTransfer(ClientHttp parent) {
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
        if (start) {
            try {
                start = false;
                initFileArray();
                sendTenBlocks();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        else if (continuu) {
            sendTenBlocks();
            continuu = false;
        }
        if (done)
            sendMessage(AES.encryptECB("DONE", parent.serverKey), new URL("http://127.0.0.1:8001/notify"));

        else
            sendMessage(AES.encryptECB("SENT", parent.serverKey), new URL("http://127.0.0.1:8001/notify"));
    }

    private void handleResponse(HttpExchange httpExchange, String msg) {

    }

    private String handlePostRequest(HttpExchange httpExchange) {
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
        try {
            httpExchange.sendResponseHeaders(200, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        httpExchange.close();
        DebugUtil.info("Mesaj primit de la " + httpExchange.getRemoteAddress().toString() + ": " + message.toString());
        if (AES.decryptECB(message.toString(), parent.serverKey).trim().toUpperCase().equals("START")) {
            start = true;
            DebugUtil.info("Decrypt: START");
        }
        else if (AES.decryptECB(message.toString(), parent.serverKey).trim().toUpperCase().equals("CONTINUE")) {
            continuu = true;
            DebugUtil.info("Decrypt: CONTINUE");
        }
        return null;
    }

    private void sendTenBlocks() throws MalformedURLException {
        String message = null;
        if (fileArray.length - lastByte <= 160) {
            message = new String(Arrays.copyOfRange(fileArray, lastByte, fileArray.length));
            DebugUtil.info("Done!");
            done = true;
        }
        else {
            message = new String(Arrays.copyOfRange(fileArray, lastByte, lastByte += 160));
        }
        if (parent.mode.equals("ECB")) {
            sendMessage(AES.encryptECB(message, parent.clientKey), new URL(parent.otherClient.toString()+"/exchange"));
        }
        else if (parent.mode.equals("OFB")) {
            sendMessage(AES.encryptOFB(message, parent.clientKey, parent.iv), new URL(parent.otherClient.toString()+"/exchange"));
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

    private void initFileArray() {
        try {
            fileArray = Resources.toByteArray(Resources.getResource("in.txt"));
            DebugUtil.info("Fisier pregatit");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
