package zo.ro.whatsappreplybot.apis;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import zo.ro.whatsappreplybot.R;
import zo.ro.whatsappreplybot.helpers.WhatsAppMessageHandler;
import zo.ro.whatsappreplybot.models.Message;

public class GeminiReplyGenerator {

    private static final String TAG = "MADARA";
    private final String API_KEY;
    private final String LLM_MODEL;
    private final WhatsAppMessageHandler messageHandler;
    private final String defaultReplyMessage;
    private final String aiReplyLanguage;
    private final String botName;

    public GeminiReplyGenerator(Context context, SharedPreferences sharedPreferences, WhatsAppMessageHandler whatsAppMessageHandler) {
        this.messageHandler = whatsAppMessageHandler;
        API_KEY = sharedPreferences.getString("api_key", "not-set").trim();
        LLM_MODEL = sharedPreferences.getString("llm_model", "gemini-1.5-flash");
        defaultReplyMessage = sharedPreferences.getString("default_reply_message", context.getString(R.string.default_bot_message));
        aiReplyLanguage = sharedPreferences.getString("ai_reply_language", "English");
        botName = sharedPreferences.getString("bot_name", "hum");
    }

    public void generateReply(String sender, String message, CustomReplyGenerator.OnReplyGeneratedListener listener) {

        messageHandler.getMessagesHistory(sender, messages -> {

            StringBuilder chatHistory = getChatHistory(messages);
            StringBuilder prompt = buildPrompt(sender, message, chatHistory);

            GenerativeModel gm = new GenerativeModel(LLM_MODEL, API_KEY);
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content content = new Content.Builder()
                    .addText(prompt.toString())
                    .build();

            Executor executor = Executors.newSingleThreadExecutor();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String resultText = result.getText();
                    listener.onReplyGenerated(resultText);
                    Log.d(TAG, "onSuccess: " + resultText);
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    listener.onReplyGenerated(defaultReplyMessage);
                    Log.e(TAG, "onFailure: ", t);
                }
            }, executor);
        });
    }

//    ----------------------------------------------------------------------------------------------

    private @NonNull StringBuilder buildPrompt(String sender, String message, StringBuilder chatHistory) {

        StringBuilder prompt = new StringBuilder();

        if (!chatHistory.toString().isEmpty()) {
            prompt.append("You are a WhatsApp auto-reply bot named ").append(botName);
            prompt.append(" who pretends to be human and doesn't disclose that you are a bot.");
            prompt.append(" Give appropriate replies according to the latest message and the previous chat history.");
            prompt.append(" Generate only reply texts. Don't add any other texts.");
            prompt.append(" Always reply in ").append(aiReplyLanguage).append(".");
            prompt.append(" Reply within 1 or 2 sentences whenever possible.");
            prompt.append(" Be polite, context-aware, and ensure your replies are relevant to the conversation.");
            Prompt.append("Reply only according to the content. Do not go off-topic.");  
            Prompt.append("Use light Indian slang words to keep the conversation natural.");  
            Prompt.append("Never disclose that you are an AI. Always reply as if you are the one being messaged.");  
            Prompt.append("If someone says 'kheloge,khelega,khelte hai' understand that they are inviting you to play Free Fire.");  
            Prompt.append("Words like 'aja, aaja, ajja, ana, anna, aana' means the person is inviting you to play Free Fire.");  
            Prompt.append("Do not reply to generic words like 'ok' or 'hmm.' If a reply is not needed, do not send one.");  
            Prompt.append("Use dark humor and sarcasm appropriately.");  
            Prompt.append("Be polite and context aware. Ensure your replies match the sender's tone casual, sarcastic, or serious.");  
            Prompt.append("Respond in a way that feels human and natural.");  
            Prompt.append("If the message allows, add a hint of sarcasm.");  
            Prompt.append("Do not provide unnecessary responses. If a message does not require a reply, ignore it.");  
            Prompt.append("FF is the short form of Free Fire. Always recognize this when mentioned.");  
            Prompt.append("Never use the word 'plan' in any reply.");  
            Prompt.append("Do not be lame. Think twice before replying—make sure your response is witty and engaging enough for a 26-year-old highly mature Indian guy with top-tier humor.");  
            prompt.append("\n\n\nMost recent message (from ");
            prompt.append(sender).append("): ");
            prompt.append(message);
            prompt.append("\n\n\nPrevious chat history: \n").append(chatHistory);
            return prompt;
        }

        prompt.append("You are a WhatsApp auto-reply bot named ").append(botName);
        prompt.append("Your task is replying to the incoming message. ");
        prompt.append("Always reply in ").append(aiReplyLanguage);
        prompt.append(". Be polite, context-aware, and ensure your replies are relevant to the conversation.\n\n");
        prompt.append("\n\n\nIncoming message (from ");
        prompt.append(sender).append("): ");
        prompt.append(message);
        return prompt;
    }

//    ----------------------------------------------------------------------------------------------

    private @NonNull StringBuilder getChatHistory(List<Message> messages) {

        StringBuilder chatHistory = new StringBuilder();

        if (!messages.isEmpty()) {

            for (Message msg : messages) {

                String senderName = msg.getSender();
                String senderMessage = msg.getMessage();
                String senderMessageTimestamp = msg.getTimestamp();
                String myReplyToSenderMessage = msg.getReply();

                chatHistory.append(senderName).append(": ").append(senderMessage);
                chatHistory.append("\n");
                chatHistory.append("Time: ").append(senderMessageTimestamp);
                chatHistory.append("\n");
                chatHistory.append("My reply: ").append(myReplyToSenderMessage);
                chatHistory.append("\n\n");
            }
        }
        return chatHistory;
    }

//    ----------------------------------------------------------------------------------------------
}
