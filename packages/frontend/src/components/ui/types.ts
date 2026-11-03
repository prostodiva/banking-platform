import type { ReactNode } from "react";

export interface TabItem<Id extends string> {
  id: Id;
  label: string;
  content: ReactNode;
}

export interface TabsProps<Id extends string> {
  tabs: TabItem<Id>[];
  defaultTabId?: Id;
  /** Accessible name for the tablist — required: a tablist without one is an ARIA violation. */
  ariaLabel: string;
}
