package com.primeraPulpa.entities;

// Presentación comercial a la que aplica un costo adicional (p.ej. empaque).
// Una bolsa de 5 kg no cuesta lo mismo que una de 1 kg, por eso el costo
// adicional indica para qué presentación aplica.
public enum PresentacionCosto {
    // Aplica a mixes cuya presentación es de 1 kg por unidad.
    UNO_KG("1 kg"),
    // Aplica a mixes cuya presentación es de 5 kg por unidad.
    CINCO_KG("5 kg"),
    // Aplica a todas las presentaciones de mix (costo genérico por unidad).
    TODOS("Todos");

    private final String etiqueta;

    PresentacionCosto(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
