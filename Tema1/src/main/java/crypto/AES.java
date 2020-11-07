package crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    private static SecretKeySpec secretKey;
    private static byte[] key;

    public static void setKey(String myKey)
    {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static byte[] encryptBlock(byte[] block)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(block);
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static String encryptECB(String message, String key) {
        pad16(message);
        setKey(key);

        byte[] msgBytes = message.getBytes();

        ByteArrayOutputStream encryption = new ByteArrayOutputStream(message.length());

//        byte[] messageBytes = message.getBytes();

        for (int i = 0; i< message.length(); i+= 16) {
            try {
                encryption.write(Objects.requireNonNull(encryptBlock(Arrays.copyOfRange(msgBytes, i, i + 16))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Base64.getEncoder().encodeToString(encryption.toByteArray());
    }

    public static byte[] decryptBlock(byte[] block)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(block);
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }

    public static String decryptECB(String cryptoText, String key) {
        byte[] bytes = Base64.getDecoder().decode(cryptoText);
        if (bytes.length % 16 != 0) {
            System.out.println("Bad encrypted message!");
            return null;
        }
        setKey(key);

        ByteArrayOutputStream decryptedMessage = new ByteArrayOutputStream(bytes.length);

        for (int i = 0; i < bytes.length; i+= 16) {
            try {
                decryptedMessage.write(Objects.requireNonNull(decryptBlock(Arrays.copyOfRange(bytes, i, i + 16))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return decryptedMessage.toString();

    }

    public static void pad16(String message) {
        if (message.getBytes().length % 16 != 0) {
            message += "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0".substring(0, 16 - message.length()%16);
//            System.out.println("New Message: " + message.length());
        }
    }

    public static byte[] copyBytesXOR (byte[] arr1, byte[] arr2) {
        boolean isArr1;
        byte[] xorArr = (isArr1 = (arr1.length > arr2.length)) ? Arrays.copyOf(arr1, arr1.length) : Arrays.copyOf(arr2, arr2.length);
        if (isArr1) {
            for (int i = 0; i < (arr2.length); i++) {
                xorArr[i] ^= arr2[i];
            }
        }
        else {
            for (int i = 0; i < (arr1.length); i++) {
                xorArr[i] ^= arr1[i];
            }
        }
        return xorArr;
    }

    public static void bytesXOR(byte[] arr1, byte[] arr2) {
        for (int i = 0; i < arr1.length; i++) {
            arr1[i] ^= arr2[i];
        }
    }

    public static String encryptOFB(String message, String key, String iv) {

        pad16(message);
        byte[] ivBytes = Arrays.copyOf(iv.getBytes(), 16);
        setKey(key);
        byte[] msgBytes = message.getBytes();


        ByteArrayOutputStream encryption = new ByteArrayOutputStream(message.length());

        for (int i = 0; i < msgBytes.length; i += 16) {
//            encryption.write();
            ivBytes = encryptBlock(ivBytes);

            byte[] blockXORiv = Arrays.copyOfRange(msgBytes, i, i+16);
            bytesXOR(blockXORiv,  ivBytes);
            try {
                encryption.write(blockXORiv);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Base64.getEncoder().encodeToString(encryption.toByteArray());
    }

    public static String decryptOFB(String cryptoText, String key, String iv) {
        byte[] encodedBytes = Base64.getDecoder().decode(cryptoText);
        if (encodedBytes.length % 16 != 0) {
            System.out.println("Bad encrypted message!");
            return null;
        }
        setKey(key);

        byte[] ivBytes = Arrays.copyOf(iv.getBytes(), 16);


        ByteArrayOutputStream decryptedMessage = new ByteArrayOutputStream(encodedBytes.length);

        for (int i = 0; i < encodedBytes.length; i+= 16) {

            ivBytes = encryptBlock(ivBytes);
            byte[] blockXORiv = Arrays.copyOfRange(encodedBytes, i, i+16);
            bytesXOR(blockXORiv,  ivBytes);
            try {
                decryptedMessage.write(blockXORiv);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return decryptedMessage.toString();

    }


}