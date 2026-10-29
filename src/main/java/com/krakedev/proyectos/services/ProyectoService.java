package com.krakedev.proyectos.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.krakedev.proyectos.entidades.Proyecto;
import com.krakedev.proyectos.repositories.ProyectoRepository;

@Service
public class ProyectoService {
    private final ProyectoRepository repository;

    public ProyectoService(ProyectoRepository repository) {
        this.repository = repository;
    }

    public Proyecto guardar(Proyecto datos) {
        return repository.save(datos);
    }

    public List<Proyecto> listar() {
        return repository.findAll();
    }

    public Proyecto buscar(int id) {
        return repository.findById(id).orElse(null);
    }

    public Proyecto actualizar(int id, Proyecto datos) {
        Proyecto actual = buscar(id);
        if (actual == null) {
            return null;
        }
        actual.setNombre(datos.getNombre());
        actual.setDescripcion(datos.getDescripcion());
        actual.setFechaInicio(datos.getFechaInicio());
        return repository.save(actual);
    }

    public boolean eliminar(int id) {
        Proyecto actual = buscar(id);
        if (actual == null) {
            return false;
        }
        repository.delete(actual);
        return true;
    }
}
