import React, {useState, useEffect} from 'react';
import axios from 'axios';
import {
  LineChart, Line, XAxis, YAxis, Tooltip,
  CartesianGrid, ResponsiveContainer
} from 'recharts';

axios.defaults.baseURL = 'http://43.201.28.73:8080';

export function PriceDashboard({ code }) {
  const [period, setPeriod]   = useState("yearly");
  const [history, setHistory] = useState([]);
  const [best, setBest] = useState(null);
  const [forecast30, setForecast30]   = useState([]);
  const [showMonthOnly, setShowMonthOnly] = useState(false);

  
useEffect(() => {
  axios.get('/api/price/myprotein/history', {
    params: { code, period }
  })
  .then(res => {
    let arr = res.data
      .map(e => {
        const dt = new Date(e.scrapedAt);
        return {
          rawDate: dt,
          label:
            period === 'daily'
              ? dt.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})
              : dt.toISOString().split('T')[0],
          originPrice: e.originPrice,
          price: e.price,
          discountRate: e.discountRate
        };
      });

    if (period === 'yearly') {
      // 월별 grouping & 최저가만 남기기
      const byMonth = arr.reduce((acc, item) => {
        const monthKey = item.rawDate.getFullYear() + '-' + String(item.rawDate.getMonth()+1).padStart(2,'0');
        if (!acc[monthKey] || item.price < acc[monthKey].price) {
          acc[monthKey] = item;
        }
        return acc;
      }, {});
      arr = Object.values(byMonth)
        // 월별 순 정렬
        .sort((a,b) => a.rawDate - b.rawDate)
        // 라벨을 “YYYY-MM”으로 변경
        .map(item => ({ 
          ...item, 
          label: item.rawDate.getFullYear() + '-' 
                 + String(item.rawDate.getMonth()+1).padStart(2,'0') 
        }));
    } else {
      arr.sort((a, b) => a.rawDate - b.rawDate);
    }

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

const fetchMonthForecast = () => {
  axios
    .get('/api/price/myprotein/forecast/multi', {
      params: { code, horizonDays: 30 }   // ← 1~30일 전체
    })
    .then(({ data }) =>
      setForecast30(
        data.points.map(p => ({
          label: p.date.split('T')[0],
          predictedPrice: p.predictedPrice
        }))
      )
    )
    .catch(console.error)
    .finally(() => setShowMonthOnly(true));
};

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
              onClick={() => { setPeriod(p); setShowMonthOnly(false); }}
              className={(!showMonthOnly && p === period) ? 'active' : 'normal'}
            >
              {p==="daily"   ? "일간"
              : p==="weekly" ? "주간"
              : p==="monthly"? "월간"
              :               "연간"}
            </button>
          ))}

        <button
          onClick={fetchMonthForecast}  /* ↓ 아래 함수 */
          className={showMonthOnly ? 'active' : 'normal'}
        >
          월간 예측
        </button>
      </div>

      {/* ── 그래프 영역 ── */}
      {showMonthOnly ? (
        /* 월간 예측 전용 그래프 */
        <div className="chart-wrapper">
          <ResponsiveContainer width="100%" height={450}>
            <LineChart data={forecast30} margin={{ top: 10, right: 30, bottom: 60, left: 0 }}>
              <CartesianGrid stroke="#eee" />
              <XAxis
                dataKey="label"
                interval={0}
                height={60}
                tick={{ fontSize: 12 }}
                angle={-45}
                textAnchor="end"
              />
              <YAxis
                domain={[0, 300000]}
                tickFormatter={v => `₩${(v/1000).toFixed(0)}k`}
              />
              <Tooltip formatter={tooltipFormatter} />
              <Line type="monotone" dataKey="predictedPrice"
                    name="예측(30일)" stroke="#4361ee" dot={false} strokeDasharray="4 4"/>
            </LineChart>
          </ResponsiveContainer>
        </div>
      ) : (
        history.length > 0 ? (
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
        )
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
                  ? Math.round(bestWeekAvg).toLocaleString() 
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
  