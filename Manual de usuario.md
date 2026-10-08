# Manual de Usuario del Sistema — Primera Pulpa

Bienvenido al manual de uso del sistema de gestión integral de **Primera Pulpa**. Esta guía explica paso a paso cómo operar cada función de la aplicación, organizar la producción, controlar el stock y registrar ventas de forma clara y accesible para el personal administrativo y de planta.

---

## Índice
1. [Acceso al Sistema y Perfiles de Usuario](#1-acceso-al-sistema-y-perfiles-de-usuario)
2. [Pantalla Principal (Panel de Control)](#2-pantalla-principal-panel-de-control)
3. [Materias Primas y Unidades de Medida](#3-materias-primas-y-unidades-de-medida)
4. [Recepción de Mercadería (Ingreso de Insumos)](#4-recepción-de-mercadería-ingreso-de-insumos)
5. [Fórmulas y Costos Adicionales](#5-fórmulas-y-costos-adicionales)
6. [Catálogo de Mixes (Productos Terminados)](#6-catálogo-de-mixes-productos-terminados)
7. [Planta de Elaboración Diaria](#7-planta-de-elaboración-diaria)
8. [Ventas: Clientes y Pedidos](#8-ventas-clientes-y-pedidos)
9. [Historial de Movimientos de Stock y Trazabilidad](#9-historial-de-movimientos-de-stock-y-trazabilidad)
10. [Informes y Estadísticas](#10-informes-y-estadísticas)
11. [Usuarios y Copias de Seguridad](#11-usuarios-y-copias-de-seguridad)
12. [Preguntas Frecuentes (FAQ)](#12-preguntas-frecuentes-faq)

---

## 1. Acceso al Sistema y Perfiles de Usuario

### Inicio de Sesión
1. Abra el navegador e ingrese la dirección web provista por la empresa.
2. Escriba su **correo electrónico** y su **contraseña**.
3. Haga clic en **Ingresar**. El sistema lo llevará directamente al Panel de Control.

### Perfiles de Acceso
* **Administrador:** Diseñado para la dirección y supervisión comercial. Tiene acceso a la creación de productos, recetas, precios, costos extra, clientes, reportes económicos, configuración de cuentas de usuario y resguardos del sistema.
* **Empleado:** Diseñado para el personal operativo de depósito y planta. Permite recibir mercadería de proveedores, asentar las elaboraciones de mixes del día, consultar existencias disponibles y despachar pedidos. Las áreas sensibles de precios y finanzas se encuentran protegidas y no son visibles para este perfil.

### Mi Perfil
En la esquina superior derecha de la pantalla puede hacer clic sobre su nombre para consultar los datos personales de su cuenta y el rol que tiene asignado.

---

## 2. Pantalla Principal (Panel de Control)

Al iniciar sesión verá un tablero con indicadores clave para el trabajo diario:

* **Alertas de Stock Bajo:** Muestra los insumos y productos terminados cuya cantidad actual cayó por debajo del stock mínimo recomendado.
* **Vencimientos Cercanos:** Avisos de lotes de materias primas próximos a expirar para que sean utilizados prioritariamente en planta.
* **Pedidos Pendientes:** Órdenes solicitadas por clientes que aún no han sido entregadas.

---

## 3. Materias Primas y Unidades de Medida

### Unidades de Medida (Solo Administrador)
Permite configurar las unidades con las que se compran y pesan los insumos (por ejemplo: kilogramos, gramos, unidades).
* Para crear una nueva, ingrese a la sección **Unidades de Medida**, presione **Nueva Unidad**, escriba el nombre o sigla y guarde.

### Catálogo de Materias Primas
Permite gestionar la lista de frutos secos, frutas deshidratadas y demás ingredientes utilizados en las recetas.

#### Registrar una Nueva Materia Prima (Solo Administrador)
1. Vaya a **Materias Primas** y seleccione **Nueva Materia Prima**.
2. Complete la información:
   * **Nombre:** Nombre claro del ingrediente (ej. *Almendra Nonpareil*).
   * **Unidad de Medida:** Elija la unidad que corresponda.
   * **Precio / Costo de Compra:** Valor monetario por unidad.
   * **Stock Mínimo:** Cantidad mínima deseada en depósito antes de que el sistema emita una alerta.
3. Presione **Guardar**.

> **Importante:** Al crear una materia prima, el stock siempre arranca en `0`. La carga de kilos reales se hace únicamente desde la sección de **Ingreso de Mercadería** para asegurar que cada lote cuente con fecha y vencimiento.

#### Modificación de Precios
* Al actualizar el costo de compra de una materia prima, el sistema recalcula en el momento el costo de producción de todos los mixes que llevan ese ingrediente en su receta.
* El stock acumulado y las fechas de compras anteriores no se pierden al editar los datos generales.

#### Dar de Baja una Materia Prima
* Si un insumo se utiliza en una receta activa, el sistema no permitirá borrarlo para evitar desconfigurar las mezclas. Para darlo de baja, primero debe quitarlo de las recetas correspondientes.

---

## 4. Recepción de Mercadería (Ingreso de Insumos)

Esta sección permite registrar la llegada de insumos enviados por los proveedores. Disponible tanto para **Administradores** como para **Empleados**.

### Cómo cargar un ingreso de compras:
1. Ingrese a **Ingreso de Materia Prima** y presione **Nuevo Ingreso**.
2. Agregue uno o varios renglones con los productos recibidos:
   * **Materia Prima:** Seleccione el insumo del listado.
   * **Cantidad:** Kilos o unidades exactas recibidas.
   * **Número de Lote:** Identificador del proveedor o número de remito.
   * **Fecha de Vencimiento:** Fecha límite indicada en el empaque.
3. Revise la lista y haga clic en **Confirmar Recepción**.

> **Atención:** Una vez confirmado el ingreso, el registro queda guardado de forma definitiva y no puede modificarse ni borrarse. Esto asegura que el stock contable coincida con las existencias reales.

---

## 5. Fórmulas y Costos Adicionales

### Costos Adicionales (Solo Administrador)
Permite sumar al producto final los gastos complementarios necesarios para empaquetarlo y despacharlo, como bolsas, etiquetas o mano de obra. Se configuran desde la sección **Costos Adicionales**.

### Fórmulas de Producción (Solo Administrador)
Representan las recetas de cada variedad de mix.

#### Crear o editar una fórmula:
1. Diríjase a **Fórmulas** y elija **Nueva Fórmula**.
2. Seleccione el **Mix** al que pertenece la receta.
3. Indique la **Cantidad de Producción Base** (por ejemplo: tanda de 10 kg).
4. Incorpore los ingredientes agregando cada materia prima y la cantidad en **gramos** requerida para esa tanda.
5. Guarde los cambios. Con esta información, el sistema calcula de forma exacta cuánto cuesta producir cada kilo de mix.

---

## 6. Catálogo de Mixes (Productos Terminados)

Sección para administrar los productos comercializados por la empresa (Solo Administrador).

### Alta y Edición de Mixes
1. Ingrese a **Mixes** y presione **Nuevo Mix**.
2. Complete la ficha del producto:
   * **Nombre:** Nombre comercial (ej. *Mix Clásico Energético*).
   * **Precio de Venta:** Precio de lista para los clientes.
   * **Cantidad por Unidad:** Peso neto del paquete terminado (por ejemplo: `1.0` si es un paquete de 1 kg o `0.5` si es de medio kilo).
3. El **Costo** de elaboración se calcula automáticamente combinando la receta vigente y los costos adicionales asociados. Cada cambio de precio queda anotado en un historial interno.

---

## 7. Planta de Elaboración Diaria

Módulo para registrar la producción física y el fraccionamiento de mezclas. Puede ser utilizado por **Administradores** y **Empleados**.

### Registrar una Elaboración:
1. Ingrese a **Elaboración** y presione **Registrar Elaboración**.
2. Seleccione:
   * El **Mix** que se va a producir.
   * La **Fecha** de elaboración.
   * La **Cantidad total en kilos** que se elaboró.
3. **Descuento de ingredientes del depósito:**
   * **Automático (Recomendado):** El sistema aplica la regla de vencimiento más próximo; consume primero los lotes de materia prima que vencen antes para evitar desperdicios.
   * **Manual:** Si en planta se utilizó un lote particular, el usuario puede ingresar manualmente qué cantidad exacta se retiró de cada partida.
4. Presione **Confirmar Elaboración**.

> **Protección de Inventario:** Si al intentar registrar la producción no hay suficiente stock de algún ingrediente, la operación se cancelará por completo avisando del faltante. Ningún stock se descontará a medias.

---

## 8. Ventas: Clientes y Pedidos

### Clientes (Solo Administrador)
Permite gestionar la libreta de clientes con razón social, número de CUIT, teléfono, domicilio y correo de contacto.

### Pedidos de Venta
Módulo para coordinar las ventas y las entregas a los compradores.

#### Cargar un nuevo pedido:
1. En **Pedidos**, elija **Nuevo Pedido**.
2. Seleccione el cliente y la fecha del pedido.
3. Agregue los productos solicitados indicando el mix y la cantidad de unidades o kilos. El sistema sugiere el precio de lista, pero puede ajustarse si hay un acuerdo especial.
4. Al guardar, la orden queda en estado **Pendiente**.

#### Entrega y Despacho
* Cuando el pedido se prepara y se entrega al cliente, debe cambiarse su estado a **Entregado**. En ese instante, los kilos correspondientes se descuentan automáticamente del stock terminado de la empresa.
* Si una orden es **Cancelada**, no descuenta mercadería y no suma al total de ventas cobradas.

---

## 9. Historial de Movimientos de Stock y Trazabilidad

Esta sección permite auditar de dónde provino y hacia dónde fue cada kilo de mercadería.

### Historial de Materia Prima
Permite seleccionar un ingrediente y un rango de fechas para ver su evolución:
* **Entradas (+):** Kilos ingresados por compras a proveedores, indicando el número de lote y el remito.
* **Salidas (−):** Kilos descontados por elaboraciones de mixes en planta.
* **Saldo:** Nivel de existencias que quedó en depósito luego de cada movimiento.

### Historial de Mixes
Permite auditar el inventario de producto terminado:
* **Entradas (+):** Kilos elaborados y empaquetados en planta.
* **Salidas (−):** Kilos despachados por pedidos entregados a clientes.

---

## 10. Informes y Estadísticas (Solo Administrador)

Módulo analítico para evaluar la marcha del negocio:

* **Balance Anual:**
  * Kilos totales producidos, comprados y vendidos durante el año.
  * Total de dinero facturado y costo absorbido en la producción.
  * Ganancia neta y porcentaje de margen comercial obtenido.
* **Evolución Mes a Mes:** Gráficos y tablas comparativas de ventas y rentabilidad a lo largo de los doce meses del año.
* **Detalle por Mix:** Identifica cuáles son las mezclas más vendidas, cuánto aportó cada una a la facturación y qué rentabilidad real dejó cada variedad.

---

## 11. Usuarios y Copias de Seguridad (Solo Administrador)

### Cuentas de Usuario
* **Creación:** Permite habilitar accesos para nuevos colaboradores asignándoles el rol de Administrador o Empleado.
* **Cambio de Contraseña:** Facilita restablecer la clave de cualquier colaborador en caso de olvido.
* **Reglas de Seguridad:**
  * Un administrador no puede desactivar su propio usuario mientras tenga la sesión iniciada.
  * El sistema no permite borrar ni desactivar al último administrador para evitar que nadie pueda configurar el sistema.
  * No se puede dar de baja a un empleado que haya registrado pedidos en el historial para resguardar la validez de los registros de venta.

### Copias de Seguridad (Backups)
* Desde esta pantalla se pueden consultar las copias de seguridad generadas en el servidor.
* **Restauración:** Permite volver el sistema a un punto anterior a partir de un archivo de respaldo. Al ser una operación delicada que sobrescribe los datos actuales, debe realizarse con precaución y confirmación previa del administrador.

---

## 12. Preguntas Frecuentes (FAQ)

#### ¿Por qué no puedo cargar el stock cuando creo una nueva materia prima?
Porque todo insumo almacenado debe tener trazabilidad (saber a qué proveedor se le compró, en qué fecha y cuándo vence). Por eso el artículo se da de alta en cero y el stock real se suma desde la pantalla **Ingreso de Materia Prima**.

#### Al intentar eliminar una materia prima aparece un mensaje de advertencia. ¿Qué debo hacer?
Significa que ese ingrediente forma parte de la receta de algún mix activo. Para poder darlo de baja, primero debe ir a la sección **Fórmulas**, quitar ese ingrediente de la receta y guardar. Luego de eso, el sistema le permitirá eliminar la materia prima.

#### ¿Cómo elige el sistema qué lote de insumos usar en la producción?
Aplica el principio de que lo primero que vence es lo primero que se utiliza. Al fabricar un mix, el sistema descuenta automáticamente los lotes de materias primas que tienen la fecha de vencimiento más cercana. Si en planta se utilizó otro lote por alguna razón puntual, se puede seleccionar de manera manual en el formulario.

#### ¿Qué ocurre si intento registrar una elaboración pero no alcanzan los ingredientes?
La operación se cancela en el momento y aparece una advertencia en pantalla indicando qué ingrediente falta. El sistema no descontará nada a medias ni inventará stock del producto terminado hasta que el faltante sea ingresado al depósito.

#### ¿Por qué un usuario con perfil de Empleado no puede ver fórmulas, clientes o reportes?
Por resguardo de la información de la empresa. La lista de clientes, los márgenes de ganancia, las fórmulas de las recetas y los precios de compra son datos reservados para el perfil de Administrador. El perfil de Empleado tiene a su disposición las pantallas necesarias para la recepción, fraccionamiento y despacho de mercadería.

#### Un empleado ya no trabaja en la planta y el sistema no me deja borrarlo. ¿Cómo procedo?
Si el colaborador alguna vez cargó o despachó un pedido, su usuario no puede eliminarse para que los comprobantes históricos sigan mostrando quién fue el responsable de la operación. Para impedir que ingrese al sistema, simplemente cámbiele la contraseña o modifique su estado a inactivo.
