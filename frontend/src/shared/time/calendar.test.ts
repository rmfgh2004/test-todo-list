import fc from 'fast-check';
import { describe, expect, it } from 'vitest';
import {
  addDays,
  formatIsoDate,
  formatIsoTime,
  parseIsoDate,
  parseIsoTime,
  weekRangeFor,
} from './calendar';
import { assertProperty, isoDateArbitrary } from '../../../tests/generators/planning';

describe('calendar core', () => {
  it('PBT_02_parse_and_format_preserve_every_valid_ISO_date', () => {
    assertProperty(
      fc.property(isoDateArbitrary, (value) => {
        expect(formatIsoDate(parseIsoDate(value))).toBe(value);
      }),
    );
  });

  it('PBT_02_parse_and_format_preserve_every_valid_HH_mm_time', () => {
    assertProperty(
      fc.property(
        fc.integer({ min: 0, max: 23 }),
        fc.integer({ min: 0, max: 59 }),
        (hour, minute) => {
          const value = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
          expect(formatIsoTime(parseIsoTime(value))).toBe(value);
        },
      ),
    );
  });

  it('UR_014_normalizes_a_date_to_a_Monday_based_seven_day_range', () => {
    const range = weekRangeFor('2026-09-03');

    expect(range).toEqual({
      start: '2026-08-31',
      endExclusive: '2026-09-07',
      days: [
        '2026-08-31',
        '2026-09-01',
        '2026-09-02',
        '2026-09-03',
        '2026-09-04',
        '2026-09-05',
        '2026-09-06',
      ],
    });
  });

  it('PBT_03_week_ranges_are_contiguous_and_exactly_seven_days', () => {
    assertProperty(
      fc.property(isoDateArbitrary, (value) => {
        const range = weekRangeFor(value);
        expect(range.days).toHaveLength(7);
        expect(range.days[0]).toBe(range.start);
        expect(addDays(range.start, 7)).toBe(range.endExclusive);
        expect(new Set(range.days)).toHaveLength(7);
      }),
    );
  });

  it.each(['2026-02-29', '2026-13-01', '2026-01-00', 'not-a-date'])(
    'PBT_02_rejects_invalid_ISO_date_%s',
    (value) => {
      expect(() => parseIsoDate(value)).toThrow('Invalid ISO date');
    },
  );
});
