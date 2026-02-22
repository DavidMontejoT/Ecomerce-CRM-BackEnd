package com.esmeraldas.backend.dto;

import lombok.Data;

import java.util.Map;

@Data
public class WhatsAppMessageDto {
    private String object;
    private Entry[] entry;

    @Data
    public static class Entry {
        private String id;
        private Change[] changes;
    }

    @Data
    public static class Change {
        private String value;
        private Field field;
    }

    @Data
    public static class Field {
        private String[] messaging_product;
        private Message[] messages;
        private Contact[] contacts;
        private String status;
    }

    @Data
    public static class Message {
        private String id;
        private String from;
        private String text;
        private String type;
        private Map<String, Object> image;
    }

    @Data
    public static class Contact {
        private Profile profile;
        private String wa_id;
    }

    @Data
    public static class Profile {
        private String name;
    }
}
