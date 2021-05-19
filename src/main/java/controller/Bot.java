package controller;

import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot {

    public TelegramWebhookBot getBot() {
        return bot;
    }

    public void setBot(TelegramWebhookBot bot) {
        this.bot = bot;
    }

    private TelegramWebhookBot bot;

    public void init(String token, String username) {
        bot = new TelegramWebhookBot() {
            @Override
            public String getBotToken() {
                return token;
            }

            @Override
            public BotApiMethod onWebhookUpdateReceived(Update update) {
                return null;
            }

            @Override
            public String getBotUsername() {
                return username;
            }

            @Override
            public String getBotPath() {
                return null;
            }
        };
    }

    public void execute(SendMessage sendMessage) throws TelegramApiException {
        bot.execute(sendMessage);
    }
}
