package servlet;

import dao.ClienteJpaController;
import java.io.IOException;
import java.io.PrintWriter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.JwtUtil;

@WebServlet(name = "LogueoCliente", urlPatterns = {"/logueocliente"})
public class LogueoCliente extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            String userParam = request.getParameter("user");
            String pass = request.getParameter("pass");

            int codiClie;
            try {
                codiClie = Integer.parseInt(userParam);
            } catch (NumberFormatException e) {
                out.println("{\"resultado\":\"error\", \"mensaje\":\"Usuario inválido\"}");
                return;
            }

            EntityManagerFactory emf = Persistence.createEntityManagerFactory("com.mycompany_Pry01_war_1.0-SNAPSHOTPU");
            ClienteJpaController clienteDAO = new ClienteJpaController(emf);

            boolean valido = clienteDAO.validar(codiClie, pass);

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
        return "Servlet para login de cliente con JWT";
    }
}
