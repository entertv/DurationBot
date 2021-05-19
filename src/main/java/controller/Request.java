package controller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import model.exceptions.ManySeparatorsException;
import model.exceptions.PathIsNotDefined;
import model.exceptions.SeparatorException;
import model.exceptions.SimilarPoints;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import model.DurationModel;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Request implements RequestStreamHandler {

    private static final String BOT_TOKEN = "1729543646:AAHsnbZEAQGr97WhTVytqMYmXkUDjHsFrto";
    private static final String BOT_USERNAME = "ImplemicaDirectionBot";

    private final DurationModel model = new DurationModel();
    private final ShowContent showContent = new ShowContent();
    private final DurationAPI durationAPI = new DurationAPI();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Bot bot = new Bot();

    public Request() {
    }

    public Request(Bot bot) {
        this.bot = bot;
    }

    private static final String[] separators = new String[]{
            ">",
            "...",
            "~",
            " to ",
            " - "
    };

    private String lastRequest = "";

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        bot.init(BOT_TOKEN, BOT_USERNAME);
        objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

        Update update = objectMapper.readValue(input, Update.class);
        Message message = update.getMessage();

        if (message != null) {
            String response = menu(message.getText());
            SendMessage sendMessage = sendMessageCreate(message, response);
            try {
                bot.execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public SendMessage sendMessageCreate(Message message, String response) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(response);
        return sendMessage;
    }


    public String menu(String request) {
        String response = null;

        if (lastRequest.isEmpty()) {
            if (request.equals(showContent.getHelpCommand())) {
                response = showContent.getHelpMessage();
            } else if (request.equals(showContent.getDurationCommand())) {
                lastRequest = request;
                response = showContent.getDurationMessage();
            } else {
                response = showContent.getHelpMessage();
            }
        } else {
            try {
                pathHandling(request);
                response = showContent.showDuration(model);
            } catch (SeparatorException e) {
                response = "You need to enter a separator.";
            } catch (ManySeparatorsException manySeparatorsException) {
                response = "You only need to enter one separator.";
            } catch (SimilarPoints similarPoints) {
                response = "You need to enter different travel points.";
            } catch (PathIsNotDefined pathIsNotDefined) {
                response = "Path is not defined.";
            } finally {
                lastRequest = "";
            }
        }

        return response;
    }

    public void pathHandling(String request) throws SeparatorException, SimilarPoints, PathIsNotDefined, ManySeparatorsException {
        model.setSeparators(separators);
        model.createPathPoints(request);

        String origin = model.getOrigin();
        String destination = model.getDestination();

        model.createDuration(
                durationAPI.getContent(origin, destination, DurationAPI.DRIVING_MODE),
                durationAPI.getContent(origin, destination, DurationAPI.TRANSIT_MODE),
                durationAPI.getContent(origin, destination, DurationAPI.BICYCLING_MODE),
                durationAPI.getContent(origin, destination, DurationAPI.WALKING_MODE)
        );
    }
}
