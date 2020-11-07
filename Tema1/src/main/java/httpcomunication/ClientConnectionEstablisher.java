package httpcomunication;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import crypto.AES;
import org.json.JSONObject;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class ClientConnectionEstablisher implements HttpHandler {
    KeyManager km;
    IvParameterSpec ivParams;


    public ClientConnectionEstablisher(KeyManager km) {
        this.km = km;
    }


    public void setMode (String mode) {
        if (km.mode == null) {
            km.mode = mode;
        }
        else if (!km.mode.equals(mode)) {
            km.mode = (ThreadLocalRandom.current().nextBoolean()) ? "ECB" : "OFB";
        }
        DebugUtil.info("Modul de criptare a fost schimbat la " + km.mode);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String msg = null;
        if("GET".equals(httpExchange.getRequestMethod())) {

        }else if("POST".equals(httpExchange.getRequestMethod())) {
            if (km.clients.size() >= 2) {
                msg = "fuck off";
            }
            else {
                msg = handlePostRequest(httpExchange);
            }
        }
        handleResponse(httpExchange, msg);
        if (km.clients.size() == 2 && !km.connectionSet) {
            linkClients();
            km.connectionSet = true;
        }
    }

    private void linkClients() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(128); // for example
            SecretKey secretKey = keyGen.generateKey();
            ivParams = null;
            if (km.mode.equals("OFB")) {
                SecureRandom randomSecureRandom = SecureRandom.getInstance("SHA1PRNG");
                byte[] iv = new byte[16];
                randomSecureRandom.nextBytes(iv);
                ivParams = new IvParameterSpec(iv);
            }
            String iv = (ivParams != null) ? Base64.getEncoder().encodeToString(ivParams.getIV()) : "none";
            sendLinkMessage("{ \"mode\": \"" + km.mode + "\", \"key\": \""+ Base64.getEncoder().encodeToString(secretKey.getEncoded()) +"\", \"iv\":\""+iv+"\"}", km.clients);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private String handleGetRequest(HttpExchange httpExchange) {
        return null;
    }

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

        JSONObject json = new JSONObject(message.toString());
        if (km.clients.size() < 2) {
            try {
                km.clients.add(new URL("http", json.get("ip").toString(), Integer.parseInt(json.get("port").toString()), ""));
                DebugUtil.info("Adaug client: " + km.clients.get(km.clients.size() - 1));
                setMode(json.get("mode").toString().toUpperCase());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        json.put("msg", "am primit");

        return json.toString();
    }

    private void sendLinkMessage(String message, List<URL> urls) {
        HttpURLConnection connection = null;
        DebugUtil.info("Trimit clientilor informatiile necesare: ");
        for (URL client :
                urls) {
            try {
                connection = (HttpURLConnection)  client.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                PrintWriter wr = new PrintWriter(connection.getOutputStream());

                JSONObject json = new JSONObject(message);
                URL otherClient = urls.get(0).equals(client) ? urls.get(1) : urls.get(0);
                json.put("otherClient", otherClient);
                wr.print(AES.encryptECB(json.toString(), km.key));
                wr.close();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                if (km.mode.equals("ECB"))
                    DebugUtil.info("Mesaj decriptat de la " + client.toString() + ": " +AES.decryptECB(reader.readLine(), km.key));
                else if (km.mode.equals("OFB")) {
                    DebugUtil.info("Mesaj decriptat de la " + client.toString() + ": " +AES.decryptOFB(reader.readLine(), km.key, Base64.getEncoder().encodeToString(ivParams.getIV())));
//                    DebugUtil.info(Base64.getEncoder().encodeToString(ivParams.getIV()));
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        sendMessage(AES.encryptECB("START", km.key), Collections.singletonList(km.clients.get(0)));
    }

    protected void sendMessage(String message, List<URL> urls) {
        HttpURLConnection connection = null;
        DebugUtil.info("Trimit la clienti mesajul: " + message);
        for (URL client :
                urls) {
            try {
                connection = (HttpURLConnection)  new URL(client.toString() +  "/exchange").openConnection();
//                DebugUtil.info(connection.getURL());
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                PrintWriter wr = new PrintWriter(connection.getOutputStream());
                wr.print(message);
                wr.flush();
                wr.close();
                connection.getInputStream().close();
                connection.getResponseCode();
//                connection.getInputStream().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void handleResponse(HttpExchange httpExchange, String response)  {
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
