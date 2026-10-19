import { useId, useState } from 'react';
import { BookOpen, Calendar, MapPin, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent } from '@/components/ui/card';
import type { DayOfWeek, TimetableSlotResponseDto } from '@/types/timetable';

const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

function slotTime(slot: TimetableSlotResponseDto) {
  return [slot.periodStartTime, slot.periodEndTime]
    .filter((time): time is string => time !== null)
    .map((time) => time.length > 5 && time.endsWith(':00') ? time.slice(0, -3) : time)
    .join(' – ');
}

function SlotCard({ slot }: { slot: TimetableSlotResponseDto }) {
  const { t } = useTranslation();
  const teacherName = [slot.teacherFirstName, slot.teacherLastName].filter(Boolean).join(' ');
  const time = slotTime(slot);

  return (
    <li data-testid="timetable-slot" data-slot-id={slot.id}
      className="min-w-0 space-y-2 rounded-lg border border-border bg-muted/30 p-4 text-sm [overflow-wrap:anywhere] lg:p-2 lg:text-xs">
      {slot.periodIndex !== null && (
        <p className="font-medium">{t('studentTimetable.period', { index: slot.periodIndex })}</p>
      )}
      {time && <p className="text-muted-foreground">{time}</p>}
      {slot.forCourseName && <p className="font-semibold text-foreground">{slot.forCourseName}</p>}
      {slot.forClassName && (
        <p className="flex items-start gap-2">
          <BookOpen className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          <span className="min-w-0">{slot.forClassName}</span>
        </p>
      )}
      {teacherName && (
        <p className="flex items-start gap-2">
          <User className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          <span className="min-w-0">{teacherName}</span>
        </p>
      )}
      {slot.roomName && (
        <p className="flex items-start gap-2">
          <MapPin className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          <span className="min-w-0">{slot.roomName}</span>
        </p>
      )}
      {slot.description && <p className="text-muted-foreground">{slot.description}</p>}
    </li>
  );
}

export function StudentTimetableView({ slots }: { slots: TimetableSlotResponseDto[] }) {
  const { t } = useTranslation();
  const [selectedDay, setSelectedDay] = useState<DayOfWeek>('MONDAY');
  const daySelectorId = useId();
  const dayHeadingId = useId();

  if (slots.length === 0) {
    return (
      <Card>
        <CardContent className="p-8 text-center text-muted-foreground">
          <Calendar className="mx-auto mb-3 size-8" aria-hidden="true" />
          <p>{t('studentTimetable.empty')}</p>
        </CardContent>
      </Card>
    );
  }

  const orderedSlots = [...slots].sort((a, b) =>
    (a.periodStartTime ?? '99').localeCompare(b.periodStartTime ?? '99')
    || (a.periodEndTime ?? '99').localeCompare(b.periodEndTime ?? '99')
    || (a.periodIndex ?? Number.MAX_SAFE_INTEGER) - (b.periodIndex ?? Number.MAX_SAFE_INTEGER)
    || a.id - b.id,
  );
  const timeRows = new Map<string, TimetableSlotResponseDto[]>();
  for (const slot of orderedSlots) {
    const key = JSON.stringify([slot.periodStartTime, slot.periodEndTime]);
    const row = timeRows.get(key);
    if (row) row.push(slot);
    else timeRows.set(key, [slot]);
  }
  const daySlots = orderedSlots.filter((slot) => slot.dayOfWeek === selectedDay);

  return (
    <div className="min-w-0 [overflow-wrap:anywhere]">
      <div className="hidden rounded-lg border border-border bg-card lg:block">
        <table aria-label={t('studentTimetable.weekly')} className="w-full table-fixed border-collapse text-sm">
          <thead className="bg-muted/50">
            <tr className="border-b border-border">
              <th scope="col" className="w-28 p-3 text-start font-semibold">{t('studentTimetable.time')}</th>
              {DAYS.map((day) => (
                <th scope="col" key={day} className="border-s border-border p-2 text-start font-semibold">
                  {t(`studentTimetable.days.${day}`)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from(timeRows, ([key, rowSlots]) => (
              <tr key={key} className="border-b border-border last:border-b-0">
                <th scope="row" className="p-3 text-start align-top text-xs font-medium text-muted-foreground">
                  {slotTime(rowSlots[0])}
                </th>
                {DAYS.map((day) => {
                  const cellSlots = rowSlots.filter((slot) => slot.dayOfWeek === day);
                  return (
                    <td key={day} className="border-s border-border p-1.5 align-top">
                      {cellSlots.length > 0 && (
                        <ul className="space-y-2">
                          {cellSlots.map((slot) => <SlotCard key={slot.id} slot={slot} />)}
                        </ul>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="min-w-0 space-y-4 lg:hidden">
        <div className="space-y-2">
          <label htmlFor={daySelectorId} className="block text-sm font-medium">{t('studentTimetable.day')}</label>
          <select id={daySelectorId} value={selectedDay}
            onChange={(event) => setSelectedDay(event.target.value as DayOfWeek)}
            className="h-11 w-full min-w-0 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
            {DAYS.map((day) => <option key={day} value={day}>{t(`studentTimetable.days.${day}`)}</option>)}
          </select>
        </div>
        <section aria-labelledby={dayHeadingId} className="min-w-0 space-y-3">
          <h2 id={dayHeadingId} className="text-lg font-semibold">{t(`studentTimetable.days.${selectedDay}`)}</h2>
          {daySlots.length > 0 ? (
            <ul className="space-y-3">{daySlots.map((slot) => <SlotCard key={slot.id} slot={slot} />)}</ul>
          ) : (
            <p className="rounded-lg border border-border p-6 text-sm text-muted-foreground">{t('studentTimetable.emptyDay')}</p>
          )}
        </section>
      </div>
    </div>
  );
}
