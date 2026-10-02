package com.primeraPulpa.Services;

import com.primeraPulpa.entities.Cliente;
import com.primeraPulpa.entities.DetallePedido;
import com.primeraPulpa.entities.Mix;
import com.primeraPulpa.entities.Pedido;
import com.primeraPulpa.entities.Usuario;
import com.primeraPulpa.repositories.ClienteRepository;
import com.primeraPulpa.repositories.UsuarioRepository;
import com.primeraPulpa.util.Textos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta el mensaje de un pedido llegado por Telegram y lo registra.
 *
 * Formato de "un mensaje por pedido":
 * <pre>
 * Beto
 * 10 x 5 trop con mani
 * 10 x 5 popular con mani
 * </pre>
 * La primera línea es la persona que pide (alias del cliente); las siguientes
 * líneas son "unidades x kg-por-paquete nombre-libre-del-mix".
 */
@Service
public class TelegramPedidoService {

    private static final Pattern LINEA_ITEM =
            Pattern.compile("^\\s*(\\d+(?:[,.]\\d+)?)\\s*[xX]\\s*(\\d+(?:[,.]\\d+)?)\\s+(.+)$");

    // Usuario de sistema que registra los pedidos del bot (si no existe, se crea).
    private static final String USUARIO_BOT_EMAIL = "telegram@primera-pulpa.local";

    private static final double UMBRAL_ALIAS = 0.85;

    private final MixService mixService;
    private final ClienteRepository clienteRepository;
    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;

