package com.primeraPulpa.controller;

import com.primeraPulpa.entities.BaseEntity;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.Services.BaseService;
import jakarta.validation.Valid;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class BaseController<T extends BaseEntity<ID>, ID> {

    protected final BaseService<T, ID> service;
    private final Class<T> entityClass;
    private final String basePath;
    private final String entityName;

    protected BaseController(BaseService<T, ID> service, Class<T> entityClass, String basePath, String entityName) {
        this.service = service;
        this.entityClass = entityClass;
        this.basePath = basePath;
        this.entityName = entityName;
    }

    @GetMapping("")
    public String getAll(@RequestParam Map<String, String> params, Model model) {
        try {
            List<T> activos = service.listarActivos();

            if (!params.isEmpty()) {
                activos = filtrarPorParametros(activos, params);
            }

            cargarAtributosBase(model);
            model.addAttribute("items", activos);
            return vistaListado();
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            return "error/500";
        } catch (Exception e) {
            model.addAttribute("error", "Error de Sistema");
            return "error/500";
        }
    }



    @GetMapping("/{id}")
    public String getOne(@PathVariable ID id, Model model) {
        try {
            T entidad = service.getOne(id);
            cargarAtributosBase(model);
            model.addAttribute("item", entidad);
            return vistaDetalle();
        } catch (ErrorServiceException e) {
            model.addAttribute("error", e.getMessage());
            return "error/404";
        } catch (Exception e) {
            model.addAttribute("error", "Entidad no encontrada");
            return "error/404";
        }
    }

    @PostMapping("")
    public String create(@Valid @ModelAttribute("item") T entidad,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("formAction", basePath);
            model.addAttribute("formTitle", "Crear " + entityName);
            return vistaFormulario();
        }

        try {
            preCreate(entidad);
            T guardado = service.alta(entidad);
            postCreate(guardado);
            redirectAttributes.addFlashAttribute("success", entityName + " creado correctamente");
            return "redirect:" + basePath;
        } catch (ErrorServiceException e) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formAction", basePath);
            model.addAttribute("formTitle", "Crear " + entityName);
            return vistaFormulario();
        } catch (Exception e) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("error", "Error al crear la entidad");
            model.addAttribute("formAction", basePath);
            model.addAttribute("formTitle", "Crear " + entityName);
            return vistaFormulario();
        }
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable ID id, Model model) {
        try {
            T entidad = service.getOne(id);
            cargarAtributosBase(model);
            model.addAttribute("item", entidad);
            cargarDatosFormulario(model);
            model.addAttribute("formAction", basePath + "/" + id);
            model.addAttribute("formTitle", "Editar " + entityName);
            return vistaFormulario();
        } catch (Exception e) {
            model.addAttribute("error", "Entidad no encontrada");
            return "error/404";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable ID id,
                         @Valid @ModelAttribute("item") T entidad,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("formAction", basePath + "/" + id);
            model.addAttribute("formTitle", "Editar " + entityName);
            return vistaFormulario();
        }

        try {
            preUpdate(entidad);
            Optional<T> actualizado = service.modificar(id, entidad);

            if (actualizado.isPresent()) {
                postUpdate(actualizado.get());
                redirectAttributes.addFlashAttribute("success", entityName + " actualizado correctamente");
                return "redirect:" + basePath;
            } else {
                model.addAttribute("error", "Entidad no encontrada");
                return "error/404";
            }
        } catch (ErrorServiceException e) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formAction", basePath + "/" + id);
            model.addAttribute("formTitle", "Editar " + entityName);
            return vistaFormulario();
        } catch (Exception e) {
            cargarAtributosBase(model);
            cargarDatosFormulario(model);
            model.addAttribute("error", "Error al actualizar la entidad");
            model.addAttribute("formAction", basePath + "/" + id);
            model.addAttribute("formTitle", "Editar " + entityName);
            return vistaFormulario();
        }
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable ID id, RedirectAttributes redirectAttributes, Model model) {
        try {
            preDelete(id);
            boolean eliminado = service.bajaLogica(id);

            if (eliminado) {
                postDelete(id);
                redirectAttributes.addFlashAttribute("success", entityName + " eliminado correctamente");
                return "redirect:" + basePath;
            } else {
                model.addAttribute("error", "Entidad no encontrada");
                return "error/404";
            }
        } catch (ErrorServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + basePath;
        } catch (Exception e) {
            model.addAttribute("error", "Error al eliminar la entidad");
            return "error/500";
        }
    }

    // Métodos para ser sobrescritos en las clases hijas si es necesario
    protected void preCreate(T entidad) throws ErrorServiceException {}
    protected void postCreate(T entidad) throws ErrorServiceException {}
    protected void preUpdate(T entidad) throws ErrorServiceException {}
    protected void postUpdate(T entidad) throws ErrorServiceException {}
    protected void preDelete(ID id) throws ErrorServiceException {}
    protected void postDelete(ID id) throws ErrorServiceException {}


    protected List<T> filtrarPorParametros(List<T> lista, Map<String, String> params) {
        return lista.stream().filter(entidad -> {
            return params.entrySet().stream().allMatch(entry -> {
                try {
                    String nombreCampo = entry.getKey();
                    String valorBuscado = entry.getValue().toLowerCase();

                    Object valorCampo = obtenerValorCampo(entidad, nombreCampo);

                    if (valorCampo == null) {
                        return false;
                    }

                    return valorCampo.toString().equalsIgnoreCase(valorBuscado);

                } catch (Exception e) {
                    return true;
                }
            });
        }).collect(java.util.stream.Collectors.toList());
    }

    private Object obtenerValorCampo(Object objeto, String nombreCampo) throws Exception {
        if (nombreCampo.contains(".")) {
            String[] partes = nombreCampo.split("\\.", 2);
            String campoActual = partes[0];
            String campoRestante = partes[1];

            Object objetoIntermedio = obtenerValorCampoSimple(objeto, campoActual);

            if (objetoIntermedio == null) {
                return null;
            }

            return obtenerValorCampo(objetoIntermedio, campoRestante);
        } else {
            return obtenerValorCampoSimple(objeto, nombreCampo);
        }
    }

    private Object obtenerValorCampoSimple(Object objeto, String nombreCampo) throws Exception {
        String getterName = "get" + nombreCampo.substring(0, 1).toUpperCase() + nombreCampo.substring(1);
        java.lang.reflect.Method getter = objeto.getClass().getMethod(getterName);
        return getter.invoke(objeto);
    }

    protected void cargarAtributosBase(Model model) {
        model.addAttribute("basePath", basePath);
        model.addAttribute("entityName", entityName);
    }

    private T crearInstanciaVacia() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/nuevo")
    public String newForm(Model model) {
        cargarAtributosBase(model);
        model.addAttribute("item", crearInstanciaVacia());
        model.addAttribute("formAction", basePath);
        model.addAttribute("formTitle", "Crear " + entityName);
        cargarDatosFormulario(model);        // ← nuevo, abstracto
        return vistaFormulario();            // ← nuevo, sobreescribible
    }

    // Métodos que cada hijo implementa según su necesidad
    protected void cargarDatosFormulario(Model model) {}  // override opcional

    protected String vistaFormulario() {
        return "crud/form";  // default, cada entidad puede tener su propio template
    }

    protected String vistaListado() {
        return "crud/list";
    }

    protected String vistaDetalle() {
        return "crud/detail";
    }


}