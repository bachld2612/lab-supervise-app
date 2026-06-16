const PERIOD_BOUNDARY_TIMES = [
  '07:00',
  '07:50',
  '07:55',
  '08:45',
  '08:50',
  '09:40',
  '09:45',
  '10:35',
  '10:40',
  '11:30',
  '11:35',
  '12:25',
  '12:55',
  '13:45',
  '13:50',
  '14:40',
  '14:45',
  '15:35',
  '15:40',
  '16:30',
  '16:35',
  '17:25',
  '17:30',
  '18:20'
];

export const getNextPeriodRefreshDelay = (now = new Date(), bufferMs = 3000) => {
  const nextBoundary = PERIOD_BOUNDARY_TIMES.map((time) => {
    const [hours, minutes] = time.split(':').map(Number);
    const boundary = new Date(now);
    boundary.setHours(hours, minutes, 0, 0);
    return boundary;
  }).find((boundary) => boundary.getTime() > now.getTime());

  const target = nextBoundary ?? (() => {
    const [hours, minutes] = PERIOD_BOUNDARY_TIMES[0].split(':').map(Number);
    const boundary = new Date(now);
    boundary.setDate(boundary.getDate() + 1);
    boundary.setHours(hours, minutes, 0, 0);
    return boundary;
  })();

  return Math.max(1000, target.getTime() - now.getTime() + bufferMs);
};
