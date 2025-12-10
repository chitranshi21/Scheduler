/**
 * Timezone utilities for handling business hours and bookings across timezones
 */

/**
 * Get the user's current timezone
 */
export const getUserTimezone = (): string => {
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
};

/**
 * Format a date/time with timezone display
 */
export const formatDateTimeWithTimezone = (
  timestamp: number,
  options?: Intl.DateTimeFormatOptions
): string => {
  const defaultOptions: Intl.DateTimeFormatOptions = {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZoneName: 'short',
    ...options,
  };

  return new Date(timestamp).toLocaleString('en-US', defaultOptions);
};

/**
 * Get timezone abbreviation (e.g., "EST", "CET")
 */
export const getTimezoneAbbreviation = (timezone?: string): string => {
  if (!timezone) {
    timezone = getUserTimezone();
  }

  const date = new Date();
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    timeZoneName: 'short',
  });

  const parts = formatter.formatToParts(date);
  const timeZonePart = parts.find((part) => part.type === 'timeZoneName');

  return timeZonePart?.value || timezone;
};

/**
 * Convert a time string (HH:mm) in business timezone to a display string with offset info
 * Note: This is for display purposes only. Actual booking times are stored as UTC timestamps.
 */
export const formatBusinessHoursWithTimezone = (
  time: string,
  businessTimezone: string
): string => {
  const userTimezone = getUserTimezone();

  if (businessTimezone === userTimezone) {
    return `${time} (${getTimezoneAbbreviation(userTimezone)})`;
  }

  // Show both business time and user's local time
  const businessTz = getTimezoneAbbreviation(businessTimezone);
  return `${time} ${businessTz}`;
};

/**
 * Check if user is in a different timezone than the business
 */
export const isDifferentTimezone = (businessTimezone: string): boolean => {
  const userTimezone = getUserTimezone();
  return businessTimezone !== userTimezone;
};

/**
 * Get a friendly message about timezone difference
 */
export const getTimezoneDifferenceMessage = (businessTimezone: string): string | null => {
  if (!isDifferentTimezone(businessTimezone)) {
    return null;
  }

  const userTz = getTimezoneAbbreviation(getUserTimezone());
  const businessTz = getTimezoneAbbreviation(businessTimezone);

  return `Times shown in your local timezone (${userTz}). Business operates in ${businessTz}.`;
};
