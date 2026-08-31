import fc from 'fast-check';
import { describe, expect, it } from 'vitest';
import {
  DAY_END_MINUTES,
  DAY_START_MINUTES,
  SLOTS_PER_DAY,
  estimateToSlotSpan,
  isQuarterHourAligned,
  snapDropToQuarter,
  timeToSlotIndex,
  toGridPosition,
} from './geometry';
import {
  assertProperty,
  dropPositionArbitrary,
  scheduleViewArbitrary,
} from '../../../tests/generators/planning';
import { parseIsoTime, weekRangeFor } from '../time/calendar';

describe('planning grid geometry', () => {
  it('UR_010_defines_exactly_56_slots_between_08_00_and_22_00', () => {
    expect(DAY_START_MINUTES).toBe(8 * 60);
    expect(DAY_END_MINUTES).toBe(22 * 60);
    expect(SLOTS_PER_DAY).toBe(56);
    expect(timeToSlotIndex('08:00')).toBe(0);
    expect(timeToSlotIndex('21:45')).toBe(55);
  });

  it('PBT_03_maps_aligned_schedule_values_inside_the_grid', () => {
    assertProperty(
      fc.property(scheduleViewArbitrary, (schedule) => {
        const position = toGridPosition(schedule, weekRangeFor(schedule.date).start);
        const durationMinutes =
          Number(schedule.endTime.slice(0, 2)) * 60 +
          Number(schedule.endTime.slice(3, 5)) -
          (Number(schedule.startTime.slice(0, 2)) * 60 + Number(schedule.startTime.slice(3, 5)));

        expect(position.startSlotIndex).toBeGreaterThanOrEqual(0);
        expect(position.dayIndex).toBeGreaterThanOrEqual(0);
        expect(position.dayIndex).toBeLessThanOrEqual(6);
        expect(position.startSlotIndex + position.slotSpan).toBeLessThanOrEqual(SLOTS_PER_DAY);
        expect(position.slotSpan).toBe(estimateToSlotSpan(durationMinutes));
      }),
    );
  });

  it('UR_011_derives_slot_span_from_estimate_minutes', () => {
    expect(estimateToSlotSpan(15)).toBe(1);
    expect(estimateToSlotSpan(840)).toBe(56);
    expect(() => estimateToSlotSpan(20)).toThrow('15-minute increment');
  });

  it('UR_012_snaps_to_the_nearest_quarter_and_rounds_an_exact_midpoint_up', () => {
    expect(snapDropToQuarter({ hour: 8, minute: 7, second: 29 })).toBe('08:00');
    expect(snapDropToQuarter({ hour: 8, minute: 7, second: 30 })).toBe('08:15');
    expect(snapDropToQuarter({ hour: 8, minute: 22, second: 30 })).toBe('08:30');
  });

  it.each([
    { hour: 7, minute: 59, second: 59 },
    { hour: 22, minute: 0, second: 0 },
  ])('SECURITY_05_rejects_drop_time_outside_the_grid_%j', (value) => {
    expect(() => snapDropToQuarter(value)).toThrow('inside 08:00~22:00');
  });

  it('PBT_03_every_drop_snaps_to_an_aligned_boundary_inside_the_closed_window', () => {
    assertProperty(
      fc.property(dropPositionArbitrary, ({ hour, minute, second }) => {
        const snapped = snapDropToQuarter({ hour, minute, second });
        expect(isQuarterHourAligned(snapped)).toBe(true);
        const snappedTime = parseIsoTime(snapped);
        const total = snappedTime.hour * 60 + snappedTime.minute;
        expect(total).toBeGreaterThanOrEqual(DAY_START_MINUTES);
        expect(total).toBeLessThanOrEqual(DAY_END_MINUTES);
      }),
    );
  });

  it.each(['07:45', '08:01', '22:00'])(
    'UR_010_rejects_a_start_outside_the_usable_grid_%s',
    (value) => {
      expect(() => timeToSlotIndex(value)).toThrow();
    },
  );
});
