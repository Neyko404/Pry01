<%@ page import="net.sf.jasperreports.engine.JasperRunManager" %>
<%@ page import="java.io.File" %>
<%@ page import="java.sql.DriverManager" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="util.JwtUtil" %> <!-- Importa tu clase de validación -->
<%@ page contentType="application/pdf" %>
<%
    String token = request.getParameter("token");

    if (token == null || !JwtUtil.validarToken(token)) {
        response.setContentType("text/plain");
        response.getWriter().println("Token inválido o expirado.");
        return;
    }

    // Conexión a tu base de datos (ajusta según tu config actual)
    String url = "jdbc:mysql://localhost:3306/pry01?useSSL=false"; // o tu base
    String usuario = "root";
    String clave = "";
    Class.forName("com.mysql.cj.jdbc.Driver"); // Para MySQL
    Connection cn = DriverManager.getConnection(url, usuario, clave);

    // Ruta del archivo .jasper
    File reporte = new File(application.getRealPath("/reportes/reporteClientes.jasper"));

    // Parámetros si necesitas enviarlos al reporte
    Map<String, Object> parametros = new HashMap<>();

    // Generar el PDF directamente al response
    byte[] bytes = JasperRunManager.runReportToPdf(reporte.getPath(), parametros, cn);
    response.setContentLength(bytes.length);
    response.getOutputStream().write(bytes);
    response.getOutputStream().flush();
    response.getOutputStream().close();

    cn.close();
%>