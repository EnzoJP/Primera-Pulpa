package com.primeraPulpa.Services;

import com.primeraPulpa.dto.LoteDiarioDTO;
import com.primeraPulpa.entities.DetalleConsumoLote;
import com.primeraPulpa.entities.DetalleIngresoMP;
import com.primeraPulpa.entities.LoteMix;
import com.primeraPulpa.entities.MateriaPrima;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.exceptions.ErrorServiceException;
import com.primeraPulpa.repositories.DetalleConsumoLoteRepository;
import com.primeraPulpa.repositories.DetalleIngresoMPRepository;
import com.primeraPulpa.repositories.LoteMixRepository;
import com.primeraPulpa.repositories.MateriaPrimaRepository;
import com.primeraPulpa.repositories.MixRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LoteMixService extends BaseService<LoteMix, Long> {

    private final LoteMixRepository loteMixRepository;
    private final MixRepository mixRepository;
    private final MixService mixService;
    private final DetalleConsumoLoteRepository consumoLoteRepository;
    private final MateriaPrimaRepository materiaPrimaRepository;
    private final DetalleIngresoMPRepository detalleIngresoMPRepository;

    public LoteMixService(LoteMixRepository repository, MixRepository mixRepository, MixService mixService,
                          DetalleConsumoLoteRepository consumoLoteRepository,
                          MateriaPrimaRepository materiaPrimaRepository,
                          DetalleIngresoMPRepository detalleIngresoMPRepository) {
        super(repository);
        this.loteMixRepository = repository;
        this.mixRepository = mixRepository;
        this.mixService = mixService;
        this.consumoLoteRepository = consumoLoteRepository;
        this.materiaPrimaRepository = materiaPrimaRepository;
        this.detalleIngresoMPRepository = detalleIngresoMPRepository;
    }

    /**
     * Registra una elaboración (reparto de lotes automático FEFO).
     */
    @Transactional
    public LoteMix registrarElaboracion(Mix mix, LocalDate fecha, Double cantidad, Usuario usuario) throws ErrorServiceException {
        return registrarElaboracion(mix, fecha, cantidad, usuario, null);
    }

    /**
     * Registra una elaboración. El "lote del día" se compone de todos los LoteMix
     * de una misma fecha. Si ya existe un LoteMix del mismo mix para esa fecha,
     * se suma la cantidad al existente; si no, se crea uno nuevo.
     * Se descuenta materia prima usando el reparto manual (usoLotes) o FEFO, y
     * se registra el desglose DetalleConsumoLote por lote.
     */
    @Transactional
    public LoteMix registrarElaboracion(Mix mix, LocalDate fecha, Double cantidad, Usuario usuario,
                                        Map<Long, Double> usoLotes) throws ErrorServiceException {
        System.out.println("Registrando elaboración: mix=" + mix + ", fecha=" + fecha + ", cantidad=" + cantidad);
        if (mix == null || mix.getId() == null) {
            throw new ErrorServiceException("Debe indicar el mix elaborado.");
        }
        if (fecha == null) {
            throw new ErrorServiceException("Debe indicar la fecha de elaboración.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }

        // Se crea/actualiza el LoteMix ANTES de descontar: si el stock no alcanza,
        // la excepción revierte todo por la transacción (incluido este guardado).
        Optional<LoteMix> loteExistente = loteMixRepository
                .findByMixAndFechaElaboracionAndEliminadoFalse(mix, fecha);

        LoteMix lote;
        if (loteExistente.isPresent()) {
            lote = loteExistente.get();
            double actual = lote.getCantidadElaborada() != null ? lote.getCantidadElaborada() : 0.0;
            lote.setCantidadElaborada(actual + cantidad);
            lote = loteMixRepository.save(lote);
        } else {
            LoteMix nuevo = new LoteMix();
            nuevo.setMix(mix);
            nuevo.setFechaElaboracion(fecha);
            nuevo.setCantidadElaborada(cantidad);
            nuevo.setUsuario(usuario);
            nuevo.setEliminado(false);
            lote = loteMixRepository.save(nuevo);
        }

        // Verifica stock de materia prima, descuenta (FEFO o reparto manual) y
        // registra el desglose de lotes consumidos.
        mixService.actualizarStockMixElaboracion(mix, cantidad, lote, usoLotes);

        return lote;
    }

    /**
     * Agrupa las elaboraciones activas por fecha (lote del día), de más reciente a más antigua.
     */
    public List<LoteDiarioDTO> listarLotesPorDia() {
        return loteMixRepository.findAllByEliminadoFalseOrderByFechaElaboracionDescIdDesc()
                .stream()
                .collect(Collectors.groupingBy(LoteMix::getFechaElaboracion,
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> new LoteDiarioDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Actualiza una elaboración: revierte el efecto de la versión anterior
     * (stock del mix, stock de materia prima y restante de los lotes) y aplica
     * la nueva cantidad/reparto (manual o FEFO), regenerando el desglose.
     * La transacción garantiza que si el nuevo reparto no alcanza, el revertido
     * también se deshace.
     */
    @Transactional
    public LoteMix actualizarElaboracion(Long id, Mix mixNuevo, LocalDate fecha, Double cantidad,
                                         Map<Long, Double> usoLotes) throws ErrorServiceException {
        if (mixNuevo == null || mixNuevo.getId() == null) {
            throw new ErrorServiceException("Debe indicar el mix elaborado.");
        }
        if (fecha == null) {
            throw new ErrorServiceException("Debe indicar la fecha de elaboración.");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }
        LoteMix lote = loteMixRepository.findById(id)
                .orElseThrow(() -> new ErrorServiceException("Elaboración no encontrada."));
        if (Boolean.TRUE.equals(lote.getEliminado())) {
            throw new ErrorServiceException("La elaboración fue eliminada.");
        }

        double cantidadVieja = lote.getCantidadElaborada() != null ? lote.getCantidadElaborada() : 0.0;
        Mix mixViejo = lote.getMix();

        // 1) Revertir el stock del mix anterior.
        if (mixViejo != null && mixViejo.getId() != null && cantidadVieja > 0) {
            Mix mixViejoEntidad = mixRepository.findById(mixViejo.getId()).orElse(mixViejo);
            mixViejoEntidad.actualizarStock(-cantidadVieja);
            mixRepository.save(mixViejoEntidad);
        }

        // 2) Revertir el consumo registrado: reponer stock de MP y restante de lote.
        for (DetalleConsumoLote c : consumoLoteRepository.findByLoteMixId(id)) {
            if (c.getMateriaPrima() != null && c.getMateriaPrima().getId() != null) {
                MateriaPrima mp = materiaPrimaRepository.findById(c.getMateriaPrima().getId())
                        .orElse(c.getMateriaPrima());
                mp.actualizarStock(c.getCantidadConsumida());
                materiaPrimaRepository.save(mp);
            }
            if (c.getLote() != null && c.getLote().getId() != null) {
                DetalleIngresoMP loteEntidad = detalleIngresoMPRepository.findById(c.getLote().getId())
                        .orElse(c.getLote());
                loteEntidad.setCantidadRestante(redondear(loteEntidad.getRestante() + c.getCantidadConsumida()));
                detalleIngresoMPRepository.save(loteEntidad);
            }
            c.setEliminado(true);
            consumoLoteRepository.save(c);
        }

        // 3) Aplicar la nueva configuración y descontar (reparto manual o FEFO).
        lote.setMix(mixNuevo);
        lote.setFechaElaboracion(fecha);
        lote.setCantidadElaborada(cantidad);
        lote = loteMixRepository.save(lote);

        mixService.actualizarStockMixElaboracion(mixNuevo, cantidad, lote, usoLotes);
        return lote;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }

    /**
     * Todos los LoteMix activos de una fecha determinada (el lote del día completo),
     * con su desglose de lotes de materia prima consumidos cargado.
     */
    @Transactional(readOnly = true)
    public List<LoteMix> listarDelDia(LocalDate fecha) {
        List<LoteMix> lotes = loteMixRepository.findAllByEliminadoFalseAndFechaElaboracionOrderByIdAsc(fecha);
        for (LoteMix lote : lotes) {
            lote.setConsumos(consumoLoteRepository.findByLoteMixId(lote.getId()));
        }
        return lotes;
    }

    /**
     * Carga el desglose de consumo de lotes de una elaboración (formulario de edición).
     */
    @Transactional(readOnly = true)
    public void cargarConsumos(LoteMix lote) {
        if (lote != null && lote.getId() != null) {
            lote.setConsumos(consumoLoteRepository.findByLoteMixId(lote.getId()));
        }
    }

    @Override
    protected void validar(LoteMix lote) throws ErrorServiceException {
        if (lote.getMix() == null) {
            throw new ErrorServiceException("Debe indicar el mix elaborado.");
        }
        if (lote.getFechaElaboracion() == null) {
            throw new ErrorServiceException("Debe indicar la fecha de elaboración.");
        }
        if (lote.getCantidadElaborada() == null || lote.getCantidadElaborada() <= 0) {
            throw new ErrorServiceException("La cantidad elaborada debe ser mayor a cero.");
        }
    }

    @Override
    protected void postBaja(Long id) throws ErrorServiceException {
        // Al eliminar una elaboración, se descarta su desglose de consumo de lotes.
        for (DetalleConsumoLote consumo : consumoLoteRepository.findByLoteMixId(id)) {
            consumo.setEliminado(true);
            consumoLoteRepository.save(consumo);
        }
    }
}