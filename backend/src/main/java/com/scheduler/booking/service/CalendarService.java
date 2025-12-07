package com.scheduler.booking.service;

import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class CalendarService {

    private final UidGenerator uidGenerator = new RandomUidGenerator();

    /**
     * Generate ICS calendar file for a booking
     *
     * @param eventTitle       Title of the event
     * @param eventDescription Description of the event
     * @param startTime        Event start time
     * @param endTime          Event end time
     * @param location         Event location
     * @param organizerEmail   Organizer's email
     * @param organizerName    Organizer's name
     * @param attendeeEmail    Attendee's email
     * @param attendeeName     Attendee's name
     * @return byte array of the ICS file
     */
    public byte[] generateIcsFile(
            String eventTitle,
            String eventDescription,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String location,
            String organizerEmail,
            String organizerName,
            String attendeeEmail,
            String attendeeName) {

        try {
            // Create a new calendar
            Calendar calendar = new Calendar();
            calendar.getProperties().add(new ProdId("-//Session Scheduler//iCal4j 3.2//EN"));
            calendar.getProperties().add(Version.VERSION_2_0);
            calendar.getProperties().add(CalScale.GREGORIAN);
            calendar.getProperties().add(Method.REQUEST);

            // Convert LocalDateTime to Date (using system default timezone)
            ZoneId zoneId = ZoneId.systemDefault();
            java.util.Date start = java.util.Date.from(startTime.atZone(zoneId).toInstant());
            java.util.Date end = java.util.Date.from(endTime.atZone(zoneId).toInstant());

            // Create the event
            VEvent event = new VEvent(new DateTime(start), new DateTime(end), eventTitle);

            // Add UID (required)
            event.getProperties().add(uidGenerator.generateUid());

            // Add description
            if (eventDescription != null && !eventDescription.isEmpty()) {
                event.getProperties().add(new Description(eventDescription));
            }

            // Add location
            if (location != null && !location.isEmpty()) {
                event.getProperties().add(new Location(location));
            }

            // Add organizer
            Organizer organizer = new Organizer(URI.create("mailto:" + organizerEmail));
            organizer.getParameters().add(new Cn(organizerName));
            event.getProperties().add(organizer);

            // Add attendee
            Attendee attendee = new Attendee(URI.create("mailto:" + attendeeEmail));
            attendee.getParameters().add(new Cn(attendeeName));
            attendee.getParameters().add(Role.REQ_PARTICIPANT);
            attendee.getParameters().add(new net.fortuna.ical4j.model.parameter.Rsvp(true));
            event.getProperties().add(attendee);

            // Add status
            event.getProperties().add(Status.VEVENT_CONFIRMED);

            // Add sequence
            event.getProperties().add(new Sequence(0));

            // Add timestamp
            event.getProperties().add(new DtStamp(new DateTime()));

            // Add event to calendar
            calendar.getComponents().add(event);

            // Output to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, outputStream);

            log.info("Generated ICS file for event: {}", eventTitle);
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating ICS file", e);
            throw new RuntimeException("Failed to generate calendar file", e);
        }
    }

    /**
     * Generate filename for ICS file
     */
    public String generateIcsFilename(String sessionName, LocalDateTime startTime) {
        String sanitizedName = sessionName.replaceAll("[^a-zA-Z0-9-]", "_");
        String dateStr = startTime.truncatedTo(ChronoUnit.DAYS).toString().replace("-", "");
        return "booking_" + sanitizedName + "_" + dateStr + ".ics";
    }
}
