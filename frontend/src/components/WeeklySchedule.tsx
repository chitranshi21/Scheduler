import { useState } from 'react';
import '../styles/WeeklySchedule.css';

interface DaySchedule {
  enabled: boolean;
  startTime: string;
  endTime: string;
}

interface WeeklyScheduleProps {
  onSave: (schedule: Record<string, DaySchedule>) => void;
}

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

const DEFAULT_SCHEDULE: DaySchedule = {
  enabled: true,
  startTime: '09:00',
  endTime: '17:00'
};

export default function WeeklySchedule({ onSave }: WeeklyScheduleProps) {
  console.log('WeeklySchedule component rendering...');
  const [schedule, setSchedule] = useState<Record<string, DaySchedule>>({
    Monday: { ...DEFAULT_SCHEDULE },
    Tuesday: { ...DEFAULT_SCHEDULE },
    Wednesday: { ...DEFAULT_SCHEDULE },
    Thursday: { ...DEFAULT_SCHEDULE },
    Friday: { ...DEFAULT_SCHEDULE },
    Saturday: { enabled: false, startTime: '09:00', endTime: '17:00' },
    Sunday: { enabled: false, startTime: '09:00', endTime: '17:00' }
  });

  const handleToggleDay = (day: string) => {
    setSchedule({
      ...schedule,
      [day]: { ...schedule[day], enabled: !schedule[day].enabled }
    });
  };

  const handleTimeChange = (day: string, field: 'startTime' | 'endTime', value: string) => {
    setSchedule({
      ...schedule,
      [day]: { ...schedule[day], [field]: value }
    });
  };

  const handleCopyToAll = (day: string) => {
    const sourceDaySchedule = schedule[day];
    const newSchedule: Record<string, DaySchedule> = {};

    DAYS.forEach(d => {
      newSchedule[d] = {
        ...sourceDaySchedule,
        enabled: schedule[d].enabled // Keep the enabled state of each day
      };
    });

    setSchedule(newSchedule);
  };

  const handleSave = () => {
    onSave(schedule);
  };

  return (
    <div className="weekly-schedule">
      <div className="schedule-header">
        <h3>Set Your Weekly Hours</h3>
        <p>Configure when you're available to accept bookings</p>
      </div>

      <div className="schedule-list">
        {DAYS.map((day) => (
          <div key={day} className={`schedule-day ${!schedule[day].enabled ? 'disabled' : ''}`}>
            <div className="day-toggle">
              <label className="toggle-switch">
                <input
                  type="checkbox"
                  checked={schedule[day].enabled}
                  onChange={() => handleToggleDay(day)}
                />
                <span className="toggle-slider"></span>
              </label>
              <span className="day-name">{day}</span>
            </div>

            {schedule[day].enabled ? (
              <div className="time-inputs">
                <input
                  type="time"
                  value={schedule[day].startTime}
                  onChange={(e) => handleTimeChange(day, 'startTime', e.target.value)}
                  className="time-input"
                />
                <span className="time-separator">to</span>
                <input
                  type="time"
                  value={schedule[day].endTime}
                  onChange={(e) => handleTimeChange(day, 'endTime', e.target.value)}
                  className="time-input"
                />
                <button
                  onClick={() => handleCopyToAll(day)}
                  className="copy-button"
                  title="Copy these hours to all days"
                >
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path d="M4 2h8a2 2 0 012 2v8M2 6h8a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V8a2 2 0 012-2z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                </button>
              </div>
            ) : (
              <span className="unavailable-text">Unavailable</span>
            )}
          </div>
        ))}
      </div>

      <div className="schedule-footer">
        <button onClick={handleSave} className="button button-primary">
          Save Schedule
        </button>
      </div>
    </div>
  );
}
