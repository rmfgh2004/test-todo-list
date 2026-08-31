export interface CalendarDate {
  readonly year: number;
  readonly month: number;
  readonly day: number;
}

export interface WallTime {
  readonly hour: number;
  readonly minute: number;
}

export interface WeekRange {
  readonly start: string;
  readonly endExclusive: string;
  readonly days: readonly string[];
}

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;
const ISO_TIME = /^(\d{2}):(\d{2})$/;

const isLeapYear = (year: number): boolean =>
  year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);

const daysInMonth = (year: number, month: number): number => {
  if (month === 2) return isLeapYear(year) ? 29 : 28;
  return [4, 6, 9, 11].includes(month) ? 30 : 31;
};

const pad = (value: number, width = 2): string => String(value).padStart(width, '0');

/** NFR-002, PBT-02: parses a strict ISO date without host-timezone conversion. */
export const parseIsoDate = (value: string): CalendarDate => {
  const match = ISO_DATE.exec(value);
  if (match === null) throw new Error('Invalid ISO date');

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (year < 1 || month < 1 || month > 12 || day < 1 || day > daysInMonth(year, month)) {
    throw new Error('Invalid ISO date');
  }
  return { year, month, day };
};

/** NFR-002, PBT-02: formats a validated calendar value at the transport boundary. */
export const formatIsoDate = ({ year, month, day }: CalendarDate): string => {
  if (
    year < 1 ||
    year > 9_999 ||
    month < 1 ||
    month > 12 ||
    day < 1 ||
    day > daysInMonth(year, month)
  ) {
    throw new Error('Invalid calendar date');
  }
  return `${pad(year, 4)}-${pad(month)}-${pad(day)}`;
};

/** NFR-002, PBT-02: parses strict Asia/Seoul wall-clock HH:mm text. */
export const parseIsoTime = (value: string): WallTime => {
  const match = ISO_TIME.exec(value);
  if (match === null) throw new Error('Invalid ISO time');
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) throw new Error('Invalid ISO time');
  return { hour, minute };
};

/** NFR-002, PBT-02: formats wall-clock time without a Date or UTC conversion. */
export const formatIsoTime = ({ hour, minute }: WallTime): string => {
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) throw new Error('Invalid wall time');
  return `${pad(hour)}:${pad(minute)}`;
};

const daysFromCivil = ({ year, month, day }: CalendarDate): number => {
  const adjustedYear = year - (month <= 2 ? 1 : 0);
  const era = Math.floor(adjustedYear / 400);
  const yearOfEra = adjustedYear - era * 400;
  const shiftedMonth = month + (month > 2 ? -3 : 9);
  const dayOfYear = Math.floor((153 * shiftedMonth + 2) / 5) + day - 1;
  const dayOfEra =
    yearOfEra * 365 + Math.floor(yearOfEra / 4) - Math.floor(yearOfEra / 100) + dayOfYear;
  return era * 146_097 + dayOfEra - 719_468;
};

const civilFromDays = (epochDay: number): CalendarDate => {
  const shiftedDay = epochDay + 719_468;
  const era = Math.floor(shiftedDay / 146_097);
  const dayOfEra = shiftedDay - era * 146_097;
  const yearOfEra = Math.floor(
    (dayOfEra -
      Math.floor(dayOfEra / 1_460) +
      Math.floor(dayOfEra / 36_524) -
      Math.floor(dayOfEra / 146_096)) /
      365,
  );
  let year = yearOfEra + era * 400;
  const dayOfYear =
    dayOfEra - (365 * yearOfEra + Math.floor(yearOfEra / 4) - Math.floor(yearOfEra / 100));
  const shiftedMonth = Math.floor((5 * dayOfYear + 2) / 153);
  const day = dayOfYear - Math.floor((153 * shiftedMonth + 2) / 5) + 1;
  const month = shiftedMonth + (shiftedMonth < 10 ? 3 : -9);
  year += month <= 2 ? 1 : 0;
  return { year, month, day };
};

/** FR-002, UR-014: adds calendar days without depending on the host timezone. */
export const addDays = (value: string, amount: number): string => {
  if (!Number.isInteger(amount)) throw new Error('Day amount must be an integer');
  return formatIsoDate(civilFromDays(daysFromCivil(parseIsoDate(value)) + amount));
};

/** FR-001, FR-002, UR-014: returns the containing Monday-based seven-day range. */
export const weekRangeFor = (value: string): WeekRange => {
  const epochDay = daysFromCivil(parseIsoDate(value));
  const mondayIndex = (((epochDay + 3) % 7) + 7) % 7;
  const startDate = civilFromDays(epochDay - mondayIndex);
  const start = formatIsoDate(startDate);
  const days = Array.from({ length: 7 }, (_, index) => addDays(start, index));
  return { start, endExclusive: addDays(start, 7), days };
};
