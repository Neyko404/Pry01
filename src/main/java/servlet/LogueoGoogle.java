/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlet;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.*;

/**
 *
 * @author DELL
 */
@WebServlet(name = "LogueoGoogle", urlPatterns = {"/logueogoogle"})
public class LogueoGoogle extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();

        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String jsonString = sb.toString();

        String idTokenString = "";
        try {
            JsonObject json = new Gson().fromJson(jsonString, JsonObject.class);
            idTokenString = json.get("id_token").getAsString();
        } catch (Exception e) {
            out.println("{\"resultado\":\"error\"}");
            return;
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("332844715861-m6754d87dt29ufhbn9hmaeuc22ntlca6.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String nombre = (String) payload.get("given_name");  // nombre
                String apellido = (String) payload.get("family_name"); // apellido
                // Puedes llenar más campos si quieres

                // Abrir conexión a base de datos
                Connection conn = null;
                PreparedStatement psSelect = null;
                PreparedStatement psInsert = null;
                ResultSet rs = null;

                try {
                    // Aquí adapta a tu forma de conectar (Driver, URL, usuario, pass)
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pry01", "root", "");

                    // Verificar si existe email
                    String sqlSelect = "SELECT codiClie FROM cliente WHERE email = ?";
                    psSelect = conn.prepareStatement(sqlSelect);
                    psSelect.setString(1, email);
                    rs = psSelect.executeQuery();

                    if (!rs.next()) {
                        // No existe usuario, insertar nuevo
                        String sqlInsert = "INSERT INTO cliente (ndniClie, appaClie, apmaClie, nombClie, fechNaciClie, logiClie, passClie, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                        psInsert = conn.prepareStatement(sqlInsert);

                        psInsert.setString(1, "00000000"); // DNI ficticio o vacío
                        psInsert.setString(2, apellido != null ? apellido : ""); // appaClie (apellido paterno)
                        psInsert.setString(3, ""); // apmaClie (apellido materno)
                        psInsert.setString(4, nombre != null ? nombre : ""); // nombre
                        psInsert.setDate(5, java.sql.Date.valueOf("1900-01-01")); // fecha nacimiento ficticia
                        psInsert.setString(6, nombre); // logiClie (usuario/login) - usamos el email
                        psInsert.setString(7, "google_auth"); // passClie - contraseña vacía o fija para Google
                        psInsert.setString(8, email); // email

                        psInsert.executeUpdate();
                    }

                    // Generar token JWT
                    String token = util.JwtUtil.generarToken(email);

                    // Crear sesión y devolver JSON con token
                    HttpSession sesion = request.getSession();
                    sesion.setAttribute("usuario", email);

                    out.println("{\"resultado\":\"ok\",\"token\":\"" + token + "\"}");

                } catch (Exception e) {
                    e.printStackTrace();
                    out.println("{\"resultado\":\"error\"}");
                } finally {
                    if (rs != null) try {
                        rs.close();
                    } catch (Exception e) {
                    }
                    if (psSelect != null) try {
                        psSelect.close();
                    } catch (Exception e) {
                    }
                    if (psInsert != null) try {
                        psInsert.close();
                    } catch (Exception e) {
                    }
                    if (conn != null) try {
                        conn.close();
                    } catch (Exception e) {
                    }
                }

            } else {
                out.println("{\"resultado\":\"error\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.println("{\"resultado\":\"error\"}");
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
