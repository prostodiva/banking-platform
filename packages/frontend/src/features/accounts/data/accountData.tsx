import type { ReactNode } from "react";
import type { AccountTab, AccountTabId } from "../types/accountTabs";
import {  OverviewPanel } from "../components/OverviewPanel";
import { TransactionsPanel } from "../components/TransactionsPanel";
import { DetailsPanel } from "../components/DetailsPanel";


export const ACCOUNT_TABS: AccountTab[] = [
  { id: "overview", label: "Overview" },
  { id: "transactions", label: "Transactions" },
  { id: "details", label: "Details" },
];

export const PANEL_CONTENT: Record<AccountTabId, ReactNode> = {
  overview: < OverviewPanel />,
  transactions: <TransactionsPanel />,
  details: <DetailsPanel />,
};
