import { useState } from "react";
import "../styles/BusinessCalendar.css";

interface Booking {
  id: string;
  sessionType?: {
    name: string;
    color?: string;
  };
  customer?: {
    firstName: string;
    lastName: string;
  };
  startTime: number; // Epoch timestamp in milliseconds
  endTime: number; // Epoch timestamp in milliseconds
  status: string;
}

interface BlockedSlot {
  id: string;
  startTime: number; // Epoch timestamp in milliseconds
  endTime: number; // Epoch timestamp in milliseconds
  reason?: string;
}

interface BusinessCalendarProps {
  bookings: Booking[];
  blockedSlots: BlockedSlot[];
  onBlockSlot: (date: Date, startTime: string, endTime: string) => void;
  onUnblockSlot: (slotId: string) => void;
}

export default function BusinessCalendar({
  bookings,
  blockedSlots,
  onBlockSlot,
  onUnblockSlot,
}: BusinessCalendarProps) {
  // Validate props and provide safe defaults
  const safeBookings = Array.isArray(bookings) ? bookings : [];
  const safeBlockedSlots = Array.isArray(blockedSlots) ? blockedSlots : [];

  console.log("BusinessCalendar rendering with:", {
    bookingsCount: safeBookings.length,
    blockedSlotsCount: safeBlockedSlots.length,
    bookingsRaw: bookings,
    blockedSlotsRaw: blockedSlots,
  });

  const [currentDate, setCurrentDate] = useState(new Date());
  const [view, setView] = useState<"week" | "day">("week");
  const [selectedSlot, setSelectedSlot] = useState<{
    dateStr: string;
    time: string;
  } | null>(null);

  const monthNames = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];

  const getWeekDays = (date: Date) => {
    try {
      const week = [];
      const start = new Date(date);

      if (isNaN(start.getTime())) {
        console.error("Invalid date in getWeekDays:", date);
        return Array(7)
          .fill(null)
          .map((_, i) => {
            const d = new Date();
            d.setDate(d.getDate() - d.getDay() + 1 + i);
            return d;
          });
      }

      start.setDate(start.getDate() - start.getDay() + 1); // Start from Monday

      for (let i = 0; i < 7; i++) {
        const day = new Date(start);
        day.setDate(start.getDate() + i);
        week.push(day);
      }
      return week;
    } catch (error) {
      console.error("Error in getWeekDays:", error);
      // Return current week as fallback
      return Array(7)
        .fill(null)
        .map((_, i) => {
          const d = new Date();
          d.setDate(d.getDate() - d.getDay() + 1 + i);
          return d;
        });
    }
  };

  const generateTimeSlots = () => {
    const slots = [];
    for (let hour = 6; hour < 22; hour++) {
      slots.push(`${hour.toString().padStart(2, "0")}:00`);
      slots.push(`${hour.toString().padStart(2, "0")}:30`);
    }
    return slots;
  };

  const getBookingsForSlot = (date: Date, time: string) => {
    try {
      const slotDate = new Date(date);
      const [hours, minutes] = time.split(":");
      slotDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);
      const slotEndTime = new Date(slotDate.getTime() + 30 * 60 * 1000); // 30 min slots

      console.log("Checking slot:", {
        slotDate: slotDate.toISOString(),
        slotEndTime: slotEndTime.toISOString(),
        bookingsCount: safeBookings.length,
      });

      const matchingBookings = safeBookings.filter((booking) => {
        if (!booking || !booking.startTime || !booking.endTime) {
          console.warn("Invalid booking data:", booking);
          return false;
        }
        try {
          // Convert epoch timestamps to Date objects
          const bookingStart = new Date(booking.startTime);
          const bookingEnd = new Date(booking.endTime);

          if (isNaN(bookingStart.getTime()) || isNaN(bookingEnd.getTime())) {
            console.warn("Invalid booking dates:", booking);
            return false;
          }

          // Check if booking overlaps with this slot
          // Booking overlaps if: booking starts before slot ends AND booking ends after slot starts
          const overlaps = bookingStart < slotEndTime && bookingEnd > slotDate;

          if (overlaps) {
            console.log("Found booking in slot:", {
              bookingStart: bookingStart.toISOString(),
              bookingEnd: bookingEnd.toISOString(),
              customer:
                booking.customer?.firstName + " " + booking.customer?.lastName,
              sessionType: booking.sessionType?.name,
            });
          }

          return overlaps;
        } catch (err) {
          console.error("Error processing booking:", booking, err);
          return false;
        }
      });

      return matchingBookings;
    } catch (error) {
      console.error("Error in getBookingsForSlot:", error);
      return [];
    }
  };

  const isSlotBlocked = (date: Date, time: string) => {
    try {
      const slotDate = new Date(date);
      const [hours, minutes] = time.split(":");
      slotDate.setHours(parseInt(hours), parseInt(minutes), 0, 0);

      return safeBlockedSlots.some((slot) => {
        if (!slot || !slot.startTime || !slot.endTime) {
          console.warn("Invalid blocked slot data:", slot);
          return false;
        }
        try {
          // Convert epoch timestamps to Date objects
          const blockStart = new Date(slot.startTime);
          const blockEnd = new Date(slot.endTime);

          if (isNaN(blockStart.getTime()) || isNaN(blockEnd.getTime())) {
            console.warn("Invalid blocked slot dates:", slot);
            return false;
          }
          return slotDate >= blockStart && slotDate < blockEnd;
        } catch (err) {
          console.error("Error processing blocked slot:", slot, err);
          return false;
        }
      });
    } catch (error) {
      console.error("Error in isSlotBlocked:", error);
      return false;
    }
  };

  const handlePreviousWeek = () => {
    try {
      console.log("Previous week clicked, current date:", currentDate);
      const newDate = new Date(currentDate);
      newDate.setDate(newDate.getDate() - 7);
      console.log("New date:", newDate);
      setCurrentDate(newDate);
    } catch (error) {
      console.error("Error in handlePreviousWeek:", error);
    }
  };

  const handleNextWeek = () => {
    try {
      console.log("Next week clicked, current date:", currentDate);
      const newDate = new Date(currentDate);
      newDate.setDate(newDate.getDate() + 7);
      console.log("New date:", newDate);
      setCurrentDate(newDate);
    } catch (error) {
      console.error("Error in handleNextWeek:", error);
    }
  };

  const handleToday = () => {
    setCurrentDate(new Date());
  };

  const weekDays = getWeekDays(currentDate);
  const timeSlots = generateTimeSlots();

  return (
    <div className="business-calendar">
      <div className="calendar-toolbar">
        <div className="calendar-nav">
          <button onClick={handleToday} className="today-button">
            Today
          </button>
          <div className="nav-buttons">
            <button onClick={handlePreviousWeek} className="nav-btn">
              ‹
            </button>
            <button onClick={handleNextWeek} className="nav-btn">
              ›
            </button>
          </div>
          <h2 className="calendar-title">
            {monthNames[weekDays[0].getMonth()]} {weekDays[0].getDate()} -{" "}
            {weekDays[6].getDate()}, {weekDays[0].getFullYear()}
          </h2>
        </div>

        <div className="view-switcher">
          <button
            className={`view-btn ${view === "week" ? "active" : ""}`}
            onClick={() => setView("week")}
          >
            Week
          </button>
          <button
            className={`view-btn ${view === "day" ? "active" : ""}`}
            onClick={() => setView("day")}
          >
            Day
          </button>
        </div>
      </div>

      <div className="calendar-legend">
        <div className="legend-item">
          <span className="legend-dot booked"></span>
          <span>Booked</span>
        </div>
        <div className="legend-item">
          <span className="legend-dot blocked"></span>
          <span>Blocked</span>
        </div>
        <div className="legend-item">
          <span className="legend-dot available"></span>
          <span>Available</span>
        </div>
      </div>

      <div className="calendar-grid-wrapper">
        <div className="calendar-grid">
          <div className="time-column">
            <div className="time-header"></div>
            {timeSlots.map((time) => (
              <div key={time} className="time-label">
                {time}
              </div>
            ))}
          </div>

          {weekDays.map((day, dayIndex) => (
            <div key={dayIndex} className="day-column">
              <div className="day-header">
                <div className="day-name">
                  {day.toLocaleDateString("en-US", { weekday: "short" })}
                </div>
                <div
                  className={`day-number ${
                    day.toDateString() === new Date().toDateString()
                      ? "today"
                      : ""
                  }`}
                >
                  {day.getDate()}
                </div>
              </div>

              {timeSlots.map((time, timeIndex) => {
                const slotBookings = getBookingsForSlot(day, time);
                const blocked = isSlotBlocked(day, time);

                // Create a new Date object to check if slot is in the past (don't mutate day)
                const slotDateTime = new Date(day);
                slotDateTime.setHours(
                  parseInt(time.split(":")[0]),
                  parseInt(time.split(":")[1]),
                  0,
                  0
                );
                const isPast = slotDateTime < new Date();

                return (
                  <div
                    key={timeIndex}
                    className={`time-slot ${blocked ? "blocked" : ""} ${
                      slotBookings.length > 0 ? "booked" : ""
                    } ${isPast ? "past" : ""}`}
                    onClick={() => {
                      if (!isPast && !blocked && slotBookings.length === 0) {
                        // Store date as ISO string to avoid mutation issues
                        setSelectedSlot({
                          dateStr: day.toISOString().split("T")[0],
                          time,
                        });
                      }
                    }}
                  >
                    {slotBookings.map((booking, idx) => (
                      <div
                        key={idx}
                        className="booking-item"
                        style={{
                          background: booking.sessionType?.color || "#4f46e5",
                        }}
                      >
                        <div className="booking-title">
                          {booking.sessionType?.name || "Unknown Session"}
                        </div>
                        <div className="booking-customer">
                          {booking.customer?.firstName || ""}{" "}
                          {booking.customer?.lastName || "Unknown Customer"}
                        </div>
                      </div>
                    ))}
                    {blocked && <div className="blocked-indicator">🚫</div>}
                  </div>
                );
              })}
            </div>
          ))}
        </div>
      </div>

      {selectedSlot && (
        <div className="modal" onClick={() => setSelectedSlot(null)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: "400px" }}
          >
            <div className="modal-header">
              <h3>Block Time Slot</h3>
              <button
                onClick={() => setSelectedSlot(null)}
                style={{
                  background: "none",
                  border: "none",
                  fontSize: "24px",
                  color: "#6b7280",
                  cursor: "pointer",
                  padding: "0",
                  lineHeight: "1",
                }}
              >
                ×
              </button>
            </div>
            <div style={{ padding: "24px" }}>
              <p style={{ color: "#6b7280", marginBottom: "16px" }}>
                {new Date(selectedSlot.dateStr).toLocaleDateString("en-US", {
                  weekday: "long",
                  month: "long",
                  day: "numeric",
                })}{" "}
                at {selectedSlot.time}
              </p>
              <div className="modal-footer">
                <button
                  onClick={() => setSelectedSlot(null)}
                  className="button button-secondary"
                >
                  Cancel
                </button>
                <button
                  onClick={() => {
                    // Parse the date and time correctly without timezone issues
                    const [hours, minutes] = selectedSlot.time.split(":");
                    const [year, month, day] = selectedSlot.dateStr
                      .split("-")
                      .map(Number);

                    // Create date in local timezone
                    const startDate = new Date(
                      year,
                      month - 1,
                      day,
                      parseInt(hours),
                      parseInt(minutes),
                      0,
                      0
                    );
                    const endDate = new Date(startDate);
                    endDate.setMinutes(endDate.getMinutes() + 30);

                    // Convert to epoch timestamps (milliseconds)
                    const startEpoch = startDate.getTime();
                    const endEpoch = endDate.getTime();

                    console.log("Blocking slot:", {
                      dateStr: selectedSlot.dateStr,
                      time: selectedSlot.time,
                      startDate: startDate.toISOString(),
                      endDate: endDate.toISOString(),
                      startEpoch,
                      endEpoch,
                    });

                    onBlockSlot(
                      startDate,
                      String(startEpoch),
                      String(endEpoch)
                    );
                    setSelectedSlot(null);
                  }}
                  className="button button-danger"
                >
                  Block Slot
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
