
export type AccountTabId = "overview" | "transactions" | "details";

export interface AccountTab {
  id: AccountTabId;
  label: string;
}
