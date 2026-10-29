package com.krakedev.proyectos.services;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.krakedev.proyectos.entidades.Empleado;
import com.krakedev.proyectos.entidades.Proyecto;
import com.krakedev.proyectos.entidades.Tarea;
import com.krakedev.proyectos.repositories.EmpleadoRepository;
import com.krakedev.proyectos.repositories.ProyectoRepository;
import com.krakedev.proyectos.repositories.TareaRepository;

@Service
public class TareaService {
    private final TareaRepository repository;
    private final ProyectoRepository proyectoRepository;
    private final EmpleadoRepository empleadoRepository;

    public TareaService(TareaRepository repository, ProyectoRepository proyectoRepository,
            EmpleadoRepository empleadoRepository) {
        this.repository = repository;
        this.proyectoRepository = proyectoRepository;
        this.empleadoRepository = empleadoRepository;
    }

    @Transactional
    public Tarea guardar(Tarea tarea) {
        if (tarea.getProyecto() == null) {
            throw new IllegalArgumentException("Debe indicar el proyecto");
        }
        Proyecto proyecto = proyectoRepository.findById(tarea.getProyecto().getId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no existe"));
        if (tarea.getEmpleados() == null || tarea.getEmpleados().isEmpty()) {
            throw new IllegalArgumentException("Debe asignar al menos un empleado");
        }
        List<Empleado> empleadosDB = new ArrayList<>();
        for (Empleado empleado : tarea.getEmpleados()) {
            if (empleado == null) {
                throw new IllegalArgumentException("Empleado no existe");
            }
            Empleado existente = empleadoRepository.findById(empleado.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Empleado no existe"));
            empleadosDB.add(existente);
        }
        tarea.setProyecto(proyecto);
        tarea.setEmpleados(empleadosDB);
        return repository.save(tarea);
    }

    public List<Tarea> listar() {
        return repository.findAll();
    }
}
