export type LayoutMode = { readonly mode: 'day' | 'week'; readonly visibleDays: 1 | 7 };

/** FR-011: the 320px path uses one day; desktop begins at the shared 768px breakpoint. */
export const layoutModeForWidth = (width: number): LayoutMode =>
  width < 768 ? { mode: 'day', visibleDays: 1 } : { mode: 'week', visibleDays: 7 };
