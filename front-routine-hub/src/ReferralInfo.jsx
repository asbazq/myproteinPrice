import React from 'react';

export default function ReferralInfo() {
  const code = 'HAXJ-RC';
  const link = `https://www.myprotein.co.kr/referrals.list?applyCode=${code}`;

  return (
    <div className="referral-info-footer">
      <p>추천인 코드: <strong>{code}</strong></p>
      <p>
        추천인 링크:{" "}
        <a href={link} target="_blank" rel="noopener noreferrer">
          {link}
        </a>
      </p>
    </div>
  );
}
