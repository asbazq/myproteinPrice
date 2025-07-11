// src/PriceAnalysis.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

axios.defaults.baseURL = 'http://localhost:8080';

export function PriceAnalysis({ code }) {
  const [avgWeek, setAvgWeek]               = useState(null);
  const [forecast, setForecast]             = useState(null);
  const [currentPrice, setCurrentPrice] = useState(null);
  const [loading, setLoading]               = useState(true);

  useEffect(() => {
    setLoading(true);

    const p1 = axios.get('/api/price/myprotein/average-lowest-week', { params: { code } });
    const p2 = axios.get('/api/price/myprotein/arima-forecast',     { params: { code } });
    const p3 = axios.get('/api/price/myprotein/history',           { params: { code, period: 'yearly' } });

    Promise.all([p1, p2, p3])
      .then(([res1, res2, res3]) => {
        setAvgWeek(res1.data);
        setForecast(res2.data); // ← 설정
      const hist = res3.data;
        if (hist.length > 0) {
          const latest = hist.reduce((a, b) =>
            new Date(a.scrapedAt) > new Date(b.scrapedAt) ? a : b
          );
          setCurrentPrice(latest.price);
        }
      })
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
  }, [code]);

  if (loading) {
    return <p className="text-center py-4">분석 중…</p>;
  }
  if (!avgWeek) {
    return <p className="text-center py-4 text-red-500">데이터를 불러올 수 없습니다.</p>;
  }

  return (
    <div className="price-analysis p-4 bg-white rounded shadow mt-6">
      {/* … 기존 평균최저가, 시즌할인율 렌더링 … */}

      {currentPrice !== null && (
        <div className="mt-4">
          <p>
            현재 할인 가격:
            <strong> ₩{currentPrice.toLocaleString()}</strong>
          </p>
        </div>
      )}

      {/* ── ARIMA 예측 결과 ── */}
      {forecast && (
        <div className="forecast mt-6 p-4 bg-gray-50 rounded">
          <p>
            다음 예측 가격:  
            <strong> ₩{Math.round(forecast.nextPrice).toLocaleString()}</strong>
          </p>
          <p>
            가격 하락 확률:  
            <strong> {(forecast.dropProbability * 100).toFixed(1)}%</strong>
          </p>
        </div>
      )}
    </div>
  );
}
