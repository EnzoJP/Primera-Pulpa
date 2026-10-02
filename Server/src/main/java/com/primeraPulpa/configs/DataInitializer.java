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
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detallePedidoRepository,
            LoteMixRepository loteMixRepository,
            DetalleConsumoLoteRepository detalleConsumoLoteRepository) { // Inyectado según el diagrama

        return args -> {
            if (usuarioRepository.count() > 0) {
                logger.info("Base de datos ya inicializada. Omitiendo seed.");
                return;
            }
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

            Usuario uInactivo = usuarioService.alta(Usuario.builder()
                    .nombre("Lucas Martínez (Inactivo)")
                    .email("lucas@example.com")
                    .passwordHash("lucas123")
                    .rol(rolEmpleado)
                    .build());
            uInactivo.setEliminado(true);
            usuarioRepository.save(uInactivo);
            logger.info("Usuarios creados exitosamente.");

            // 4. Costos Adicionales
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Bolsa Doypack 1Kg zipper").valor(250.0).presentacion(PresentacionCosto.UNO_KG).build());
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Bolsa 5Kg zipper").valor(500.0).presentacion(PresentacionCosto.CINCO_KG).build());
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Etiqueta Autoadhesiva Full Color").presentacion(PresentacionCosto.TODOS).valor(100.0).build());
            costoAdicionalRepository.save(CostoAdicional.builder().descripcion("Costo Laboral").valor(1500.0).presentacion(PresentacionCosto.TODOS).build());
            logger.info("Costos adicionales cargados.");

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

            MateriaPrima mpArandanos = new MateriaPrima("Arándanos Rojos Deshidratados", umKg, 16500.0, 12.0, 25.0, LocalDate.now().minusDays(5));
            mpArandanos.setEliminado(false);
            materiaPrimaRepository.save(mpArandanos);
            logger.info("Materias primas cargadas.");

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
                    .numeroLote("LOT-ALM-001")
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
                    .numeroLote("LOT-NUEZ-001")
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
                    .numeroLote("LOT-PAS-001")
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
                    .numeroLote("LOT-CAJ-001")
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
                    .numeroLote("LOT-MANI-001")
                    .fechaVencimiento(LocalDate.now().plusMonths(9))
                    .build();
            det2Ing2.setEliminado(false);
            detalleIngresoMPRepository.save(det2Ing2);
            logger.info("Ingresos y lotes de materia prima registrados.");

            // 7. Mixes
            Mix mixClasico = new Mix();
            mixClasico.setNombre("Mix Clásico Energético");
            mixClasico.setPrecioVenta(14500.0);
            mixClasico.setStock(25.0);
            mixClasico.setCantidadPorUnidad(1.0);
            mixClasico.setEliminado(false);
            mixRepository.save(mixClasico);

            Mix mixPremium = new Mix();
            mixPremium.setNombre("Mix Frutos Secos Premium");
            mixPremium.setPrecioVenta(11000.0);
            mixPremium.setCantidadPorUnidad(5.0);
            mixPremium.setStock(25.0);
            mixPremium.setEliminado(false);
            mixRepository.save(mixPremium);

            Mix mixFitness = new Mix();
            mixFitness.setNombre("Mix Fitness & Sport");
            mixFitness.setPrecioVenta(11500.0);
            mixFitness.setStock(30.0);
            mixFitness.setCantidadPorUnidad(1.0);
            mixFitness.setEliminado(false);
            mixRepository.save(mixFitness);

            // 8. Fórmulas
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

            mixService.recalcularTodosLosCostos();

            // 8.1 Elaboraciones (LoteMix) y Consumo de Lotes (DetalleConsumoLote)
            // Lote 1: Mix Clásico (30 kg elaborados)
            LoteMix lote1 = LoteMix.builder()
                    .mix(mixClasico)
                    .fechaElaboracion(LocalDate.now().minusDays(4))
                    .cantidadElaborada(30.0)
                    .usuario(uEmpleado1)
                    .build();
            lote1.setEliminado(false);
            loteMixRepository.save(lote1);

            // Registro de consumos de lote para Lote 1:
            // Para 30 kg según fórmula (ratio x3): 12kg Maní, 9kg Pasas, 6kg Almendras, 3kg Nueces
            detalleConsumoLoteRepository.save(DetalleConsumoLote.builder()
                    .loteMix(lote1)
                    .lote(det2Ing2) // o detalleIngresoMP según tu nombre de propiedad
                    .materiaPrima(mpMani)
                    .cantidadConsumida(12.0)
                    .build());
            det2Ing2.setCantidadRestante(det2Ing2.getCantidadRestante() - 12.0);
            detalleIngresoMPRepository.save(det2Ing2);

            detalleConsumoLoteRepository.save(DetalleConsumoLote.builder()
                    .loteMix(lote1)
                    .lote(det3Ing1)
                    .materiaPrima(mpPasas)
                    .cantidadConsumida(9.0)
                    .build());
            det3Ing1.setCantidadRestante(det3Ing1.getCantidadRestante() - 9.0);
            detalleIngresoMPRepository.save(det3Ing1);

            detalleConsumoLoteRepository.save(DetalleConsumoLote.builder()
                    .loteMix(lote1)
                    .lote(det1Ing1)
                    .materiaPrima(mpAlmendra)
                    .cantidadConsumida(6.0)
                    .build());
            det1Ing1.setCantidadRestante(det1Ing1.getCantidadRestante() - 6.0);
            detalleIngresoMPRepository.save(det1Ing1);

            detalleConsumoLoteRepository.save(DetalleConsumoLote.builder()
                    .loteMix(lote1)
                    .lote(det2Ing1)
                    .materiaPrima(mpNuez)
                    .cantidadConsumida(3.0)
                    .build());
            det2Ing1.setCantidadRestante(det2Ing1.getCantidadRestante() - 3.0);
            detalleIngresoMPRepository.save(det2Ing1);

            // Lote 2: Mix Premium (20 kg elaborados)
            LoteMix lote2 = LoteMix.builder()
                    .mix(mixPremium)
                    .fechaElaboracion(LocalDate.now().minusDays(4))
                    .cantidadElaborada(20.0)
                    .usuario(uEmpleado1)
                    .build();
            lote2.setEliminado(false);
            loteMixRepository.save(lote2);

            // Consumos Lote 2 (ratio x2): 7kg Almendras, 7kg Nueces, 6kg Cajú
            detalleConsumoLoteRepository.save(DetalleConsumoLote.builder()
                    .loteMix(lote2)
                    .lote(det1Ing1)
                    .materiaPrima(mpAlmendra)
                    .cantidadConsumida(7.0)
                    .build());
            det1Ing1.setCantidadRestante(det1Ing1.getCantidadRestante() - 7.0);
            detalleIngresoMPRepository.save(det1Ing1);

            detalleConsumoLoteRepository.save(DetalleConsumoLote.builder()
                    .loteMix(lote2)
                    .lote(det2Ing1)
                    .materiaPrima(mpNuez)
                    .cantidadConsumida(7.0)
                    .build());
            det2Ing1.setCantidadRestante(det2Ing1.getCantidadRestante() - 7.0);
            detalleIngresoMPRepository.save(det2Ing1);

            detalleConsumoLoteRepository.save(DetalleConsumoLote.builder()
                    .loteMix(lote2)
                    .lote(det1Ing2)
                    .materiaPrima(mpCaju)
                    .cantidadConsumida(6.0)
                    .build());
            det1Ing2.setCantidadRestante(det1Ing2.getCantidadRestante() - 6.0);
            detalleIngresoMPRepository.save(det1Ing2);

            logger.info("Lotes de elaboración y consumos de lotes de MP registrados.");

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
            logger.info("Clientes registrados.");

            // 10. Pedidos (usando EstadoPedido enum)
            // Pedido 1: ENTREGADO
            Pedido p1 = new Pedido();
            p1.setCliente(cliente2);
            p1.setEstadoPedido(EstadoPedido.ENTREGADO);
            p1.setFecha(LocalDate.now().minusDays(3));
            p1.setUsuario(uEmpleado1);
            p1.setEliminado(false);
            pedidoRepository.save(p1);

            DetallePedido dp1P1 = DetallePedido.builder()
                    .pedido(p1)
                    .mix(mixPremium)
                    .cantidad(10.0)
                    .precioUnitario(55000.0)
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

            // Pedido 2: PENDIENTE
            Pedido p2 = new Pedido();
            p2.setCliente(cliente1);
            p2.setEstadoPedido(EstadoPedido.PENDIENTE);
            p2.setFecha(LocalDate.now());
            p2.setUsuario(uEmpleado1);
            p2.setEliminado(false);
            pedidoRepository.save(p2);

            DetallePedido dp1P2 = DetallePedido.builder()
                    .pedido(p2)
                    .mix(mixClasico)
                    .cantidad(20.0)
                    .precioUnitario(14500.0)
                    .preparado(true)
                    .build();
            dp1P2.setEliminado(false);
            detallePedidoRepository.save(dp1P2);

            DetallePedido dp2P2 = DetallePedido.builder()
                    .pedido(p2)
                    .mix(mixFitness)
                    .cantidad(10.0)
                    .precioUnitario(11500.0)
                    .preparado(false)
                    .build();
            dp2P2.setEliminado(false);
            detallePedidoRepository.save(dp2P2);

            // Pedido 3: PREPARADO
            Pedido p3 = new Pedido();
            p3.setCliente(cliente3);
            p3.setEstadoPedido(EstadoPedido.PREPARADO);
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

            logger.info("Pedidos de prueba con detalles creados.");
            logger.info("--- INICIALIZACIÓN COMPLETADA CON ÉXITO ---");
        };
    }
}