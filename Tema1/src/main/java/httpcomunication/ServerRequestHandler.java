package httpcomunication;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import crypto.AES;
import org.json.JSONObject;

import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

public class ServerRequestHandler implements HttpHandler {

    ClientHttp parent;
    private IvParameterSpec ivParam;


    public ServerRequestHandler(ClientHttp parent) {
        this.parent = parent;
    }

    @Override
    public void handle(HttpExchange httpExchange)  {
        String msg = null;
        if("GET".equals(httpExchange.getRequestMethod())) {

        }else if("POST".equals(httpExchange.getRequestMethod())) {
            msg = handlePostRequest(httpExchange);
        }
        handleResponse(httpExchange, msg);
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
        DebugUtil.info("Am primit de la server: " + message.toString());
        String decrypted = AES.decryptECB(message.toString(), parent.serverKey);
        DebugUtil.info("Am decriptat: " + decrypted);
        JSONObject json = new JSONObject(decrypted);
        try {
            parent.otherClient = new URL(json.get("otherClient").toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        parent.mode = json.get("mode").toString();
        parent.clientKey = json.get("key").toString();

        if (parent.mode.equals("ECB")) {
            return AES.encryptECB("ALLGOOD", parent.serverKey);
        }
        else if (parent.mode.equals("OFB")) {
            parent.iv = json.get("iv").toString();
            ivParam = new IvParameterSpec(Base64.getDecoder().decode(parent.iv));
            return AES.encryptOFB("ALLGOOD", parent.serverKey, parent.iv);
        }
        else return "Bad";
//        else if (mode.equals("OFB")) {
//            return AES.encryptOFB(clientKey, serverKey, "asdsad");
//        }
    }

    private void handleResponse(HttpExchange httpExchange, String response)  {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpExchange.getResponseBody()));

        try {
            writer.write(response);
            httpExchange.sendResponseHeaders(200, response.length());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
