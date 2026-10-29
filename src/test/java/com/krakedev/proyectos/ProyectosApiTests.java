package com.krakedev.proyectos;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProyectosApiTests {
    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> enviar(String metodo, String ruta, String json) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/" + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json)).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private int id(String json) {
        var matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        assertTrue(matcher.find(), json);
        return Integer.parseInt(matcher.group(1));
    }

    @Test
    void flujoDeEntregaCrudYRechazoDeRelacionesInexistentes() throws Exception {
        String empleadoJson = """
                {"nombre":"Diana Rojas","cargo":"Desarrollador Senior"}
                """;
        var empleado = enviar("POST", "empleados", empleadoJson);
        assertEquals(201, empleado.statusCode(), empleado.body());
        int empleadoId = id(empleado.body());
        String proyectoJson = """
                {"nombre":"E-Commerce Plataforma","descripcion":"Desarrollo del nuevo portal de ventas online",
                "fechaInicio":"2026-05-20"}
                """;
        var proyecto = enviar("POST", "proyectos", proyectoJson);
        assertEquals(201, proyecto.statusCode(), proyecto.body());
        int proyectoId = id(proyecto.body());
        // Los IDs en texto reproducen las variables de la colección de Postman.
        String tareaJson = """
                {"descripcion":"Diseñar e implementar el modelo de datos para la base de datos",
                "fechaLimite":"2026-06-15","costoEstimado":450.50,
                "proyecto":{"id":"%d"},"empleados":[{"id":"%d"}]}
                """.formatted(proyectoId, empleadoId);
        var tarea = enviar("POST", "tareas", tareaJson);
        assertEquals(201, tarea.statusCode(), tarea.body());
        assertTrue(tarea.body().contains("Diana Rojas"), tarea.body());
        assertTrue(tarea.body().contains("E-Commerce Plataforma"), tarea.body());
        var lista = enviar("GET", "tareas", null);
        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("Diana Rojas"), lista.body());
        assertTrue(lista.body().contains("E-Commerce Plataforma"), lista.body());
        assertTrue(lista.body().contains("450.5"), lista.body());
        assertFalse(lista.body().contains("\"tareas\""), lista.body());
        assertEquals(1, jdbc.queryForObject("select count(*) from tarea_empleados", Integer.class));
        assertEquals(proyectoId, jdbc.queryForObject("select proyecto_id from tareas", Integer.class));
        assertEquals(200, enviar("GET", "proyectos/" + proyectoId, null).statusCode());
        assertEquals(200, enviar("GET", "empleados/" + empleadoId, null).statusCode());
        assertEquals(200, enviar("GET", "proyectos", null).statusCode());
        assertEquals(200, enviar("GET", "empleados", null).statusCode());
        var sinProyecto = enviar("POST", "tareas", tareaJson.replace("\"id\":\"" + proyectoId + "\"}",
                "\"id\":\"999999\"}"));
        assertEquals(500, sinProyecto.statusCode(), sinProyecto.body());
        assertTrue(sinProyecto.body().contains("Proyecto no existe"));
        var sinEmpleado = enviar("POST", "tareas", """
                {"descripcion":"Inválida","fechaLimite":"2026-06-15","costoEstimado":10,
                "proyecto":{"id":%d},"empleados":[{"id":999999}]}
                """.formatted(proyectoId));
        assertEquals(500, sinEmpleado.statusCode(), sinEmpleado.body());
        assertTrue(sinEmpleado.body().contains("Empleado no existe"));
        assertEquals(1, jdbc.queryForObject("select count(*) from tareas", Integer.class));
        var sinAsignados = enviar("POST", "tareas", """
                {"descripcion":"Sin empleados","fechaLimite":"2026-06-15","costoEstimado":10,
                "proyecto":{"id":%d},"empleados":[]}
                """.formatted(proyectoId));
        assertEquals(500, sinAsignados.statusCode());
        assertEquals(200, enviar("PUT", "proyectos/" + proyectoId,
                proyectoJson.replace("portal de ventas online", "portal actualizado")).statusCode());
        assertTrue(enviar("GET", "proyectos/" + proyectoId, null).body().contains("portal actualizado"));
        assertEquals(200, enviar("PUT", "empleados/" + empleadoId,
                empleadoJson.replace("Desarrollador Senior", "Líder de desarrollo")).statusCode());
        assertTrue(enviar("GET", "empleados/" + empleadoId, null).body().contains("Líder de desarrollo"));
        assertEquals(404, enviar("GET", "proyectos/999999", null).statusCode());
        assertEquals(404, enviar("PUT", "empleados/999999", empleadoJson).statusCode());
        assertEquals(404, enviar("DELETE", "proyectos/999999", null).statusCode());
        // Eliminar registros independientes sin destruir el flujo de evidencia.
        int temporalEmpleado = id(enviar("POST", "empleados", empleadoJson).body());
        int temporalProyecto = id(enviar("POST", "proyectos", proyectoJson).body());
        assertEquals(204, enviar("DELETE", "empleados/" + temporalEmpleado, null).statusCode());
        assertEquals(204, enviar("DELETE", "proyectos/" + temporalProyecto, null).statusCode());
        assertEquals(404, enviar("GET", "empleados/" + temporalEmpleado, null).statusCode());
        assertEquals(404, enviar("GET", "proyectos/" + temporalProyecto, null).statusCode());
        assertTrue(enviar("GET", "tareas", null).body().contains("Diana Rojas"));
    }
}
