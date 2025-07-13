import React from 'react';
import Holidays from 'date-holidays';

export default function DiscountBanner() {
  const today = new Date();
  const month = today.getMonth() + 1;
  const date  = today.getDate();

  // 1) 월===일 이벤트 체크 (1/1,2/2,…)
  const isMonthDay = month === date;

  // 2) 공휴일 체크 (KR 기준)
  const hd = new Holidays('KR');
  const holidayInfo = hd.isHoliday(today);
  const isHoliday = !!holidayInfo;

  if (!isMonthDay && !isHoliday) return null;

  // 표시할 라벨 조합
  let label = [];
  if (isMonthDay) label.push(`${month}/${date} 특가 이벤트`);
  if (isHoliday) {
    // holidayInfo가 배열일 수도 있으니 문자열로 변환
    const names = Array.isArray(holidayInfo)
      ? holidayInfo.map(h => h.name).join(', ')
      : holidayInfo.name;
    label.push(`${names} 공휴일 할인`);
  }

  return (
    <div className="discount-banner">
      <strong>🎉 오늘은 {label.join(' · ')} 진행 중입니다! 🎉</strong>
    </div>
  );
}
