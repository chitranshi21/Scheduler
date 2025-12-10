import { useState, useEffect } from 'react';
import '../styles/BookingCalendar.css';
import { customerAPI } from '../services/api';
import type { BusinessHours, BlockedSlot } from '../types';
import { getUserTimezone, getTimezoneAbbreviation } from '../utils/timezone';

interface TimeSlot {
  time: string;
  available: boolean;
  reason?: string;
}

interface BookingCalendarProps {
  sessionDurationMinutes: number;
  onSelectSlot: (datetime: string) => void;
  tenantId: string;
  sessionTypeId?: string;
}

export default function BookingCalendar({
  sessionDurationMinutes,
  onSelectSlot,
  tenantId,
  sessionTypeId
}: BookingCalendarProps) {
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [currentMonth, setCurrentMonth] = useState<Date>(new Date());
  const [availableSlots, setAvailableSlots] = useState<TimeSlot[]>([]);
  const [selectedTime, setSelectedTime] = useState<string | null>(null);
  const [businessHours, setBusinessHours] = useState<BusinessHours[]>([]);
  const [blockedSlots, setBlockedSlots] = useState<BlockedSlot[]>([]);

  // Load business hours and blocked slots
  useEffect(() => {
    const loadAvailability = async () => {
      try {
        const [hoursRes, slotsRes] = await Promise.all([
          customerAPI.getBusinessHours(tenantId),
          customerAPI.getBlockedSlots(tenantId)
        ]);
        setBusinessHours(hoursRes.data);
        setBlockedSlots(slotsRes.data);
      } catch (error) {
        console.error('Failed to load availability data:', error);
      }
    };

    if (tenantId) {
      loadAvailability();
    }
  }, [tenantId]);

  // Check if a time is within business hours
  const isWithinBusinessHours = (date: Date, time: string): boolean => {
    const dayOfWeek = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][date.getDay()] as BusinessHours['dayOfWeek'];

    const dayHours = businessHours.filter(h => h.dayOfWeek === dayOfWeek && h.enabled);

    if (dayHours.length === 0) {
      return false; // No business hours set for this day
    }

    // Check if time falls within any of the business hour ranges for this day
    return dayHours.some(hour => {
      return time >= hour.startTime && time < hour.endTime;
    });
  };

  // Check if a time slot is blocked
  const isSlotBlocked = (date: Date, time: string): boolean => {
    const [hours, minutes] = time.split(':').map(Number);
    const slotDate = new Date(date);
    slotDate.setHours(hours, minutes, 0, 0);
    const slotTime = slotDate.getTime();

    return blockedSlots.some(blocked => {
      return slotTime >= blocked.startTime && slotTime < blocked.endTime;
    });
  };

  // Generate time slots for a given date
  const generateTimeSlots = (date: Date): TimeSlot[] => {
    const slots: TimeSlot[] = [];
    const startHour = 0; // Start from midnight
    const endHour = 24; // End at midnight next day

    for (let hour = startHour; hour < endHour; hour++) {
      for (let minute = 0; minute < 60; minute += 30) {
        const timeString = `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
        const slotDate = new Date(date);
        slotDate.setHours(hour, minute, 0, 0);

        // Check if slot is in the past
        const isPast = slotDate <= new Date();

        // Check if slot is within business hours
        const withinBusinessHours = isWithinBusinessHours(date, timeString);

        // Check if slot is blocked
        const blocked = isSlotBlocked(date, timeString);

        const isAvailable = !isPast && withinBusinessHours && !blocked;

        slots.push({
          time: timeString,
          available: isAvailable,
          reason: isPast ? 'Past' : !withinBusinessHours ? 'Outside business hours' : blocked ? 'Blocked' : undefined
        });
      }
    }

    return slots;
  };

  useEffect(() => {
    // Generate slots for selected date when date, business hours, or blocked slots change
    const slots = generateTimeSlots(selectedDate);
    setAvailableSlots(slots);
    setSelectedTime(null);
  }, [selectedDate, businessHours, blockedSlots]);

  const getDaysInMonth = (date: Date) => {
    const year = date.getFullYear();
    const month = date.getMonth();
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const daysInMonth = lastDay.getDate();
    const startingDayOfWeek = firstDay.getDay();

    return { daysInMonth, startingDayOfWeek };
  };

  const handlePreviousMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1));
  };

  const handleDateSelect = (day: number) => {
    const newDate = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day);
    if (newDate >= new Date(new Date().setHours(0, 0, 0, 0))) {
      setSelectedDate(newDate);
    }
  };

  const handleTimeSelect = (time: string) => {
    setSelectedTime(time);
    const [hours, minutes] = time.split(':');
    const datetime = new Date(selectedDate);
    datetime.setHours(parseInt(hours), parseInt(minutes), 0, 0);
    onSelectSlot(datetime.toISOString());
  };

  const { daysInMonth, startingDayOfWeek } = getDaysInMonth(currentMonth);
  const monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
                      'July', 'August', 'September', 'October', 'November', 'December'];
  const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  const isToday = (day: number) => {
    const today = new Date();
    return day === today.getDate() &&
           currentMonth.getMonth() === today.getMonth() &&
           currentMonth.getFullYear() === today.getFullYear();
  };

  const isSelected = (day: number) => {
    return day === selectedDate.getDate() &&
           currentMonth.getMonth() === selectedDate.getMonth() &&
           currentMonth.getFullYear() === selectedDate.getFullYear();
  };

  const isPastDate = (day: number) => {
    const date = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date < today;
  };

  return (
    <div className="booking-calendar">
      <div className="calendar-section">
        <div className="calendar-header">
          <button
            onClick={handlePreviousMonth}
            className="calendar-nav-btn"
            aria-label="Previous month"
          >
            ‹
          </button>
          <h3 className="calendar-month">
            {monthNames[currentMonth.getMonth()]} {currentMonth.getFullYear()}
          </h3>
          <button
            onClick={handleNextMonth}
            className="calendar-nav-btn"
            aria-label="Next month"
          >
            ›
          </button>
        </div>

        <div className="calendar-grid">
          {dayNames.map(day => (
            <div key={day} className="calendar-day-name">
              {day}
            </div>
          ))}

          {Array.from({ length: startingDayOfWeek }).map((_, index) => (
            <div key={`empty-${index}`} className="calendar-day empty" />
          ))}

          {Array.from({ length: daysInMonth }).map((_, index) => {
            const day = index + 1;
            const past = isPastDate(day);
            const today = isToday(day);
            const selected = isSelected(day);

            return (
              <button
                key={day}
                onClick={() => handleDateSelect(day)}
                disabled={past}
                className={`calendar-day ${past ? 'past' : ''} ${today ? 'today' : ''} ${selected ? 'selected' : ''}`}
              >
                {day}
              </button>
            );
          })}
        </div>
      </div>

      <div className="time-slots-section">
        <div className="selected-date-header">
          <h3>
            {selectedDate.toLocaleDateString('en-US', {
              weekday: 'long',
              month: 'long',
              day: 'numeric'
            })}
          </h3>
          <p className="duration-text">
            {sessionDurationMinutes} minutes • {getTimezoneAbbreviation(getUserTimezone())}
          </p>
        </div>

        <div className="time-slots-grid">
          {availableSlots.length > 0 ? (
            availableSlots.map((slot) => (
              <button
                key={slot.time}
                onClick={() => handleTimeSelect(slot.time)}
                disabled={!slot.available}
                className={`time-slot ${!slot.available ? 'unavailable' : ''} ${selectedTime === slot.time ? 'selected' : ''}`}
              >
                {slot.time}
              </button>
            ))
          ) : (
            <p className="no-slots-text">No available time slots for this date</p>
          )}
        </div>
      </div>
    </div>
  );
}
