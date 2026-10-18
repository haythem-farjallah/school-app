import { useTranslation } from 'react-i18next';
import { RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { StudentTimetableView } from '@/features/timetable/components/StudentTimetableView';
import { useMyTimetable } from '@/features/timetable/hooks';

export default function MyTimetable() {
  const { t } = useTranslation();
  const { data, isPending, isError, refetch, isFetching } = useMyTimetable();

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-foreground">{t('navigation.myTimetable')}</h1>
      {isPending ? (
        <Card>
          <CardContent className="flex items-center justify-center gap-2 p-8" role="status">
            <RefreshCw className="size-5 animate-spin" aria-hidden="true" />
            <span>{t('studentTimetable.loading')}</span>
          </CardContent>
        </Card>
      ) : isError ? (
        <Card>
          <CardContent className="space-y-3 p-8" role="alert">
            <p className="font-medium text-destructive">{t('studentTimetable.error')}</p>
            <Button variant="outline" onClick={() => void refetch()} disabled={isFetching}>
              {t('studentTimetable.retry')}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <StudentTimetableView slots={data.slots} />
      )}
    </div>
  );
}
