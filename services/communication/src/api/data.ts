export function formatarData(data: Date): string {
  return data.toISOString().slice(0, -1);
}
