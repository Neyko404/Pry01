/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlet;

import dao.ClienteJpaController;
import dto.Cliente;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.JwtUtil;

/**
 *
 * @author SASHA
 */
@WebServlet(name = "CambiarClave", urlPatterns = {"/cambiarclave"})
public class CambiarClave extends HttpServlet {

    private String descifrarAES(String textoCifrado, String clave) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(clave.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decodedBytes = Base64.getDecoder().decode(textoCifrado);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, "UTF-8");
    }

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
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            
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
        response.setContentType("text/plain;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            // Leer claves cifradas enviadas desde frontend
            String claveActualCifrada = request.getParameter("claveActual");
            String nuevaClaveCifrada = request.getParameter("nuevaClave");
            String confirmaClaveCifrada = request.getParameter("confirmaClave");

            // Obtener token de cookie para saber usuario
            String token = null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if (token == null || !JwtUtil.validarToken(token)) {
                out.print("Token inválido o no autorizado");
                return;
            }

            String user = JwtUtil.obtenerUsername(token);
            if (user == null) {
                out.print("No se pudo extraer usuario del token");
                return;
            }

            int codiClie;
            try {
                codiClie = Integer.parseInt(user);
            } catch (NumberFormatException e) {
                out.print("Código de usuario inválido");
                return;
            }

            // Clave AES que usas (igual en frontend)
            String claveAES = "la fe de cuto123";

            // Descifrar las claves
            String claveActualDescifrada = descifrarAES(claveActualCifrada, claveAES);
            String nuevaClaveDescifrada = descifrarAES(nuevaClaveCifrada, claveAES);
            String confirmaClaveDescifrada = descifrarAES(confirmaClaveCifrada, claveAES);

            // Validar que nueva y confirma sean iguales
            if (!nuevaClaveDescifrada.equals(confirmaClaveDescifrada)) {
                out.print("La nueva clave y la confirmación no coinciden");
                return;
            }

            // Hashear la clave actual para comparar con la BD
            String hashClaveActual = hashSHA512(claveActualDescifrada);

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("com.mycompany_Pry01_war_1.0-SNAPSHOTPU");
            ClienteJpaController clienteDAO = new ClienteJpaController(emf);
            Cliente cliente = clienteDAO.findCliente(codiClie);

            if (cliente == null) {
                out.print("Usuario no encontrado");
                return;
            }

            if (!cliente.getPassClie().equals(hashClaveActual)) {
                out.print("La clave actual es incorrecta");
                return;
            }

            // Hash de nueva clave y guardar
            String hashNuevaClave = hashSHA512(nuevaClaveDescifrada);
            cliente.setPassClie(hashNuevaClave);
            clienteDAO.edit(cliente);

            out.print("ok");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("Error al cambiar la clave");
        }
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
