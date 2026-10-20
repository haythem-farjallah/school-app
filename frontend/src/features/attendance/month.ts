/** Calendar arithmetic only: API date strings never pass through UTC conversion. */
export function attendanceMonth(params: URLSearchParams, now = new Date()): string {
  const values = params.getAll("month");
  const month = values[0];
  if (values.length === 1 && /^\d{4}-(0[1-9]|1[0-2])$/.test(month) && !month.startsWith("0000")) {
    return month;
  }
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

export function attendanceMonthBounds(month: string) {
  const [year, number] = month.split("-").map(Number);
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = number === 2 ? (leapYear ? 29 : 28) : [4, 6, 9, 11].includes(number) ? 30 : 31;
  return { startDate: `${month}-01`, endDate: `${month}-${days}` };
}

export function adjacentAttendanceMonth(month: string, offset: -1 | 1): string {
  const [year, number] = month.split("-").map(Number);
  const index = year * 12 + number - 1 + offset;
  return `${String(Math.floor(index / 12)).padStart(4, "0")}-${String(index % 12 + 1).padStart(2, "0")}`;
}
