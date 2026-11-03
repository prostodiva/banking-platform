import { useState } from "react";
import { Button } from "./Button";
import type { TabsProps } from "./types";
import { TabPanel } from "./TabPanel";


export function Tabs<Id extends string>({ tabs, defaultTabId, ariaLabel }: TabsProps<Id>) {
  const [activeId, setActiveId] = useState<Id | undefined>(
    defaultTabId ?? tabs[0]?.id
  );

  const activeTab = tabs.find((t) => t.id === activeId);

  return (
    <div>
      <div role="tablist" aria-label={ariaLabel}>
        {tabs.map((tab) => (
          <Button
            key={tab.id}
            id={`tab-${tab.id}`}
            role="tab"
            aria-selected={tab.id === activeId}
            aria-controls={`panel-${tab.id}`}
            onClick={() => setActiveId(tab.id)}
          >
            {tab.label}
          </Button>
        ))}
      </div>
      <section
        role="tabpanel"
        id={`panel-${activeTab?.id}`}
        aria-labelledby={`tab-${activeTab?.id}`}
      >
         {activeTab && <TabPanel tabId={activeTab.id}>{activeTab.content}</TabPanel>}
      </section>
    </div>
  );
}
