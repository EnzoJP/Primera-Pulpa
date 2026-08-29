package com.primeraPulpa.configs;

import com.primeraPulpa.Services.MixService;
import com.primeraPulpa.Services.RolService;
import com.primeraPulpa.Services.UsuarioService;
import com.primeraPulpa.entities.*;
import com.primeraPulpa.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            UsuarioService usuarioService,
            RolService rolService,
            UnidadMedidaRepository unidadMedidaRepository,
            CostoAdicionalRepository costoAdicionalRepository,
            MateriaPrimaRepository materiaPrimaRepository,
            IngresoMPRepository ingresoMPRepository,
            DetalleIngresoMPRepository detalleIngresoMPRepository,
            MixRepository mixRepository,
            MixService mixService,
            FormulaRepository formulaRepository,
            DetalleFormulaRepository detalleFormulaRepository,
            ClienteRepository clienteRepository,
            EstadoPedidoRepository estadoPedidoRepository,
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detallePedidoRepository,
            LoteMixRepository loteMixRepository) {

        return args -> {
            logger.info("--- INICIALIZANDO DATOS DE PRUEBA EN PRIMERA PULPA ---");

            // 1. Unidades de Medida
            UnidadMedida umKg = unidadMedidaRepository.save(UnidadMedida.builder().descripcion("kg").build());
            unidadMedidaRepository.save(UnidadMedida.builder().descripcion("litros").build());
            unidadMedidaRepository.save(UnidadMedida.builder().descripcion("unidades").build());
            unidadMedidaRepository.save(UnidadMedida.builder().descripcion("gramos").build());
            logger.info("Unidades de medida cargadas.");

            // 2. Roles
            Rol rolAdmin = rolService.alta(Rol.builder().descripcion("ADMIN").build());
            Rol rolEmpleado = rolService.alta(Rol.builder().descripcion("EMPLEADO").build());
            logger.info("Roles ADMIN y EMPLEADO cargados.");

            // 3. Usuarios
            usuarioService.alta(Usuario.builder()
                    .nombre("Administrador Principal")
                    .email("admin@example.com")
                    .passwordHash("admin123")
                    .rol(rolAdmin)
                    .build());

            Usuario uEmpleado1 = usuarioService.alta(Usuario.builder()
                    .nombre("Juan Pérez (Operador)")
                    .email("empleado@example.com")
                    .passwordHash("empleado123")
                    .rol(rolEmpleado)
                    .build());

            Usuario uEmpleado2 = usuarioService.alta(Usuario.builder()
                    .nombre("María González (Producción)")
                    .email("maria@example.com")
                    .passwordHash("maria123")
                    .rol(rolEmpleado)
                    .build());

            // Usuario inactivo de prueba para verificar estilos en rojo
            Usuario uInactivo = usuarioService.alta(Usuario.builder()
                    .nombre("Lucas Martínez (Inactivo)")
                    .email("lucas@example.com")
                    .passwordHash("lucas123")
                    .rol(rolEmpleado)
                    .build());
            uInactivo.setEliminado(true);
            usuarioRepository.save(uInactivo);
            logger.info("Usuarios creados: admin@example.com, empleado@example.com, maria@example.com y lucas@example.com (desactivado).");

            // 4. Costos Adicionales
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Bolsa Doypack 1Kg zipper").valor(250.0).presentacion(PresentacionCosto.UNO_KG).build());
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Bolsa 5Kg zipper").valor(500.0).presentacion(PresentacionCosto.CINCO_KG).build());
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Etiqueta Autoadhesiva Full Color").presentacion(PresentacionCosto.TODOS).valor(100.0).build());
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Costo Laboral").valor(1500.0).presentacion(PresentacionCosto.TODOS).build());
            logger.info("Costos adicionales de empaque cargados.");

            // 5. Materias Primas
            MateriaPrima mpAlmendra = new MateriaPrima("Almendras Enteras Nonpareil", umKg, 12500.0, 120.0, 30.0, LocalDate.now().minusDays(15));
            mpAlmendra.setEliminado(false);
            materiaPrimaRepository.save(mpAlmendra);

            MateriaPrima mpNuez = new MateriaPrima("Nueces Chandler Mariposa", umKg, 11000.0, 85.0, 25.0, LocalDate.now().minusDays(15));
            mpNuez.setEliminado(false);
            materiaPrimaRepository.save(mpNuez);

            MateriaPrima mpCaju = new MateriaPrima("Castañas de Cajú W450 Tostadas", umKg, 14500.0, 60.0, 20.0, LocalDate.now().minusDays(12));
            mpCaju.setEliminado(false);
            materiaPrimaRepository.save(mpCaju);

            MateriaPrima mpPasas = new MateriaPrima("Pasas de Uva Sultanas", umKg, 4200.0, 150.0, 40.0, LocalDate.now().minusDays(10));
            mpPasas.setEliminado(false);
            materiaPrimaRepository.save(mpPasas);

            MateriaPrima mpMani = new MateriaPrima("Maní Tostado sin Sal Repelado", umKg, 3100.0, 200.0, 50.0, LocalDate.now().minusDays(10));
            mpMani.setEliminado(false);
            materiaPrimaRepository.save(mpMani);

            MateriaPrima mpGirasol = new MateriaPrima("Semillas de Girasol Peladas", umKg, 2800.0, 90.0, 20.0, LocalDate.now().minusDays(8));
            mpGirasol.setEliminado(false);
            materiaPrimaRepository.save(mpGirasol);

            // Materia prima con stock bajo (12 kg vs 25 kg mín.) para probar la alerta visual
            MateriaPrima mpArandanos = new MateriaPrima("Arándanos Rojos Deshidratados", umKg, 16500.0, 12.0, 25.0, LocalDate.now().minusDays(5));
            mpArandanos.setEliminado(false);
            materiaPrimaRepository.save(mpArandanos);
            logger.info("Materias primas cargadas con stock inicial.");

            // 6. Ingresos de Materia Prima con Lotes
            IngresoMP ingreso1 = IngresoMP.builder()
                    .fechaHora(LocalDateTime.now().minusDays(5))
                    .usuario(uEmpleado1)
                    .build();
            ingreso1.setEliminado(false);
            ingresoMPRepository.save(ingreso1);

            DetalleIngresoMP det1Ing1 = DetalleIngresoMP.builder()
                    .ingresoMP(ingreso1)
                    .materiaPrima(mpAlmendra)
                    .cantidad(100.0)
                    .costoUnitario(12000.0)
                    .cantidadRestante(100.0)
                    .fechaVencimiento(LocalDate.now().plusMonths(8))
                    .build();
            det1Ing1.setEliminado(false);
            detalleIngresoMPRepository.save(det1Ing1);

            DetalleIngresoMP det2Ing1 = DetalleIngresoMP.builder()
                    .ingresoMP(ingreso1)
                    .materiaPrima(mpNuez)
                    .cantidad(60.0)
                    .costoUnitario(10500.0)
                    .cantidadRestante(60.0)
                    .fechaVencimiento(LocalDate.now().plusMonths(6))
                    .build();
            det2Ing1.setEliminado(false);
            detalleIngresoMPRepository.save(det2Ing1);

            DetalleIngresoMP det3Ing1 = DetalleIngresoMP.builder()
                    .ingresoMP(ingreso1)
                    .materiaPrima(mpPasas)
                    .cantidad(100.0)
                    .costoUnitario(4000.0)
                    .cantidadRestante(100.0)
                    .fechaVencimiento(LocalDate.now().plusMonths(12))
                    .build();
            det3Ing1.setEliminado(false);
            detalleIngresoMPRepository.save(det3Ing1);

            IngresoMP ingreso2 = IngresoMP.builder()
                    .fechaHora(LocalDateTime.now().minusDays(2))
                    .usuario(uEmpleado2)
                    .build();
            ingreso2.setEliminado(false);
            ingresoMPRepository.save(ingreso2);

            DetalleIngresoMP det1Ing2 = DetalleIngresoMP.builder()
                    .ingresoMP(ingreso2)
                    .materiaPrima(mpCaju)
                    .cantidad(50.0)
                    .costoUnitario(14000.0)
                    .cantidadRestante(50.0)
                    .fechaVencimiento(LocalDate.now().plusMonths(10))
                    .build();
            det1Ing2.setEliminado(false);
            detalleIngresoMPRepository.save(det1Ing2);

            DetalleIngresoMP det2Ing2 = DetalleIngresoMP.builder()
                    .ingresoMP(ingreso2)
                    .materiaPrima(mpMani)
                    .cantidad(150.0)
                    .costoUnitario(3000.0)
                    .cantidadRestante(150.0)
                    .fechaVencimiento(LocalDate.now().plusMonths(9))
                    .build();
            det2Ing2.setEliminado(false);
            detalleIngresoMPRepository.save(det2Ing2);
            logger.info("Ingresos de materias primas y lotes cargados.");

            // 7. Mixes
            // Balance exacto: Elaborado - Pedidos preparados
            // Mix Clásico: 60 kg elaborados - 35 kg preparados (15kg Ped1 + 20kg Ped2) = 25 kg stock
            Mix mixClasico = new Mix();
            mixClasico.setNombre("Mix Clásico Energético");
            mixClasico.setPrecioVenta(14500.0);
            mixClasico.setStock(25.0);
            mixClasico.setEliminado(false);
            mixRepository.save(mixClasico);

            // Mix Premium: 35 kg elaborados - 10 kg preparados (10kg Ped1) = 25 kg stock
            Mix mixPremium = new Mix();
            mixPremium.setNombre("Mix Frutos Secos Premium");
            mixPremium.setPrecioVenta(22000.0);
            mixPremium.setStock(25.0);
            mixPremium.setEliminado(false);
            mixRepository.save(mixPremium);

            // Mix Fitness: 60 kg elaborados - 30 kg preparados (30kg Ped3) = 30 kg stock
            // (Los 10 kg de Pedido 2 están pendientes de preparar; al prepararlos baja a 20 kg)
            Mix mixFitness = new Mix();
            mixFitness.setNombre("Mix Fitness & Sport");
            mixFitness.setPrecioVenta(11500.0);
            mixFitness.setStock(30.0);
            mixFitness.setEliminado(false);
            mixRepository.save(mixFitness);
            logger.info("Mixes creados en catálogo con stock cuadrado con elaboraciones y pedidos.");

            // 8. Fórmulas (10 kg cada una)
            // Fórmula 1: Mix Clásico (4kg Maní, 3kg Pasas, 2kg Almendras, 1kg Nueces)
            Formula formulaClasico = new Formula();
            formulaClasico.setMix(mixClasico);
            formulaClasico.setCantidad(10.0);
            formulaClasico.setEliminado(false);
            formulaRepository.save(formulaClasico);

            List<DetalleFormula> dfList1 = List.of(
                    new DetalleFormula(formulaClasico, mpMani, 4000.0),
                    new DetalleFormula(formulaClasico, mpPasas, 3000.0),
                    new DetalleFormula(formulaClasico, mpAlmendra, 2000.0),
                    new DetalleFormula(formulaClasico, mpNuez, 1000.0)
            );
            dfList1.forEach(d -> { d.setEliminado(false); detalleFormulaRepository.save(d); });
            formulaClasico.setDetalles(new ArrayList<>(dfList1));

            // Fórmula 2: Mix Premium (3.5kg Almendras, 3.5kg Nueces, 3kg Castañas de Cajú)
            Formula formulaPremium = new Formula();
            formulaPremium.setMix(mixPremium);
            formulaPremium.setCantidad(10.0);
            formulaPremium.setEliminado(false);
            formulaRepository.save(formulaPremium);

            List<DetalleFormula> dfList2 = List.of(
                    new DetalleFormula(formulaPremium, mpAlmendra, 3500.0),
                    new DetalleFormula(formulaPremium, mpNuez, 3500.0),
                    new DetalleFormula(formulaPremium, mpCaju, 3000.0)
            );
            dfList2.forEach(d -> { d.setEliminado(false); detalleFormulaRepository.save(d); });
            formulaPremium.setDetalles(new ArrayList<>(dfList2));

            // Fórmula 3: Mix Fitness (4kg Maní, 3kg Pasas, 2kg Girasol, 1kg Arándanos)
            Formula formulaFitness = new Formula();
            formulaFitness.setMix(mixFitness);
            formulaFitness.setCantidad(10.0);
            formulaFitness.setEliminado(false);
            formulaRepository.save(formulaFitness);

            List<DetalleFormula> dfList3 = List.of(
                    new DetalleFormula(formulaFitness, mpMani, 4000.0),
                    new DetalleFormula(formulaFitness, mpPasas, 3000.0),
                    new DetalleFormula(formulaFitness, mpGirasol, 2000.0),
                    new DetalleFormula(formulaFitness, mpArandanos, 1000.0)
            );
            dfList3.forEach(d -> { d.setEliminado(false); detalleFormulaRepository.save(d); });
            formulaFitness.setDetalles(new ArrayList<>(dfList3));

            // Recalcular costos de los mixes en base a las fórmulas + costos adicionales
            mixService.recalcularTodosLosCostos();
            logger.info("Fórmulas creadas y costos recalculados para todos los mixes.");

            // 8.1 Elaboraciones de Mixes (Lotes de Producción)
            LoteMix lote1 = LoteMix.builder()
                    .mix(mixClasico)
                    .fechaElaboracion(LocalDate.now().minusDays(4))
                    .cantidadElaborada(30.0)
                    .usuario(uEmpleado1)
                    .build();
            lote1.setEliminado(false);
            loteMixRepository.save(lote1);

            LoteMix lote2 = LoteMix.builder()
                    .mix(mixPremium)
                    .fechaElaboracion(LocalDate.now().minusDays(4))
                    .cantidadElaborada(20.0)
                    .usuario(uEmpleado1)
                    .build();
            lote2.setEliminado(false);
            loteMixRepository.save(lote2);

            LoteMix lote3 = LoteMix.builder()
                    .mix(mixClasico)
                    .fechaElaboracion(LocalDate.now().minusDays(2))
                    .cantidadElaborada(30.0)
                    .usuario(uEmpleado2)
                    .build();
            lote3.setEliminado(false);
            loteMixRepository.save(lote3);

            LoteMix lote4 = LoteMix.builder()
                    .mix(mixFitness)
                    .fechaElaboracion(LocalDate.now().minusDays(2))
                    .cantidadElaborada(30.0)
                    .usuario(uEmpleado2)
                    .build();
            lote4.setEliminado(false);
            loteMixRepository.save(lote4);

            LoteMix lote5 = LoteMix.builder()
                    .mix(mixPremium)
                    .fechaElaboracion(LocalDate.now().minusDays(1))
                    .cantidadElaborada(15.0)
                    .usuario(uEmpleado2)
                    .build();
            lote5.setEliminado(false);
            loteMixRepository.save(lote5);

            LoteMix lote6 = LoteMix.builder()
                    .mix(mixFitness)
                    .fechaElaboracion(LocalDate.now().minusDays(1))
                    .cantidadElaborada(30.0)
                    .usuario(uEmpleado1)
                    .build();
            lote6.setEliminado(false);
            loteMixRepository.save(lote6);
            logger.info("Lotes de elaboración de mix cargados.");

            // 9. Clientes
            Cliente cliente1 = Cliente.builder()
                    .nombre("Panadería y Confitería San Cayetano")
                    .cuit("30-71458963-2")
                    .contacto("+54 9 11 4321-8765")
                    .build();
            cliente1.setEliminado(false);
            clienteRepository.save(cliente1);

            Cliente cliente2 = Cliente.builder()
                    .nombre("Dietética & Nutrición Salud Natural")
                    .cuit("30-68954123-8")
                    .contacto("compras@saludnatural.com.ar")
                    .build();
            cliente2.setEliminado(false);
            clienteRepository.save(cliente2);

            Cliente cliente3 = Cliente.builder()
                    .nombre("Supermercados Alvear Express")
                    .cuit("30-55443322-1")
                    .contacto("+54 9 341 555-1234")
                    .build();
            cliente3.setEliminado(false);
            clienteRepository.save(cliente3);

            Cliente cliente4 = Cliente.builder()
                    .nombre("Gimnasio & Bar MegaSport Gym")
                    .cuit("27-32111222-4")
                    .contacto("+54 9 11 9988-7766")
                    .build();
            cliente4.setEliminado(false);
            clienteRepository.save(cliente4);
            logger.info("Clientes registrados con información de contacto.");

            // 10. Estados de Pedido
            EstadoPedido epPendiente = new EstadoPedido();
            epPendiente.setDescripcion("PENDIENTE");
            epPendiente.setEliminado(false);
            estadoPedidoRepository.save(epPendiente);

            EstadoPedido epPreparado = new EstadoPedido();
            epPreparado.setDescripcion("PREPARADO");
            epPreparado.setEliminado(false);
            estadoPedidoRepository.save(epPreparado);

            EstadoPedido epEntregado = new EstadoPedido();
            epEntregado.setDescripcion("ENTREGADO");
            epEntregado.setEliminado(false);
            estadoPedidoRepository.save(epEntregado);

            EstadoPedido epCancelado = new EstadoPedido();
            epCancelado.setDescripcion("CANCELADO");
            epCancelado.setEliminado(false);
            estadoPedidoRepository.save(epCancelado);
            logger.info("Estados de pedido cargados.");

            // 11. Pedidos
            // Pedido 1: ENTREGADO (hace 3 días - 2/2 preparados)
            Pedido p1 = new Pedido();
            p1.setCliente(cliente2);
            p1.setEstadoPedido(epEntregado);
            p1.setFecha(LocalDate.now().minusDays(3));
            p1.setUsuario(uEmpleado1);
            p1.setEliminado(false);
            pedidoRepository.save(p1);

            DetallePedido dp1P1 = DetallePedido.builder()
                    .pedido(p1)
                    .mix(mixPremium)
                    .cantidad(10.0)
                    .precioUnitario(22000.0)
                    .preparado(true)
                    .build();
            dp1P1.setEliminado(false);
            detallePedidoRepository.save(dp1P1);

            DetallePedido dp2P1 = DetallePedido.builder()
                    .pedido(p1)
                    .mix(mixClasico)
                    .cantidad(15.0)
                    .precioUnitario(14500.0)
                    .preparado(true)
                    .build();
            dp2P1.setEliminado(false);
            detallePedidoRepository.save(dp2P1);

            // Pedido 2: PENDIENTE (hoy - 1/2 preparado para probar estado parcial 50%)
            Pedido p2 = new Pedido();
            p2.setCliente(cliente1);
            p2.setEstadoPedido(epPendiente);
            p2.setFecha(LocalDate.now());
            p2.setUsuario(uEmpleado1);
            p2.setEliminado(false);
            pedidoRepository.save(p2);

            DetallePedido dp1P2 = DetallePedido.builder()
                    .pedido(p2)
                    .mix(mixClasico)
                    .cantidad(20.0)
                    .precioUnitario(14500.0)
                    .preparado(true) // Preparado
                    .build();
            dp1P2.setEliminado(false);
            detallePedidoRepository.save(dp1P2);

            DetallePedido dp2P2 = DetallePedido.builder()
                    .pedido(p2)
                    .mix(mixFitness)
                    .cantidad(10.0)
                    .precioUnitario(11500.0)
                    .preparado(false) // Pendiente
                    .build();
            dp2P2.setEliminado(false);
            detallePedidoRepository.save(dp2P2);

            // Pedido 3: PREPARADO (ayer - 1/1 preparado)
            Pedido p3 = new Pedido();
            p3.setCliente(cliente3);
            p3.setEstadoPedido(epPreparado);
            p3.setFecha(LocalDate.now().minusDays(1));
            p3.setUsuario(uEmpleado2);
            p3.setEliminado(false);
            pedidoRepository.save(p3);

            DetallePedido dp1P3 = DetallePedido.builder()
                    .pedido(p3)
                    .mix(mixFitness)
                    .cantidad(30.0)
                    .precioUnitario(11500.0)
                    .preparado(true)
                    .build();
            dp1P3.setEliminado(false);
            detallePedidoRepository.save(dp1P3);

            logger.info("Pedidos de prueba con detalles creados exitosamente.");
            logger.info("--- INICIALIZACIÓN COMPLETADA CON ÉXITO ---");
        };
    }
}
