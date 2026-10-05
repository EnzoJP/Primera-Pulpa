# Historias de usuario — Control de stock Primera Pulpa

**Proyecto:** Control de stock para PP  
**Equipo:** NoCompila  
**Versión:** 3.0 — basada en el diagrama de clases final

---

## Roles del sistema

| Rol | Descripción |
|-----|-------------|
| **Admin** | Gerente o sub-gerente. Acceso completo a configuración, precios, costos y reportes. |
| **Empleado** | Acceso operativo: carga ingresos, elaboraciones y pedidos. No puede modificar precios ni configuración. |

---

## Módulo 1 — Autenticación y usuarios

### HU-01 — Iniciar sesión
**Como** usuario del sistema,  
**quiero** ingresar con mi email y contraseña,  
**para** acceder a las funciones habilitadas para mi rol.

**Criterios de aceptación:**
- El sistema valida credenciales y redirige al dashboard según el rol (ADMIN / EMPLEADO).
- Si las credenciales son incorrectas, muestra un mensaje genérico sin revelar qué campo falló.
- La sesión expira tras un período de inactividad configurable.

---

### HU-02 — Gestionar usuarios
**Como** administrador,  
**quiero** crear, editar y dar de baja usuarios asignándoles un rol,  
**para** controlar quién accede al sistema y qué puede hacer.

**Criterios de aceptación:**
- Se puede crear un `Usuario` con nombre, email, contraseña temporal y `Rol` (ADMIN o EMPLEADO). La contraseña se almacena encriptada (hash).
- La baja es lógica (`eliminado = true`): el usuario no puede ingresar, pero se preserva su historial en ingresos, elaboraciones, pedidos y cambios de estado o de precio.
- El administrador puede restablecer la contraseña de cualquier usuario.

---

## Módulo 2 — Materias primas

### HU-03 — Administrar catálogo de materias primas
**Como** administrador,  
**quiero** dar de alta, editar y dar de baja materias primas y sus unidades de medida,  
**para** mantener actualizado el catálogo de insumos.

**Criterios de aceptación:**
- Cada `MateriaPrima` tiene: nombre, `UnidadMedida`, precio, cantidad mínima y cantidad actual (que inicia en 0).
- Las unidades de medida se administran como un catálogo propio (`UnidadMedida`).
- La baja es lógica y no se permite si la materia prima está incluida en un `DetalleFormula` de una fórmula activa.

---

### HU-04 — Registrar ingreso de materia prima
**Como** empleado,  
**quiero** registrar un `IngresoMP` con las materias primas recibidas y sus cantidades,  
**para** que el sistema actualice su stock y registre el lote correspondiente.

**Criterios de aceptación:**
- Un `IngresoMP` puede incluir múltiples materias primas en una sola operación (`DetalleIngresoMP`).
- Cada línea requiere: materia prima, cantidad, costo unitario de esa compra, número de lote y fecha de vencimiento.
- Al confirmar el ingreso, `MateriaPrima.cantidadActual` se incrementa con lo ingresado y el lote queda con `cantidadRestante` igual a la cantidad ingresada.
- El `IngresoMP` queda registrado con fecha, hora y usuario responsable.
- No se puede modificar un `IngresoMP` ya confirmado; se debe registrar uno nuevo para corregir.

---

### HU-05 — Consultar stock actual de materias primas
**Como** usuario,  
**quiero** ver el stock actual de todas las materias primas,  
**para** saber con qué insumos cuenta la empresa en este momento.

**Criterios de aceptación:**
- Se muestra una lista con nombre, unidad de medida y `cantidadActual` de cada materia prima.
- Las materias primas con `cantidadActual` por debajo de `cantidadMinima` se destacan visualmente con una alerta.
- Se puede filtrar por nombre.

---

### HU-06 — Alerta por stock bajo de materia prima
**Como** usuario,  
**quiero** recibir una alerta cuando el stock de un insumo cae por debajo del mínimo configurado,  
**para** gestionar la reposición antes de que impacte en la producción.

**Criterios de aceptación:**
- La alerta se muestra en el dashboard al iniciar sesión.
- Se listan todas las materias primas con `cantidadActual < cantidadMinima`.
- Cada alerta muestra nombre, stock actual y stock mínimo del insumo.

---

## Módulo 3 — Fórmulas

### HU-07 — Gestionar fórmulas de mixes
**Como** administrador,  
**quiero** crear, editar y eliminar fórmulas que definan qué materias primas componen cada `Mix` y cuántos gramos de cada una,  
**para** que el sistema pueda calcular automáticamente el consumo al elaborar.

