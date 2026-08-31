import fc from 'fast-check';
import type { Arbitrary, IRawProperty } from 'fast-check';
import type { components } from '@/shared/api/generated/planning-api';

type ScheduleView = components['schemas']['ScheduleView'];
type TaskView = components['schemas']['TaskView'];

export interface DropPosition {
  readonly date: string;
  readonly hour: number;
  readonly minute: number;
  readonly second: number;
}

export const PBT_SEED = 20260901;

const pad = (value: number): string => String(value).padStart(2, '0');

const formatDate = (year: number, month: number, day: number): string =>
  `${String(year).padStart(4, '0')}-${pad(month)}-${pad(day)}`;

const formatTime = (minuteOfDay: number): string =>
  `${pad(Math.floor(minuteOfDay / 60))}:${pad(minuteOfDay % 60)}`;

const isLeapYear = (year: number): boolean =>
  year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);

const daysInMonth = (year: number, month: number): number => {
  if (month === 2) return isLeapYear(year) ? 29 : 28;
  return [4, 6, 9, 11].includes(month) ? 30 : 31;
};

/** PBT-07: valid calendar dates, including leap-day and month boundaries. */
export const isoDateArbitrary: Arbitrary<string> = fc
  .tuple(fc.integer({ min: 1970, max: 2100 }), fc.integer({ min: 1, max: 12 }))
  .chain(([year, month]) =>
    fc
      .integer({ min: 1, max: daysInMonth(year, month) })
      .map((day) => formatDate(year, month, day)),
  );

/** PBT-07: valid 15-minute schedule values inside the 08:00~22:00 window. */
export const scheduleViewArbitrary: Arbitrary<ScheduleView> = fc
  .tuple(isoDateArbitrary, fc.integer({ min: 0, max: 55 }))
  .chain(([date, startSlot]) =>
    fc.integer({ min: 1, max: 56 - startSlot }).map((slotSpan) => ({
      date,
      startTime: formatTime(8 * 60 + startSlot * 15),
      endTime: formatTime(8 * 60 + (startSlot + slotSpan) * 15),
    })),
  );

/** PBT-07: realistic task payloads derived from generated contract types. */
export const taskViewArbitrary: Arbitrary<TaskView> = fc
  .record({
    id: fc.uuid(),
    title: fc.string({ minLength: 1, maxLength: 120 }).filter((title) => title.trim().length > 0),
    description: fc.option(fc.string({ maxLength: 2_000 }), { nil: undefined }),
    priority: fc.constantFrom('LOW' as const, 'MEDIUM' as const, 'HIGH' as const),
    estimateMinutes: fc.integer({ min: 1, max: 56 }).map((quarters) => quarters * 15),
    dueDate: fc.option(isoDateArbitrary, { nil: undefined }),
    status: fc.constantFrom('TODO' as const, 'COMPLETED' as const),
    schedule: fc.option(scheduleViewArbitrary, { nil: undefined }),
    version: fc.nat({ max: 1_000_000 }),
    createdAt: isoDateArbitrary.map((date) => `${date}T00:00:00+09:00`),
    updatedAt: isoDateArbitrary.map((date) => `${date}T23:59:59+09:00`),
  })
  .map(({ description, dueDate, schedule, ...required }) => ({
    ...required,
    ...(description === undefined ? {} : { description }),
    ...(dueDate === undefined ? {} : { dueDate }),
    ...(schedule === undefined ? {} : { schedule }),
  }));

/** PBT-07: pointer drops include seconds so midpoint rounding is exercised. */
export const dropPositionArbitrary: Arbitrary<DropPosition> = fc.record({
  date: isoDateArbitrary,
  hour: fc.integer({ min: 8, max: 21 }),
  minute: fc.integer({ min: 0, max: 59 }),
  second: fc.integer({ min: 0, max: 59 }),
});

/** PBT-08/PBT-09: fixed seed is printed and shrinking remains enabled. */
export const assertProperty = <Ts>(property: IRawProperty<Ts>): void => {
  console.info(`[fast-check] seed=${PBT_SEED}`);
  fc.assert(property, { seed: PBT_SEED, numRuns: 200, verbose: 2 });
};
