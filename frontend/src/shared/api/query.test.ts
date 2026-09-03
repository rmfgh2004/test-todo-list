import { describe, expect, it } from 'vitest';
import { parseTaskListQuery, serializeTaskListQuery } from './query';

describe('task list query allowlist', () => {
  it('F_N05_accepts_only_contract_keys_and_values', () => {
    const input = new URLSearchParams({
      status: 'TODO',
      scheduled: 'true',
      priority: 'HIGH',
      sort: 'TITLE',
      direction: 'ASC',
      page: '2',
      size: '100',
      injected: '<script>',
    });

    expect(parseTaskListQuery(input)).toEqual({
      status: 'TODO',
      scheduled: true,
      priority: 'HIGH',
      sort: 'TITLE',
      direction: 'ASC',
      page: 2,
      size: 100,
    });
  });

  it('F_N05_falls_back_for_out_of_range_values_instead_of_forwarding_them', () => {
    const query = parseTaskListQuery(
      new URLSearchParams({
        status: 'DELETED',
        scheduled: 'sometimes',
        priority: 'URGENT',
        sort: 'DROP TABLE',
        direction: 'SIDEWAYS',
        page: '-1',
        size: '1000',
      }),
    );

    expect(query).toEqual({ sort: 'CREATED_AT', direction: 'DESC', page: 0, size: 25 });
    expect(serializeTaskListQuery(query)).toBe('sort=CREATED_AT&direction=DESC&page=0&size=25');
  });
});
