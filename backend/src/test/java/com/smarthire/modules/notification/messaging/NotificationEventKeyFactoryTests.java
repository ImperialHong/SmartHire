package com.smarthire.modules.notification.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class NotificationEventKeyFactoryTests {

    @Test
    void generateShouldReturnStableKeyForSamePayload() {
        String first = NotificationEventKeyFactory.generate(
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );
        String second = NotificationEventKeyFactory.generate(
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );

        assertEquals(first, second);
    }

    @Test
    void generateShouldChangeWhenPayloadChanges() {
        String first = NotificationEventKeyFactory.generate(
            99L,
            "APPLICATION_SUBMITTED",
            "New application received",
            "Candidate User applied for Backend Engineer",
            "APPLICATION",
            200L
        );
        String second = NotificationEventKeyFactory.generate(
            99L,
            "APPLICATION_STATUS_CHANGED",
            "Application status updated",
            "Your application for Backend Engineer is now REVIEWING",
            "APPLICATION",
            200L
        );

        assertNotEquals(first, second);
    }
}
