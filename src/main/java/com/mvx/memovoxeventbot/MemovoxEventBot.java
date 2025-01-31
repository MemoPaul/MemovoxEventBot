/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mvx.memovoxeventbot;

/**
 *
 * @author pauladler
 */
//public class MemovoxEventBot {
  

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MemovoxEventBot handles participants:
 * - If they arrive via link ( /start <eventCode> ), it greets them, 
 *   associating them with that event.
 * - If they arrive with just /start, it asks for an event code. 
 * - Once a user is associated with an event, they can send a voice note 
 *   to be stored for that event.
 */
public class MemovoxEventBot extends TelegramLongPollingBot {

    // eventCode -> list of voice fileIds
    private static final Map<String, List<String>> voiceNotesByEvent = new HashMap<>();
    
    // userId -> current eventCode (if any)
    private static final Map<Long, String> userEventMap = new HashMap<>();

    @Override
    public String getBotUsername() {
        // Replace with your actual event bot username (without '@')
        return "Memovox_eventBot";
    }

    @Override
    public String getBotToken() {
        // Replace with your real event bot token from BotFather
        return System.getenv("BOT_TOKEN");
    }

    /**
     * This method is called whenever a new update/message arrives from Telegram.
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();

        // Check if user typed a text message
        if (message.hasText()) {
            String text = message.getText();
            if (text.startsWith("/start")) {
                handleStartCommand(message);
                return;
            }
            // If user typed something else, check if we are waiting for event code
            handleTextMessage(message);
        }
        else if (message.hasVoice()) {
            // If user sent a voice note
            handleVoiceNote(message);
        }
    }

    /**
     * Called if user types /start possibly with or without a code:
     *   e.g. "/start MyBirthday_John_Smith_2025-02-01"
     */
    private void handleStartCommand(Message message) {
        Long userId = message.getFrom().getId();
        String[] parts = message.getText().split("\\s+", 2); 
        // e.g. ["/start", "MyBirthday_John_Smith_2025-02-01"]

        if (parts.length < 2) {
            // No code provided
            sendText(message.getChatId(),
                "Welcome to Memovox Event Bot!\n" + 
                "You haven't provided an event code.\n" +
                "Please enter your event code now, or type /start <code> next time."
            );
            // userEventMap not set yet, we'll wait for them to type it
            return;
        }

        // We have an event code
        String eventCode = parts[1];
        userEventMap.put(userId, eventCode);

        sendText(message.getChatId(),
            "Hello participant! You've joined event code: " + eventCode + "\n" +
            "Please send me a short voice note for this occasion."
        );
    }

    /**
     * If the user typed some random text (not /start) and hasn't provided an event code yet.
     */
    private void handleTextMessage(Message message) {
        Long userId = message.getFrom().getId();
        String userText = message.getText();

        // Check if we already have an event code
        String currentCode = userEventMap.get(userId);
        if (currentCode == null) {
            // Maybe the user is typing the code now
            // We'll treat any text as an event code if they haven't given one
            userEventMap.put(userId, userText);
            sendText(message.getChatId(), 
                "Thanks! Event code set to: " + userText + "\n" +
                "Please send a voice note to record your message."
            );
        } else {
            // They already have an event code, maybe they're just texting instead of voice
            sendText(message.getChatId(),
                "You're already assigned to event code: " + currentCode + "\n" +
                "Please send a voice note, or type /start <anotherCode> to switch events."
            );
        }
    }

    /**
     * Handle voice notes: store them in memory associated with the user's event code.
     */
    private void handleVoiceNote(Message message) {
        Long userId = message.getFrom().getId();
        Voice voice = message.getVoice();

        if (voice == null) {
            // Shouldn't happen if hasVoice() is true, but just in case
            sendText(message.getChatId(), "No valid voice note detected.");
            return;
        }

        // Check if we have an event code for this user
        String eventCode = userEventMap.get(userId);
        if (eventCode == null) {
            // They haven't provided an event code yet
            sendText(message.getChatId(), 
                "I don't know which event you're participating in.\n" +
                "Type /start <yourEventCode> or just enter the code so I can record your note."
            );
            return;
        }

        // We have a voice note; store the fileId in the event's list
        String fileId = voice.getFileId();
        voiceNotesByEvent.computeIfAbsent(eventCode, k -> new ArrayList<>()).add(fileId);

        sendText(message.getChatId(),
            "Thank you for your voice note! It's been saved.\n" +
            "After the deadline, the organizer will receive all voice notes."
        );
    }

    /**
     * Helper method to send text responses.
     */
    private void sendText(Long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // -------------- MAIN METHOD --------------
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MemovoxEventBot());
            System.out.println("MemovoxEventBot is running...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
