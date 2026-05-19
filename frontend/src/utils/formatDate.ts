export default function formatDate(date: string) {
  if (!date || isNaN(new Date(date).getTime())) return '';

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(new Date(date));
}

export function formatDateTime(date: string) {
  if (!date || isNaN(new Date(date).getTime())) return '';

  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(date));
}

export function formatDateWithoutYear(date: string) {
  if (!date || isNaN(new Date(date).getTime())) return '';

  const formattedDate = new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  }).format(new Date(date));

  return formattedDate.substring(0, formattedDate.lastIndexOf('/'));
}

export function formatTimeWithoutSecond(time: string) {
  if (!time) return '';
  const parts = time.split(':');
  if (parts.length >= 2) {
    return `${parts[0]}:${parts[1]}`;
  }
  return time;
}
