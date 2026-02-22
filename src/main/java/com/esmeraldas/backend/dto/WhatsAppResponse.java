package com.esmeraldas.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppResponse {
    private Message messaging_product;
    private Recipient recipient;
    private MessageTo to;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recipient {
        private String id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageTo {
        private String phone_number;
    }
}
