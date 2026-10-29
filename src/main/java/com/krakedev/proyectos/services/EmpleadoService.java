package com.krakedev.proyectos.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.krakedev.proyectos.entidades.Empleado;
import com.krakedev.proyectos.repositories.EmpleadoRepository;

@Service
public class EmpleadoService {
    private final EmpleadoRepository repository;

    public EmpleadoService(EmpleadoRepository repository) {
        this.repository = repository;
    }

    public Empleado guardar(Empleado datos) {
        return repository.save(datos);
    }

    public List<Empleado> listar() {
        return repository.findAll();
    }

    public Empleado buscar(int id) {
        return repository.findById(id).orElse(null);
    }

    public Empleado actualizar(int id, Empleado datos) {
        Empleado actual = buscar(id);
        if (actual == null) {
            return null;
        }
        actual.setNombre(datos.getNombre());
        actual.setCargo(datos.getCargo());
        return repository.save(actual);
    }

    public boolean eliminar(int id) {
        Empleado actual = buscar(id);
        if (actual == null) {
            return false;
        }
        repository.delete(actual);
        return true;
    }
}
