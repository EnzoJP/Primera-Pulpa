package com.primeraPulpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Mix extends BaseEntity<Long> {

    private String nombre;

    // Puede no estar definido todavía (se define viendo los costos del mix)
    private Double precioVenta;

    // Costo por kg del mix. Se calcula desde la fórmula (gramos de cada
    // materia prima * precio, dividido la cantidad que produce) más los
    // costos adicionales del catálogo (bolsa, etiqueta, etc.).
    private double costo;

    //stock actual del mix. Se actualiza cada vez que se registra una elaboración o venta.
    private double stock;

    // Cantidad en kg de una unidad/paquete del mix (ej: 1 kg o 5 kg).
    // Todo el stock y el costo se manejan internamente por kg; esta es la
    // presentación comercial que usamos para mostrar precio/costo por unidad
    // y para el toggle "paquetes/kg" al cargar pedidos. Los costos adicionales
    // del catálogo aplican según esta presentación (1 kg, 5 kg o todos).
    private Double cantidadPorUnidad;

    // Campo transitorio (no se persiste) para que el formulario cargue el
    // precio POR UNIDAD/PAQUETE. En el controller se convierte a precio por kg
    // (precioVenta) usando cantidadPorUnidad. Todo internamente queda por kg.
    @jakarta.persistence.Transient
    private Double precioVentaUnidad;

    // Usuario que realizó la elaboración de este mix. INNCESARIO
    //@ManyToOne
    //private Usuario usuario;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Boolean getEliminado() {
        return this.eliminado;
    }

    @Override
    public void setEliminado(Boolean eliminado) {
        this.eliminado = eliminado;
    }

    public void actualizarStock(double cantidad) {
        this.stock += cantidad;
    }

    // Kg por unidad/paquete. Si no se cargó (mixes viejos), asumimos 1 kg.
    public double getCantidadPorUnidadOrDefault() {
        return cantidadPorUnidad != null && cantidadPorUnidad > 0 ? cantidadPorUnidad : 1.0;
    }

    // Precio de venta por unidad/paquete (presentación comercial).
    public Double getPrecioVentaPorUnidad() {
        return precioVenta != null
                ? Math.round(precioVenta * getCantidadPorUnidadOrDefault() * 100.0) / 100.0
                : null;
    }

    // Costo por unidad/paquete (incluye empaque amortizado).
    public double getCostoPorUnidad() {
        return Math.round((costo * getCantidadPorUnidadOrDefault()) * 100.0) / 100.0;
    }

    // Precio por unidad/paquete que se muestra en el formulario. Si aún no se
    // cargó en esta sesión, se calcula desde el precio por kg guardado.
    public Double getPrecioVentaUnidad() {
        return precioVentaUnidad != null
                ? precioVentaUnidad
                : getPrecioVentaPorUnidad();
    }

    public void setPrecioVentaUnidad(Double valor) {
        this.precioVentaUnidad = valor;
    }
}