**Criterios de aceptación:**
- Una `Formula` pertenece a un `Mix` y define la cantidad total que produce (cantidad, por ejemplo 25 kg).
- La `Formula` contiene uno o más `DetalleFormula`, cada uno con una `MateriaPrima` y los gramos que lleva esa cantidad total.
- Un `Mix` debe tener una fórmula con al menos un detalle para poder elaborarse.
- No se puede eliminar una `Formula` si el `Mix` asociado tiene `LoteMix` registrados.
- Al editar una `Formula` no se modifican las elaboraciones ya registradas, porque sus consumos quedan guardados en `DetalleConsumoLote`.

---

## Módulo 4 — Mixes

### HU-08 — Administrar catálogo de mixes
**Como** administrador,  
**quiero** dar de alta, editar y dar de baja mixes,  
**para** mantener actualizado el catálogo de productos elaborados de la empresa.

**Criterios de aceptación:**
- Cada `Mix` tiene: nombre, precio de venta, presentación (`cantidadPorUnidad`, por ejemplo 1 kg o 5 kg) y stock (que inicia en 0).
- La baja es lógica y no se permite si el mix tiene pedidos pendientes asociados.
- Cada cambio de precio de venta genera un `HistorialPrecioMix` con precio anterior, precio nuevo, fecha, hora y usuario.
- Al editar un mix se conservan su stock y su costo.

---

### HU-09 — Registrar elaboración de un mix
**Como** empleado,  
**quiero** registrar la elaboración de un mix indicando el mix y la cantidad a producir,  
**para** que el sistema descuente el stock de las materias primas consumidas y actualice el stock del mix.

**Criterios de aceptación:**
- El usuario selecciona el `Mix` y la cantidad a producir.
- El sistema calcula el consumo de cada `MateriaPrima` escalando los gramos del `DetalleFormula` según la cantidad a producir: `gramos × (cantidad a producir / Formula.cantidad)`.
- Si algún insumo no alcanza para cubrir el consumo, el sistema advierte al usuario antes de confirmar.
- Al confirmar: se crea un `LoteMix` (cantidad elaborada, fecha de elaboración y usuario), se descuenta la cantidad correspondiente de `cantidadRestante` en los lotes de materia prima y de `cantidadActual`, y se suma la cantidad producida a `Mix.stock`.
- Cada consumo queda registrado en un `DetalleConsumoLote` (lote de materia prima, lote de mix y cantidad consumida), lo que da trazabilidad completa del producto elaborado.

---

### HU-10 — Consultar stock actual de mixes
**Como** usuario,  
**quiero** ver el stock actual de todos los mixes,  
**para** saber qué hay disponible para despachar.

**Criterios de aceptación:**
- Se muestra una lista con nombre del mix, stock disponible (descontando lo comprometido en pedidos activos), cantidad pedida y cantidad por elaborar.
- Los mixes sin stock disponible se destacan visualmente.
- Se puede filtrar por nombre.

---

### HU-11 — Consultar costo y ganancia de un mix
**Como** administrador,  
**quiero** ver el costo de producción y el porcentaje de ganancia de cada mix,  
**para** tomar decisiones informadas sobre precios de venta.

**Criterios de aceptación:**
- El costo del mix (Mix.costo) se calcula a partir de su Formula: se suman DetalleFormula.gramos × MateriaPrima.precio de cada componente, más los CostoAdicional aplicables, y se divide por Formula.cantidad para obtener el costo por unidad de producto.
- El porcentaje de ganancia se calcula como ((Mix.precioVenta - Mix.costo) / Mix.costo) × 100.
- Si el porcentaje de ganancia es menor a un umbral configurable, se muestra una alerta visual.
- El costo se recalcula automáticamente cuando se actualiza el precio de alguna materia prima.

---

## Módulo 5 — Clientes

### HU-12 — Gestionar clientes
**Como** empleado,  
**quiero** registrar, editar y consultar clientes,  
**para** asociarlos a los pedidos de manera ágil.

**Criterios de aceptación:**
- Cada `Cliente` tiene: nombre, CUIT y contacto.
- Se puede buscar un cliente por nombre o contacto.
- La baja es lógica y no se permite si el cliente tiene pedidos asociados.

---

## Módulo 6 — Pedidos

### HU-13 — Registrar pedido de cliente
**Como** empleado,  
**quiero** cargar un `Pedido` indicando el cliente, los mixes y las cantidades,  
**para** registrar lo que debe despacharse y reservar el stock correspondiente.

**Criterios de aceptación:**
- Un `Pedido` puede incluir múltiples mixes (`DetallePedido`), cada uno con cantidad y precio unitario.
- Si el stock de algún mix no cubre la cantidad pedida, el sistema lo advierte antes de confirmar e indica cuánto falta elaborar.
- El pedido se crea en estado PENDIENTE con fecha y usuario que lo registró, y se genera el primer `HistorialEstadoPedido`.

---

