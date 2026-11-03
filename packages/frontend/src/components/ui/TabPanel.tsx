import type { ReactNode } from "react";

interface TabPanelProps {
  tabId: string;
  children: ReactNode;
}

export function TabPanel({ tabId, children }: TabPanelProps) {
  return (
    <section
      role="tabpanel"
      id={`panel-${tabId}`}
      aria-labelledby={`tab-${tabId}`}
      tabIndex={0}
    >
      {children}
    </section>
  );
}
