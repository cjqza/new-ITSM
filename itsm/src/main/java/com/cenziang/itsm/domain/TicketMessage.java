package com.cenziang.itsm.domain;

import java.time.Instant;

public record TicketMessage(
        MessageSender sender,
        String senderId,
        String content,
        Instant sentAt
) {
}
