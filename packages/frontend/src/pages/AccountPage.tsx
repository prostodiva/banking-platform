
import { Tabs } from "../components/ui/Tabs";
import type { TabItem } from "../components/ui/types";
import { ACCOUNT_TABS, PANEL_CONTENT } from "../features/accounts/data/accountData";
import type { AccountTabId } from "../features/accounts/types/accountTabs";

const tabs: TabItem<AccountTabId>[] = ACCOUNT_TABS.map((tab) => ({
  ...tab,
  content: PANEL_CONTENT[tab.id],
}));


export default function AccountPage() {
  return (
    <main>
      <h1>Account</h1>
      <Tabs tabs={tabs} defaultTabId="overview" ariaLabel="Account sections"/>
    </main>
  );
}
