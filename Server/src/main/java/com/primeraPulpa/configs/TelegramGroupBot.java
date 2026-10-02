package com.primeraPulpa.configs;

import com.primeraPulpa.Services.TelegramPedidoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

/**
 * Bot de telegram que escucha pedidos. Un mensaje = un pedido, con la persona
 * (alias del cliente) en la primera línea y luego líneas "10 x 5 nombre mix".
 */
@Component
public class TelegramGroupBot extends DefaultLongPollingUpdateConsumer implements SpringLongPollingBot {

    private final String botToken;
    private final TelegramClient telegramClient;
    private final TelegramPedidoService telegramPedidoService;

    public TelegramGroupBot(@Value("${telegram.bot.token}") String botToken,
                            TelegramPedidoService telegramPedidoService) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.telegramPedidoService = telegramPedidoService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String texto = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        try {
            List<String> lineas = telegramPedidoService.procesarPedido(texto);
            // Mensajes que no parecen pedidos → el bot no responde.
            if (lineas == null || lineas.isEmpty()) {
                return;
            }
            enviar(chatId, String.join("\n", lineas));
        } catch (Exception e) {
            try {
                enviar(chatId, "Error procesando el mensaje: " + e.getMessage());
            } catch (Exception ignored) {
                // si tampoco se puede responder, se descarta
            }
        }
    }

    private void enviar(Long chatId, String texto) {
        try {
            telegramClient.execute(SendMessage.builder().chatId(chatId).text(texto).build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}