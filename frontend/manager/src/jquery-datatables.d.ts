export {};

declare global {
  interface JQuery {
    DataTable(options?: object): { destroy: () => void };
  }
}
