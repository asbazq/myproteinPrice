import { useState, useMemo } from 'react';

const formulas = {
  Epley:     { calc: (w, r) => w * (1 + 0.0333 * r),                          desc: '간단하고 널리 알려진 공식. 고반복 시 과대추정 경향' },
  Brzycki:   { calc: (w, r) => (w * 36) / (37 - r),                             desc: '10회 이하에서 정확도 높음. 보수적' },
  Lombardi:  { calc: (w, r) => w * Math.pow(r, 0.10),                           desc: '고반복/고중량 모두에서 안정적. 증가율 완만' },
  Wathan:    { calc: (w, r) => (100 * w) / (48.8 + 53.8 * Math.exp(-0.075 * r)),desc: '고급자에게 적합. 보수적' },
  Mayhew:    { calc: (w, r) => (100 * w) / (52.2 + 41.9 * Math.exp(-0.055 * r)),desc: '벤치프레스에 특화된 공식. 보수적' },
};

function pickRecommended(r) {
  if (r <= 3)  return ['Lombardi', 'Wathan'];
  if (r <= 10) return ['Brzycki'];
  return ['Lombardi', 'Wathan'];
}

export default function OneRMCalculator() {
  const [weight, setWeight] = useState('');
  const [reps, setReps]     = useState('');

  const { rows } = useMemo(() => {
    const w = parseFloat(weight);
    const r = parseInt(reps, 10);
    if (!w || !r || r <= 0 || r >= 37) {
      return { rows: [], recommended: [] };
    }
    const rec = pickRecommended(r);
    const list = Object.entries(formulas).map(([name, f]) => ({
      name,
      value: f.calc(w, r).toFixed(1),
      desc: f.desc,
      recommended: rec.includes(name),
    }));
    return { rows: list, recommended: rec };
  }, [weight, reps]);

  return (
    <section className="one-rm card fade-in">
        <h2 className="sec-title">1RM 계산기</h2>

      <div className="form-row">
        <label>중량 (kg)</label>
        <input
        type="number"
        value={weight}
        onChange={(e) => setWeight(e.target.value)}
        placeholder="예: 100"
        />
    </div>

    <div className="form-row">
        <label>반복 횟수 (reps)</label>
        <input
        type="number"
        value={reps}
        onChange={(e) => setReps(e.target.value)}
        placeholder="예: 5"
        />
    </div>


    {rows.length > 0 && (
        <>
        <div className="table-wrap">
            <table className="rm-table">
            <thead>
                <tr>
                <th>이름</th>
                <th className="num">예측 값 (kg)</th>
                <th>특징</th>
                </tr>
            </thead>
            <tbody>
                {rows.map((r) => (
                <tr key={r.name} className={r.recommended ? 'recommended-row' : ''}>
                    <td>
                    {r.name}
                    {r.recommended && <span className="badge">추천</span>}
                    </td>
                    <td className="num">{r.value}</td>
                    <td>{r.desc}</td>
                </tr>
                ))}
            </tbody>
            </table>
        </div>
        </>
    )}
    </section>
  );
}
