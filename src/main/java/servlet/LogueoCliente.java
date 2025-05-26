package servlet;

import dao.ClienteJpaController;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.JwtUtil;
import java.util.Base64;

@WebServlet(name = "LogueoCliente", urlPatterns = {"/logueocliente"})
public class LogueoCliente extends HttpServlet {

    // Método para descifrar AES
    private String descifrarAES(String textoCifrado, String clave) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(clave.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] decodedBytes = Base64.getDecoder().decode(textoCifrado);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);

        return new String(decryptedBytes, "UTF-8");
    }

    // Método para hacer hash SHA-512
    private String hashSHA512(String texto) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] hashBytes = md.digest(texto.getBytes("UTF-8"));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            String userParam = request.getParameter("user");
            String passCifrada = request.getParameter("pass");

            int codiClie;
            try {
                codiClie = Integer.parseInt(userParam);
            } catch (NumberFormatException e) {
                out.println("{\"resultado\":\"error\", \"mensaje\":\"Usuario inválido\"}");
                return;
            }

            String claveAES = "la fe de cuto123"; // 16 caracteres (ojo con el espacio al final)
            String passPlano;
            String passHasheada;

            try {
                passPlano = descifrarAES(passCifrada, claveAES);
                passHasheada = hashSHA512(passPlano);
            } catch (Exception e) {
                e.printStackTrace();
                out.println("{\"resultado\":\"error\", \"mensaje\":\"Error al procesar la contraseña\"}");
                return;
            }

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("com.mycompany_Pry01_war_1.0-SNAPSHOTPU");
            ClienteJpaController clienteDAO = new ClienteJpaController(emf);

            boolean valido = clienteDAO.validar(codiClie, passHasheada);

            if (valido) {
                try {
                    String token = JwtUtil.generarToken(userParam);
                    request.getSession().setAttribute("usuario", userParam);
                    out.println("{\"resultado\":\"ok\",\"token\":\"" + token + "\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    out.println("{\"resultado\":\"error\",\"mensaje\":\"Error al generar token\"}");
                }
            } else {
                out.println("{\"resultado\":\"error\",\"mensaje\":\"Usuario o contraseña incorrectos\"}");
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet para login de cliente con JWT y cifrado AES + hash SHA-512";
    }
}
