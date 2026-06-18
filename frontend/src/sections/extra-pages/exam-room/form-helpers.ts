import * as Yup from 'yup';
import { ALL_PERIODS } from 'utils/schedule';

export interface ExamRoomFormValues {
  code: string;
  roomId: number;
  teacher1Id: number;
  teacher2Id: number;
  subjectId: number;
  semesterId: number;
  maxStudent: number | '';
  examDate: string;
  periods: string;
}

export const parsePeriodValues = (periods?: string) =>
  (periods ?? '')
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isInteger(value) && value >= 1 && value <= 12)
    .sort((a, b) => a - b);

export const normalizePeriods = (periods: string) => [...new Set(parsePeriodValues(periods))].join(',');

export const isContinuousPeriods = (periods: string) => {
  const values = parsePeriodValues(periods);
  if (values.length === 0) return false;
  return values.every((value, index) => index === 0 || value === values[index - 1] + 1);
};

const normalizeTimeForInput = (time?: string) => (time ? time.slice(0, 5) : '');

export const derivePeriodsFromTime = (startTime?: string, endTime?: string) => {
  const start = normalizeTimeForInput(startTime);
  const end = normalizeTimeForInput(endTime);
  const startIndex = ALL_PERIODS.findIndex((period) => period.label.includes(start));
  const endIndex = ALL_PERIODS.findIndex((period) => period.label.includes(end));

  if (startIndex < 0 || endIndex < startIndex) return '';

  return ALL_PERIODS.slice(startIndex, endIndex + 1)
    .map((period) => period.value)
    .join(',');
};

export const examRoomValidationSchema = Yup.object({
  code: Yup.string().required('Mã phòng thi không được để trống'),
  roomId: Yup.number().min(1, 'Vui lòng chọn phòng').required('Vui lòng chọn phòng'),
  teacher1Id: Yup.number().min(1, 'Vui lòng chọn giảng viên 1').required('Vui lòng chọn giảng viên 1'),
  teacher2Id: Yup.number().min(1, 'Vui lòng chọn giảng viên 2').required('Vui lòng chọn giảng viên 2'),
  subjectId: Yup.number().min(1, 'Vui lòng chọn môn học').required('Vui lòng chọn môn học'),
  semesterId: Yup.number().min(1, 'Vui lòng chọn học kỳ').required('Vui lòng chọn học kỳ'),
  maxStudent: Yup.number().min(1, 'Sĩ số phải lớn hơn 0').required('Vui lòng nhập sĩ số tối đa'),
  examDate: Yup.string().required('Vui lòng chọn ngày thi'),
  periods: Yup.string()
    .required('Vui lòng chọn tiết thi')
    .test('continuous-periods', 'Các tiết thi phải liên tục', (value) => isContinuousPeriods(value ?? ''))
});

export const emptyExamRoomValues: ExamRoomFormValues = {
  code: '',
  roomId: 0,
  teacher1Id: 0,
  teacher2Id: 0,
  subjectId: 0,
  semesterId: 0,
  maxStudent: '',
  examDate: '',
  periods: ''
};
