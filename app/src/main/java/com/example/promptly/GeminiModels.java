package com.example.promptly;

import java.util.List;

public class GeminiModels {

    // === Request ===
    public static class GeminiRequest {
        public List<Content> contents;

        public GeminiRequest(List<Content> contents) {
            this.contents = contents;
        }
    }

    public static class Content {
        public List<Part> parts;

        public Content(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        public String text;

        public Part(String text) {
            this.text = text;
        }
    }

    // === Response ===
    public static class GeminiResponse {
        public List<Candidate> candidates;
    }

    public static class Candidate {
        public Content content;
        public String finishReason;
    }
}