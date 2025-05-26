package util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class SeguridadUtil {

    public static String descifrarAES(String textoCifrado, String clave) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(clave.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] bytesDescifrados = cipher.doFinal(Base64.getDecoder().decode(textoCifrado));
        return new String(bytesDescifrados, "UTF-8");
    }

    // HASH SHA-512
    public static String hashSHA512(String texto) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = md.digest(texto.getBytes("UTF-8"));

        // Convertir a hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
     // Para testear el flujo completo
    public static void main(String[] args) {
        try {
            String clave = "la fe de cuto123"; // 16 caracteres
            String contrasena = "1234";

            // 1. Cifrado AES (simula el frontend)
            SecretKeySpec keySpec = new SecretKeySpec(clave.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(contrasena.getBytes("UTF-8"));
            String textoCifrado = Base64.getEncoder().encodeToString(encrypted);
            System.out.println("🔐 Contraseña cifrada (AES): " + textoCifrado);

            // 2. Descifrar AES (simula el backend)
            String textoPlano = descifrarAES(textoCifrado, clave);
            System.out.println("🔓 Contraseña descifrada: " + textoPlano);

            // 3. Hasheo SHA-512 (guardar en base de datos)
            String hash = hashSHA512(textoPlano);
            System.out.println("🔒 Contraseña hasheada (SHA-512): " + hash);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
