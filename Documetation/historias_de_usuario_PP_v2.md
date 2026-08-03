# Historias de usuario — Control de stock Primera Pulpa

**Proyecto:** Control de stock para PP  
**Equipo:** NoCompila  
**Versión:** 2.0 — basada en diagrama de clases revisado

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
**quiero** crear, editar y desactivar usuarios asignándoles un rol,  
**para** controlar quién accede al sistema y qué puede hacer.

**Criterios de aceptación:**
- Se puede crear un usuario con nombre, email, contraseña temporal y rol (ADMIN o EMPLEADO).
- Se puede desactivar un usuario sin eliminarlo, preservando su historial de movimientos.
- No se puede eliminar un usuario que tenga registros asociados (ingresos, elaboraciones, pedidos).
- El administrador puede restablecer la contraseña de cualquier usuario.

---

## Módulo 2 — Materias primas

### HU-03 — Administrar catálogo de materias primas
**Como** administrador,  
**quiero** dar de alta, editar y dar de baja materias primas,  
**para** mantener actualizado el catálogo de insumos.

**Criterios de aceptación:**
- Cada materia prima tiene: nombre, unidad de medida, precio unitario y stock mínimo.
- Al dar de alta una materia prima se crea automáticamente su registro `StockMP` con cantidad 0.
- No se puede dar de baja una materia prima referenciada en una `Formula` activa.

---

### HU-04 — Registrar ingreso de materia prima
**Como** empleado,  
**quiero** registrar un `IngresoMP` con las materias primas recibidas y sus cantidades,  
**para** que el sistema actualice el `StockMP` correspondiente.

**Criterios de aceptación:**
- Un `IngresoMP` puede incluir múltiples materias primas en una sola operación (`DetalleIngresoMP`).
- Cada línea requiere: materia prima, cantidad y costo unitario de esa compra.
- Al confirmar el ingreso, la cantidad de cada `StockMP` se incrementa con lo ingresado.
- El `IngresoMP` queda registrado con fecha, hora y usuario responsable.
- No se puede modificar un `IngresoMP` ya confirmado; se debe registrar uno nuevo para corregir.

---

### HU-05 — Consultar stock actual de materias primas
**Como** usuario,  
**quiero** ver el stock actual de todas las materias primas,  
**para** saber con qué insumos cuenta la empresa en este momento.

**Criterios de aceptación:**
- Se muestra una lista con nombre de la materia prima, unidad de medida y cantidad actual del `StockMP`.
- Las materias primas cuyo `StockMP` está por debajo del stock mínimo se destacan visualmente con una alerta.
- Se puede filtrar por nombre.

---

### HU-06 — Alerta por stock bajo de materia prima
**Como** usuario,  
**quiero** recibir una alerta cuando el `StockMP` de un insumo cae por debajo del mínimo configurado,  
**para** gestionar la reposición antes de que impacte en la producción.

**Criterios de aceptación:**
- La alerta se muestra en el dashboard al iniciar sesión.
- Se listan todas las materias primas con `StockMP.cantidad < MateriaPrima.stockMinimo`.
- Cada alerta muestra nombre, stock actual y stock mínimo del insumo.

---

## Módulo 3 — Fórmulas

### HU-07 — Gestionar fórmulas de mixes
**Como** administrador,  
**quiero** crear, editar y eliminar fórmulas que definan qué materias primas componen cada `Mix` y en qué cantidades,  
**para** que el sistema pueda calcular automáticamente el consumo al elaborar.

**Criterios de aceptación:**
- Una `Formula` vincula un `Mix` con una `MateriaPrima` y especifica la cantidad necesaria por unidad producida.
- Un `Mix` debe tener al menos una `Formula` para poder usarse en una `ElaboracionMix`.
- No se puede eliminar una `Formula` si el `Mix` asociado tiene `ElaboracionMix` registradas.
- Al editar una `Formula` no se modifican las elaboraciones ya registradas.

---

## Módulo 4 — Mixes

### HU-08 — Administrar catálogo de mixes
**Como** administrador,  
**quiero** dar de alta, editar y dar de baja mixes,  
**para** mantener actualizado el catálogo de productos elaborados de la empresa.

**Criterios de aceptación:**
- Cada `Mix` tiene: nombre y precio de venta.
- Al dar de alta un `Mix` se crea automáticamente su registro `StockMix` con cantidad 0.
- No se puede dar de baja un `Mix` con pedidos pendientes asociados.
- El cambio de precio de venta queda registrado con fecha y usuario.

---

### HU-09 — Registrar elaboración de un mix
**Como** empleado,  
**quiero** registrar una `ElaboracionMix` indicando el mix producido y la cantidad,  
**para** que el sistema descuente el `StockMP` de las materias primas consumidas y actualice el `StockMix`.

**Criterios de aceptación:**
- El usuario selecciona el `Mix` y la cantidad a producir.
- El sistema calcula el consumo de cada `MateriaPrima` usando las `Formula` del mix.
- Si algún `StockMP` no alcanza para cubrir el consumo, el sistema advierte al usuario antes de confirmar.
- Al confirmar: se descuenta la cantidad correspondiente de cada `StockMP` y se suma la cantidad producida al `StockMix` del mix.
- La `ElaboracionMix` queda registrada con fecha, hora, usuario y detalle de consumos.

---