    public TelegramPedidoService(MixService mixService,
                                 ClienteRepository clienteRepository,
                                 PedidoService pedidoService,
                                 UsuarioRepository usuarioRepository) {
        this.mixService = mixService;
        this.clienteRepository = clienteRepository;
        this.pedidoService = pedidoService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Procesa el mensaje y devuelve las líneas de confirmación/error para
     * responder por Telegram. Devuelve lista vacía si el mensaje no parece un
     * pedido (el bot no responde).
     */
    @Transactional
    public List<String> procesarPedido(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }

        String alias = null;
        List<String> lineasItem = new ArrayList<>();
        for (String linea : texto.split("\\R")) {
            String l = linea.trim();
            if (l.isEmpty()) {
                continue;
            }
            if (alias == null) {
                alias = l;
            } else {
                lineasItem.add(l);
            }
        }

        if (alias == null || lineasItem.isEmpty()) {
            return List.of();
        }

        List<String> errores = new ArrayList<>();
        List<Item> items = new ArrayList<>();
        for (String l : lineasItem) {
            Matcher m = LINEA_ITEM.matcher(l);
            if (!m.matches()) {
                errores.add("No entendí esta línea: \"" + l + "\" (se espera algo como 10 x 5 nombre del mix)");
                continue;
            }
            try {
                double unidades = parseNumero(m.group(1));
                double presentacion = parseNumero(m.group(2));
                if (unidades <= 0 || presentacion <= 0) {
                    throw new NumberFormatException("cantidades inválidas");
                }
                items.add(new Item(unidades, presentacion, m.group(3).trim()));
            } catch (NumberFormatException e) {
                errores.add("Cantidad inválida en: \"" + l + "\"");
            }
        }

        // Resolver la persona → cliente.
        List<Cliente> coincidencias = resolverCliente(alias);
        if (coincidencias.isEmpty()) {
            List<String> resp = new ArrayList<>();
            resp.add("No encontré al cliente/persona \"" + alias + "\".");
            resp.add("Agregá ese nombre en el formulario del cliente (campo \"Otros nombres\").");
            resp.addAll(errores);
            return resp;
        }
        if (coincidencias.size() > 1) {
            List<String> resp = new ArrayList<>();
            resp.add("\"" + alias + "\" corresponde a varios clientes:");
            for (Cliente c : coincidencias) {
                resp.add("  • " + c.getNombre());
            }
            resp.add("Usá un nombre más específico.");
            resp.addAll(errores);
            return resp;
        }

        Cliente cliente = coincidencias.get(0);
        Usuario usuario = obtenerUsuarioBot();

        // Resolver cada línea del pedido a un mix.
        List<DetallePedido> detalles = new ArrayList<>();
        List<String> confirmaciones = new ArrayList<>();
        for (Item item : items) {
            MixService.MixMatch match = mixService.matchearMix(item.mixTexto(), item.presentacion());
            if (match == null) {
                errores.add("No reconocí el mix \"" + item.mixTexto() + "\".");
                continue;
            }
            Mix mix = match.mix();
            double cantidadKg = redondear(item.unidades() * item.presentacion());
            DetallePedido detalle = new DetallePedido();
            detalle.setMix(mix);
            detalle.setCantidad(cantidadKg);
            detalle.setPrecioUnitario(mix.getPrecioVenta() != null ? mix.getPrecioVenta() : 0.0);
            detalles.add(detalle);
            confirmaciones.add("✓ " + formatearNumero(item.unidades()) + " x "
                    + formatearNumero(item.presentacion()) + " → " + mix.getNombreConPresentacion()
                    + " (" + formatearNumero(cantidadKg) + " kg)");
        }

        if (detalles.isEmpty()) {
            List<String> resp = new ArrayList<>();
            resp.add("No se pudo registrar el pedido de \"" + alias + "\": ningún mix fue reconocido.");
            resp.addAll(errores);
            return resp;
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUsuario(usuario);
        pedido.setDetalles(detalles);
        pedidoService.registrar(pedido);

        List<String> resp = new ArrayList<>();
        resp.add("Pedido registrado para " + alias + " (cliente " + cliente.getNombre() + ").");
        resp.addAll(confirmaciones);
        if (!errores.isEmpty()) {
            resp.add("");
            resp.add("Problemas (no enviados):");
            resp.addAll(errores);
        }
        return resp;
    }

    /**
     * Devuelve los clientes activos que coinciden con la persona que pide
     * (nombre, u alguno de sus "Otros nombres").
     */
    private List<Cliente> resolverCliente(String alias) {
        String aliasNorm = Textos.normalizar(alias);
        if (aliasNorm.isEmpty()) {
            return List.of();
        }
        List<Cliente> coincidencias = new ArrayList<>();
        for (Cliente c : clienteRepository.findAll()) {
            if (Boolean.TRUE.equals(c.getEliminado())) {
                continue;
            }
            if (coincideAlias(c, aliasNorm)) {
                coincidencias.add(c);
            }
        }
        return coincidencias;
    }

    private boolean coincideAlias(Cliente cliente, String aliasNorm) {
        String nombreNorm = Textos.normalizar(cliente.getNombre());
        if (!nombreNorm.isEmpty() && coincideNombre(nombreNorm, aliasNorm)) {
            return true;
        }
        if (cliente.getOtrosNombres() != null && !cliente.getOtrosNombres().isBlank()) {
            for (String parte : cliente.getOtrosNombres().split("[,;]")) {
                String otroNorm = Textos.normalizar(parte);
                if (!otroNorm.isEmpty() && coincideNombre(otroNorm, aliasNorm)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean coincideNombre(String nombreNorm, String aliasNorm) {
        if (nombreNorm.equals(aliasNorm)) {
            return true;
        }
        if (nombreNorm.contains(aliasNorm) || aliasNorm.contains(nombreNorm)) {
            return true;
        }
        return Textos.similitudFuzzy(nombreNorm, aliasNorm) >= UMBRAL_ALIAS;
    }

    private Usuario obtenerUsuarioBot() {
        return usuarioRepository.findByEmail(USUARIO_BOT_EMAIL).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setNombre("Telegram Bot");
            u.setEmail(USUARIO_BOT_EMAIL);
            u.setPasswordHash("{noop}" + UUID.randomUUID());
            u.setEliminado(false);
            return usuarioRepository.save(u);
        });
    }

    private static double parseNumero(String s) {
        return Double.parseDouble(s.replace(',', '.').trim());
    }

    private static String formatearNumero(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    private static double redondear(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private record Item(double unidades, double presentacion, String mixTexto) {
    }
}