import React, {useState, useEffect} from 'react';
import axios from 'axios';
import {
  LineChart, Line, XAxis, YAxis, Tooltip,
  CartesianGrid, ResponsiveContainer
} from 'recharts';

axios.defaults.baseURL = 'http://localhost:8080';

export function PriceDashboard({ code }) {
  const [period, setPeriod]   = useState("yearly");
  const [history, setHistory] = useState([]);
  const [best, setBest] = useState(null);

  useEffect(() => {
    axios.get('/api/price/myprotein/history', {
      params: { code, period }
    })
    .then(res => {
      const arr = res.data
        .map(e => {
          const dt = new Date(e.scrapedAt);
          return {
            label:
              period === 'daily'
                ? dt.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})
                : dt.toISOString().split('T')[0],
            originPrice: e.originPrice,
            price: e.price,
            discountRate: e.discountRate  // ← 여기 추가
          };
        })
        .sort((a, b) => new Date(a.label) - new Date(b.label));
      setHistory(arr);
    })
    .catch(err => console.error(err));

    axios.get('/api/price/myprotein/best-time', {
      params: { code }
    })
    .then(res => {
      // 컨트롤러에서 받고 있는 DTO가 (weekOfMonth, avgPrice)이면 변환 필요
      // 만약 /best-time 같은 형태라면 그걸 맞춰 가져오시면 됩니다.
      setBest(res.data);
    })
    .catch(err => console.error('Best Time 호출 에러:', err));

  }, [code, period]);

  // 툴팁 포맷터: 가격은 ₩, 할인율은 %
  const tooltipFormatter = (value, name) => {
    if (name === "할인율") {
      return [`${value}%`, name];
    }
    if (name === "원가") {
      return [`₩${value.toLocaleString()}`, name];
    }
    // 그 외(price)는 할인가
    return [`₩${value.toLocaleString()}`, name];
  };

  const bestWeekAvg = best?.weeklyAverages
    ?.find(w => w.weekOfMonth === best.bestWeekOfMonth)
    ?.averagePrice;

  return (
    <div className="price-dashboard w-full p-4 bg-white rounded shadow">
      {/* 기간 선택 버튼 */}
      <div className="mb-4 period-select">
        {["daily","weekly","monthly","yearly"].map(p => (
          <button
            key={p}
            onClick={()=>setPeriod(p)}
            className={p===period ? "active":"normal"}
          >
            {p==="daily"   ? "일간"
             : p==="weekly" ? "주간"
             : p==="monthly"? "월간"
             :               "연간"}
          </button>
        ))}
      </div>

      {history.length > 0 ? (
        <div className="chart-wrapper">
        <ResponsiveContainer width="100%" height={450}>
          <LineChart data={history} margin={{ bottom: 60 }}>
            <CartesianGrid stroke="#eee" />

            {/* X축: 날짜/시간 */}
            <XAxis
              dataKey="label"
              interval={0}
              height={60}
              tick={{ fontSize: 12 }}
              angle={-45}
              textAnchor="end"
            />

            {/* 왼쪽 Y축: 가격 (150,000원 ~ 300,000원) */}
            <YAxis
              yAxisId="left"
              domain={[0, 300000]}
              tickFormatter={v => `₩${(v/1000).toFixed(0)}k`}
            />

            {/* 오른쪽 Y축: 할인율 (30% ~ 70%) */}
            <YAxis
              yAxisId="right"
              orientation="right"
              domain={[0, 100]}
              tickFormatter={v => `${v}%`}
            />

            <Tooltip formatter={tooltipFormatter} />

            {/* 원가: 빨간 */}
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="originPrice"
              name="원가"
              stroke="#e63946"
              dot={false}
            />
            {/* 할인가: 초록 */}
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="price"
              name="할인가"
              stroke="#2a9d8f"
              dot={{ r: 3 }}
            />
            {/* 할인율: 주황 */}
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="discountRate"
              name="할인율"
              stroke="#f4a261"
              dot={{ r: 3 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      ) : (
        <p className="text-center py-8 text-gray-500">
          아직 수집된 가격 데이터가 없습니다.
        </p>
      )}

      {/* 최저가 & 주차별 평균 최저가 */}
      {best && (
        <div className="best-time mt-6 p-4 bg-gray-100 rounded">
          <h3 className="font-semibold">Best Time to Buy</h3>
          <p>
            최저가 기록:&nbsp;
            <strong>₩{best.minPrice.toLocaleString()}</strong>&nbsp;
            (날짜: {best.minPriceDate})
          </p>
          <p className="mt-2">
            최근 3개월 중&nbsp;
            <strong>{best.bestWeekOfMonth}주차</strong>&nbsp;
            평균 최저가:&nbsp;
            <strong>
              ₩{bestWeekAvg != null 
                  ? bestWeekAvg.toLocaleString() 
                  : 'N/A'}
            </strong>
          </p>
        </div>
      )}
  
     {/* 하단 우측 기준 텍스트 */}
     <div className="data-source">
       IMPACT_WHEY 내추럴 초콜렛 2x2.5kg 기준
     </div>
    </div>
  );
}
  