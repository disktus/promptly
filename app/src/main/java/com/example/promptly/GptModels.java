package com.example.promptly;

import java.util.List;

public class GptModels {

    public static class GptRequest {
        public String model;
        public List<Message> messages;
        public double temperature;

        public GptRequest(String model, List<Message> messages, double temperature) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
        }
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class GptResponse {
        public String id;
        public String object;
        public long created;
        public String model;
        public List<Choice> choices;
    }

    public static class Choice {
        public int index;
        public Message message;
        public String finish_reason;
    }
}