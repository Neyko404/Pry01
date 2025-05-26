package WB;

import util.JwtUtil;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;

@ServerEndpoint("/chat")
public class ChatEndpoint {

    private static final Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());
    private static final Map<Session, SecretKey> sessionKeys = new HashMap<>();
    private static final Map<Session, String> sessionUsers = new HashMap<>();
    private static final byte[] iv = new byte[16]; // IV fijo ceros para AES-CBC

    private static final Gson gson = new Gson();

    // Clave fija AES (16 bytes)
    private static final SecretKey fixedKey = new SecretKeySpec("clave-demo-12345".getBytes(StandardCharsets.UTF_8), "AES");

    @OnOpen
    public void onOpen(Session session) {
        try {
            String token = getTokenFromCookies(session);
            if (token == null || !JwtUtil.validarToken(token)) {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Token inválido o ausente."));
                return;
            }

            String username = JwtUtil.obtenerUsername(token);
            // Usar clave fija
            sessionKeys.put(session, fixedKey);
            sessionUsers.put(session, username);
            clients.add(session);

            // Enviar mensaje de bienvenida como JSON cifrado
            Map<String, String> welcomeMsg = new HashMap<>();
            welcomeMsg.put("user", "Sistema");
            welcomeMsg.put("message", "Conexión establecida. Bienvenido " + username + ".");
            welcomeMsg.put("datetime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            String json = gson.toJson(welcomeMsg);
            String encrypted = encryptAES(json, fixedKey);
            session.getBasicRemote().sendText(encrypted);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String encryptedMessage, Session session) {
        try {
            SecretKey key = sessionKeys.get(session);
            String username = sessionUsers.get(session);

            if (key == null || username == null) {
                return;
            }

            String decrypted = decryptAES(encryptedMessage, key);

            // El mensaje que viene ya es JSON con user, message y datetime
            // Lo podemos parsear si quieres, pero acá solo reenviamos con user y datetime del backend
            Map<?, ?> obj = gson.fromJson(decrypted, Map.class);
            String message = (String) obj.get("message");

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            Map<String, String> broadcastMsg = new HashMap<>();
            broadcastMsg.put("user", username);
            broadcastMsg.put("message", message);
            broadcastMsg.put("datetime", timestamp);

            String jsonToSend = gson.toJson(broadcastMsg);

            synchronized (clients) {
                for (Session s : clients) {
                    if (s.isOpen()) {
                        SecretKey targetKey = sessionKeys.get(s);
                        if (targetKey != null) {
                            String encryptedToSend = encryptAES(jsonToSend, targetKey);
                            s.getBasicRemote().sendText(encryptedToSend);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
        sessionKeys.remove(session);
        sessionUsers.remove(session);
    }

    private String encryptAES(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new javax.crypto.spec.IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptAES(String encryptedText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new javax.crypto.spec.IvParameterSpec(iv));
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String getTokenFromCookies(Session session) {
        List<String> tokens = session.getRequestParameterMap().get("token");
        if (tokens != null && !tokens.isEmpty()) {
            return tokens.get(0);
        }
        return null;
    }
}
