package com.scheduler.booking;

public class TestEmailSimple {
    // This is just to test if emails work - simple templates
    public static String customerTemplate(String name, String session, String dateTime) {
        return String.format(
            "<html><body style='font-family: Arial;'>" +
            "<h2 style='color: #667eea;'>Booking Confirmed!</h2>" +
            "<p>Hello %s,</p>" +
            "<p>Your %s session is confirmed for %s.</p>" +
            "<p>Thank you for booking with us!</p>" +
            "</body></html>",
            name, session, dateTime
        );
    }

    public static String businessTemplate(String customerName, String session, String dateTime) {
        return String.format(
            "<html><body style='font-family: Arial;'>" +
            "<h2 style='color: #3b82f6;'>New Booking</h2>" +
            "<p>Customer: %s</p>" +
            "<p>Session: %s</p>" +
            "<p>Date/Time: %s</p>" +
            "</body></html>",
            customerName, session, dateTime
        );
    }
}