### HU-14 — Preparar pedido y gestionar su estado
**Como** empleado,  
**quiero** preparar los ítems de un pedido y actualizar su estado (PENDIENTE → PREPARADO → ENTREGADO / CANCELADO),  
**para** descontar el stock a medida que armo el pedido y tener trazabilidad del proceso de despacho.

**Criterios de aceptación:**
- Cada ítem del pedido (`DetallePedido`) se puede marcar como preparado de forma individual, lo que permite la preparación parcial. Al hacerlo, se descuenta la cantidad del `Mix.stock`.
- Se muestra el progreso de armado del pedido (por ejemplo, 1 de 3 ítems preparados).
- Los estados posibles son: PENDIENTE, PREPARADO, ENTREGADO y CANCELADO.
- Cada cambio de estado queda registrado en `HistorialEstadoPedido` con fecha, hora y usuario.
- Al cancelar un pedido, se restaura el stock de los ítems que ya estaban preparados.
- No se puede volver atrás desde el estado ENTREGADO.

---

## Módulo 7 — Historial y trazabilidad

### HU-15 — Consultar historial de movimientos de materia prima
**Como** usuario,  
**quiero** ver todos los movimientos que afectaron el stock de una materia prima,  
**para** saber qué entró y qué se consumió en un período determinado.

**Criterios de aceptación:**
- Se puede filtrar por materia prima y rango de fechas.
- Cada registro muestra: tipo de movimiento (ingreso, con su número de lote, o consumo por elaboración), fecha, cantidad y usuario responsable.
- El saldo resultante se muestra al final del período consultado.

---

### HU-16 — Consultar historial de movimientos de mix
**Como** usuario,  
**quiero** ver todos los movimientos que afectaron el stock de un mix,  
**para** saber qué se elaboró y qué se despachó en un período determinado.

**Criterios de aceptación:**
- Se puede filtrar por mix y rango de fechas.
- Cada registro muestra: tipo de movimiento (elaboración mediante `LoteMix` o preparación de pedido), fecha, cantidad y usuario responsable.
- El saldo resultante se muestra al final del período consultado.

---

## Módulo 8 — Sistema general

### HU-17 — Acceder desde cualquier lugar
**Como** usuario,  
**quiero** acceder al sistema desde cualquier dispositivo con internet,  
**para** consultar el stock o registrar movimientos fuera de la empresa.

**Criterios de aceptación:**
- El sistema es accesible desde cualquier dispositivo con navegador, previa conexión a la red privada (VPN) de la empresa.
- La interfaz es responsiva y funciona en celular y tablet.
- Acceso remoto mediante VPN (Tailscale) y dominio gestionado en Cloudflare

---

### HU-18 — Respaldo automático de datos
**Como** administrador,  
**quiero** que el sistema genere backups automáticos de la base de datos,  
**para** poder recuperar la información en caso de fallo del servidor.

**Criterios de aceptación:**
- Los backups se generan automáticamente con frecuencia configurable.
- Los archivos se almacenan localmente con nombre que incluye fecha y hora.
- El administrador puede descargar o restaurar un backup desde la interfaz.

---

## Módulo 9 — Estadísticas

### HU-19 — Consultar estadísticas mensuales y anuales
**Como** administrador,  
**quiero** ver resúmenes mensuales y anuales de la operación,  
**para** evaluar la rentabilidad del negocio.

**Criterios de aceptación:**
- Se muestran: kg vendidos, facturado, costo de producción, rentabilidad, kg ingresados, kg elaborados y margen promedio.
- Se lista el detalle de los mixes vendidos en el período, con kg vendidos, facturado, costo y ganancia real.
- Se puede elegir el mes o el año a consultar.

---

## Resumen

| ID | Módulo | Rol mínimo | Prioridad |
|----|--------|------------|-----------|
| HU-01 | Autenticación | Todos | Alta |
| HU-02 | Autenticación | Admin | Media |
| HU-03 | Materias primas | Admin | Alta |
| HU-04 | Materias primas | Empleado | Alta |
| HU-05 | Materias primas | Todos | Alta |
| HU-06 | Materias primas | Todos | Baja |
| HU-07 | Fórmulas | Admin | Alta |
| HU-08 | Mixes | Admin | Alta |
| HU-09 | Mixes | Empleado | Alta |
| HU-10 | Mixes | Todos | Alta |
| HU-11 | Mixes | Admin | Baja |
| HU-12 | Clientes | Empleado | Baja |
| HU-13 | Pedidos | Empleado | Alta |
| HU-14 | Pedidos | Empleado | Media |
| HU-15 | Historial | Todos | Media |
| HU-16 | Historial | Todos | Media |
| HU-17 | Sistema | Todos | Alta |
| HU-18 | Sistema | Admin | Media |
| HU-19 | Estadísticas | Admin | Media |