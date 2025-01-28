/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mvx.memovoxorgbot;



import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;
import java.util.Map;

public class MemovoxOrgBot extends TelegramLongPollingBot {

// Data structure for conversation state
    private static class OrgState {
        String occasion;
        String firstName;
        String lastName;
        String deadline;
        String participantsCount;
        // to track which question we are asking next
        int step = 0; 
    }

//Data structure to store the event info
    private static class EventData {
        String occasion;
        String firstName;
        String lastName;
        String deadline;
        String participantsCount;
        String eventCode; //         unique code
    }


    // Store user conversation states: userId -> OrgState
    private static final Map<Long, OrgState> userStates = new HashMap<>();

    // Store events by their unique code
    public static final Map<String, EventData> events = new HashMap<>();

    @Override
    public String getBotUsername() {
        // Bot username (without '@'), from BotFather
        return "Memovox_orgBot";
    }

    @Override
    public String getBotToken() {
        // Bot token from BotFather
        return "7219759494:AAGs4bUzJLrbT-bCao5Fxm1SuPKgKOjdsAw";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }

        long userId = message.getFrom().getId();
        String userText = message.getText();
        OrgState orgState = userStates.getOrDefault(userId, new OrgState());

        if (userText.startsWith("/start")) {
            // Start conversation
            orgState.step = 0;
            userStates.put(userId, orgState);
            sendText(message.getChatId(), 
                    "Hello! Let's create your event.\n" + 
                    "What is the occasion?");
            return;
        }

        switch (orgState.step) {
            case 0:
                // We expect the occasion
                orgState.occasion = userText;
                orgState.step = 1;
                sendText(message.getChatId(), "Great! Now, please enter the recipient's first name:");
                break;

            case 1:
                orgState.firstName = userText;
                orgState.step = 2;
                sendText(message.getChatId(), "Got it. Now, the recipient's last name:");
                break;

            case 2:
                orgState.lastName = userText;
                orgState.step = 3;
                sendText(message.getChatId(), "Perfect. Next, what's the deadline? (e.g. 2025-03-31)");
                break;

            case 3:
                orgState.deadline = userText;
                orgState.step = 4;
                sendText(message.getChatId(), "How many participants do you expect?");
                break;

            case 4:
                orgState.participantsCount = userText;
                // All data collected
                String eventCode = generateEventCode(
                    orgState.occasion, 
                    orgState.firstName, 
                    orgState.lastName, 
                    orgState.deadline
                );

                // Store the event data
                EventData eventData = new EventData();
                eventData.occasion = orgState.occasion;
                eventData.firstName = orgState.firstName;
                eventData.lastName = orgState.lastName;
                eventData.deadline = orgState.deadline;
                eventData.participantsCount = orgState.participantsCount;
                eventData.eventCode = eventCode;

                events.put(eventCode, eventData);

                
                
                
                
                
                
                // Provide link to the second bot
                String inviteLink = "https://t.me/Memovox_eventBot?start=" + eventCode;

                
                
                
                
                // End conversation
                orgState.step = 5;
                sendText(message.getChatId(), 
                    "Your event is created! The unique code is: " + eventCode + "\n\n" +
                    "Share this link with participants:\n" + inviteLink + "\n" +
                    "They will join the second bot with your specific event code."
                );
                break;

            default:
                sendText(message.getChatId(), "You've already created the event. Type /start to create another.");
                break;
        }

        userStates.put(userId, orgState);
    }

    private String generateEventCode(String occasion, String firstName, String lastName, String deadline) {
        // A simple example: combine the strings, replace spaces with underscores
        // In practice, you might use a random or hashed string
        return (occasion + "_" + firstName + "_" + lastName + "_" + deadline)
                .replaceAll("\\s+", "_");
    }

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

// -------------- MAIN METHOD TO RUN THIS BOT --------------
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MemovoxOrgBot());
            System.out.println("MemovoxOrgBot is running...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