### HU-10 — Consultar stock actual de mixes
**Como** usuario,  
**quiero** ver el stock actual de todos los mixes,  
**para** saber qué hay disponible para despachar.

**Criterios de aceptación:**
- Se muestra una lista con nombre del mix y cantidad actual del `StockMix`.
- Los mixes con `StockMix.cantidad = 0` se destacan visualmente.
- Se puede filtrar por nombre.

---

### HU-11 — Consultar costo y ganancia de un mix
**Como** administrador,  
**quiero** ver el costo de producción y el porcentaje de ganancia de cada mix,  
**para** tomar decisiones informadas sobre precios de venta.

**Criterios de aceptación:**
- El costo se calcula sumando `(Formula.cantidad × MateriaPrima.precioUnitario)` para cada componente del mix.
- El porcentaje de ganancia se calcula como `((Mix.precioVenta - costo) / costo) × 100`.
- Si el porcentaje de ganancia es menor a un umbral configurable, se muestra una alerta visual.
- El costo se recalcula automáticamente cuando se actualiza el precio de alguna materia prima.

---

## Módulo 5 — Clientes

### HU-12 — Gestionar clientes
**Como** empleado,  
**quiero** registrar, editar y consultar clientes,  
**para** asociarlos a los pedidos de manera ágil.

**Criterios de aceptación:**
- Cada `Cliente` tiene: nombre y contacto.
- Se puede buscar un cliente por nombre o contacto.
- No se puede eliminar un cliente con pedidos asociados.

---

## Módulo 6 — Pedidos

### HU-13 — Registrar pedido de cliente
**Como** empleado,  
**quiero** cargar un `Pedido` indicando el cliente, los mixes y las cantidades,  
**para** registrar el egreso del `StockMix` correspondiente.

**Criterios de aceptación:**
- Un `Pedido` puede incluir múltiples mixes (`DetallePedido`), cada uno con cantidad y precio unitario.
- Si el `StockMix` de algún mix no cubre la cantidad pedida, el sistema lo advierte antes de confirmar.
- Al confirmar, la cantidad de cada `StockMix` se decrementa según el detalle del pedido.
- El pedido se crea en estado PENDIENTE con fecha, hora y usuario que lo registró.

---

### HU-14 — Gestionar estado de un pedido
**Como** empleado,  
**quiero** actualizar el estado de un pedido (PENDIENTE → PREPARADO → ENTREGADO / CANCELADO),  
**para** tener trazabilidad del proceso de despacho.

**Criterios de aceptación:**
- Los estados posibles son: PENDIENTE, PREPARADO, ENTREGADO, CANCELADO.
- Al cancelar un pedido, el `StockMix` de los mixes involucrados se restaura.
- No se puede volver atrás desde el estado ENTREGADO.
- Cada cambio de estado queda registrado con fecha, hora y usuario.

---

## Módulo 7 — Historial y trazabilidad

### HU-15 — Consultar historial de movimientos de StockMP
**Como** usuario,  
**quiero** ver todos los movimientos que afectaron el `StockMP` de una materia prima,  
**para** saber qué entró y qué se consumió en un período determinado.

**Criterios de aceptación:**
- Se puede filtrar por materia prima y rango de fechas.
- Cada registro muestra: tipo de movimiento (IngresoMP o ElaboracionMix), fecha, cantidad y usuario responsable.
- El saldo resultante se muestra al final del período consultado.

---

### HU-16 — Consultar historial de movimientos de StockMix
**Como** usuario,  
**quiero** ver todos los movimientos que afectaron el `StockMix` de un mix,  
**para** saber qué se elaboró y qué se despachó en un período determinado.

**Criterios de aceptación:**
- Se puede filtrar por mix y rango de fechas.
- Cada registro muestra: tipo de movimiento (ElaboracionMix o Pedido), fecha, cantidad y usuario responsable.
- El saldo resultante se muestra al final del período consultado.

---

## Módulo 8 — Sistema general

### HU-17 — Acceder desde cualquier lugar
**Como** usuario,  
**quiero** acceder al sistema desde cualquier dispositivo con internet,  
**para** consultar el stock o registrar movimientos fuera de la empresa.

**Criterios de aceptación:**
- El sistema es accesible vía navegador web sin instalar software adicional.
- La interfaz es responsiva y funciona en celular y tablet.
- El acceso remoto está habilitado mediante Cloudflare Tunnel sobre el servidor local.

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

## Resumen

| ID | Módulo | Rol mínimo | Prioridad |
|----|--------|------------|-----------|
| HU-01 | Autenticación | Todos | Alta |
| HU-02 | Autenticación | Admin | Alta |
| HU-03 | Materias primas | Admin | Alta |
| HU-04 | Materias primas | Empleado | Alta |
| HU-05 | Materias primas | Todos | Alta |
| HU-06 | Materias primas | Todos | Media |
| HU-07 | Fórmulas | Admin | Alta |
| HU-08 | Mixes | Admin | Alta |
| HU-09 | Mixes | Empleado | Alta |
| HU-10 | Mixes | Todos | Alta |
| HU-11 | Mixes | Admin | Media |
| HU-12 | Clientes | Empleado | Alta |
| HU-13 | Pedidos | Empleado | Alta |
| HU-14 | Pedidos | Empleado | Media |
| HU-15 | Historial | Todos | Media |
| HU-16 | Historial | Todos | Media |
| HU-17 | Sistema | Todos | Alta |
| HU-18 | Sistema | Admin | Media |
