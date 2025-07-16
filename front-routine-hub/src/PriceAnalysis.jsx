// src/PriceAnalysis.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

axios.defaults.baseURL = 'http://43.201.28.73:8080';

export function PriceAnalysis({ code }) {
  const [avgWeek,       setAvgWeek]       = useState(null);
  const [forecast,      setForecast]      = useState(null);   // 내일 예측
  const [cheapest,      setCheapest]      = useState(null);   // 다음 달 중 최저 예측
  const [currentPrice,  setCurrentPrice]  = useState(null);
  const [loading,       setLoading]       = useState(true);

  // 커스텀 offset 예측
  const [customOffset,  setCustomOffset]  = useState(1);
  const [customValue,   setCustomValue]   = useState(null);

  useEffect(() => {
    setLoading(true);

    const p1 = axios.get('/api/price/myprotein/average-lowest-week', { params: { code } });
    const p2 = axios.get('/api/price/myprotein/arima-forecast',      { params: { code } });
    const p3 = axios.get('/api/price/myprotein/history',            { params: { code, period: 'yearly' } });
    const p4 = axios.get('/api/price/myprotein/forecast/cheapest',   { params: { code } });

    Promise.all([p1, p2, p3, p4])
      .then(([res1, res2, res3, res4]) => {
        setAvgWeek(res1.data);
        setForecast(res2.data);

        const hist = res3.data;
        if (hist.length > 0) {
          const latest = hist.reduce((a, b) =>
            new Date(a.scrapedAt) > new Date(b.scrapedAt) ? a : b
          );
          setCurrentPrice(latest.price);
        }

        setCheapest({
          date: res4.data.date,
          predictedPrice: res4.data.predictedPrice
        });
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [code]);

  // 커스텀 offset 예측 불러오기
  const loadCustom = () => {
    axios.get('/api/price/myprotein/forecast/offset', { params: { code, offset: customOffset } })
      .then(({ data }) => setCustomValue({ offset: customOffset, predicted: data.predictedPrice }))
      .catch(console.error);
  };

  if (loading) return <p className="text-center py-4">분석 중…</p>;
  if (!avgWeek)  return <p className="text-center py-4 text-red-500">데이터를 불러올 수 없습니다.</p>;

  return (
    <div className="price-analysis p-4 bg-white rounded shadow mt-6">
      {/* 현재 가격, 최저 예측 */}
      {currentPrice !== null && (
        <div className="mt-4">
          <p>
            현재 할인 가격: <strong>₩{currentPrice.toLocaleString()}</strong>
          </p>
          {cheapest && (
            <p>
              최저 예측: <strong>₩{Math.round(cheapest.predictedPrice).toLocaleString()}</strong>
            </p>
          )}
        </div>
      )}

      {/* ARIMA 내일 예측 */}
      {forecast && (
        <div className="forecast mt-6 p-4 bg-gray-50 rounded">
          <p>
            내일 예측 가격: <strong>₩{Math.round(forecast.nextPrice).toLocaleString()}</strong>
          </p>
          <p>
            가격 하락 확률: <strong>{(forecast.dropProbability * 100).toFixed(1)}%</strong>
          </p>
        </div>
      )}

      {/* ---- 커스텀 offset 예측 ---- */}
      <div className="mt-6 p-4 bg-gray-50 rounded">
        <label>
          <select value={customOffset} onChange={e => setCustomOffset(+e.target.value)} className="mr-2">
            {Array.from({ length: 30 }, (_, i) => (
              <option key={i + 1} value={i + 1}>{i + 1}일 뒤</option>
            ))}
          </select>
          <button className="period-btn" onClick={loadCustom}>예측</button>
        </label>
        {customValue && (
          <p className="mt-2">
            {customValue.offset}일 뒤 예측 가격: <strong>₩{Math.round(customValue.predicted).toLocaleString()}</strong>
          </p>
        )}
      </div>
    </div>
  );
}

