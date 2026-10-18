import { BookOpen, Calendar, MapPin, User } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import type { DayOfWeek, TimetableSlotResponseDto } from '@/types/timetable';

const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export function StudentTimetableView({ slots }: { slots: TimetableSlotResponseDto[] }) {
  const { t } = useTranslation();

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

  return (
    <div className="grid min-w-0 gap-4 md:grid-cols-2 xl:grid-cols-3">
      {DAYS.map((day) => {
        const daySlots = slots.filter((slot) => slot.dayOfWeek === day).sort((a, b) =>
          (a.periodIndex ?? Number.MAX_SAFE_INTEGER) - (b.periodIndex ?? Number.MAX_SAFE_INTEGER)
          || (a.periodStartTime ?? '').localeCompare(b.periodStartTime ?? '')
          || a.id - b.id,
        );
        if (daySlots.length === 0) return null;

        return (
          <section key={day} aria-labelledby={`timetable-${day}`} className="min-w-0">
            <Card className="h-full">
              <CardHeader>
                <h2 id={`timetable-${day}`} className="text-lg font-semibold">
                  {t(`studentTimetable.days.${day}`)}
                </h2>
              </CardHeader>
              <CardContent>
                <ul className="space-y-4">
                  {daySlots.map((slot) => {
                    const teacherName = [slot.teacherFirstName, slot.teacherLastName].filter(Boolean).join(' ');
                    const times = [slot.periodStartTime, slot.periodEndTime]
                      .filter((time): time is string => time !== null)
                      .map((time) => time.length > 5 && time.endsWith(':00') ? time.slice(0, -3) : time);

                    return (
                      <li key={slot.id} className="space-y-2 rounded-lg border border-border bg-muted/30 p-4 text-sm [overflow-wrap:anywhere]">
                        {slot.periodIndex !== null && (
                          <p className="font-medium">{t('studentTimetable.period', { index: slot.periodIndex })}</p>
                        )}
                        {times.length > 0 && <p className="text-muted-foreground">{times.join(' – ')}</p>}
                        {slot.forCourseName && <p className="font-semibold text-foreground">{slot.forCourseName}</p>}
                        {slot.forClassName && (
                          <p className="flex items-start gap-2">
                            <BookOpen className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                            <span>{slot.forClassName}</span>
                          </p>
                        )}
                        {teacherName && (
                          <p className="flex items-start gap-2">
                            <User className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                            <span>{teacherName}</span>
                          </p>
                        )}
                        {slot.roomName && (
                          <p className="flex items-start gap-2">
                            <MapPin className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                            <span>{slot.roomName}</span>
                          </p>
                        )}
                        {slot.description && <p className="text-muted-foreground">{slot.description}</p>}
                      </li>
                    );
                  })}
                </ul>
              </CardContent>
            </Card>
          </section>
        );
      })}
    </div>
  );
}
