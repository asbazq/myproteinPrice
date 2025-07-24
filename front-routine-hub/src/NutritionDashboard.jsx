// src/NutritionDashboard.jsx
import React, { useState } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip,
  CartesianGrid, ResponsiveContainer, Legend
} from 'recharts'
import './App.css'

// 1. 데이터 정의 (맛별 영양성분)
const NUTRITION_DATA = [
{ flavor: "내추럴 초콜렛", calories: 97, carbs: 1.5, protein: 18, fat: 1.9 },
  { flavor: "무맛", calories: 103, carbs: 1,   protein: 21,  fat: 1.9 },
  { flavor: "라떼", calories: 103, carbs: 1.5, protein: 20,  fat: 1.8 },
  { flavor: "망고", calories: 103, carbs: 1,   protein: 21,  fat: 1.9 },
  { flavor: "스트로베리 크림", calories: 99,  carbs: 2,   protein: 19,  fat: 1.8 },
  { flavor: "아이스 라떼", calories: 95,  carbs: 2,   protein: 21,  fat: 1.3 },
  { flavor: "민트초코", calories: 98,  carbs: 1.9, protein: 18,  fat: 1.9 },
  { flavor: "블루베리", calories: 101, carbs: 2.1, protein: 19,  fat: 1.8 },
  { flavor: "애플 크럼블 앤 커스타드", calories: 102, carbs: 1.7, protein: 20, fat: 1.8 },
  { flavor: "블루베리 앤 라즈베리 스테비아", calories: 101, carbs: 3.2, protein: 18, fat: 1.7 },
  { flavor: "스트로베리 잼 롤리폴리", calories: 101, carbs: 1.7, protein: 20, fat: 1.8 },
  { flavor: "이튼 메스", calories: 98, carbs: 2.2, protein: 19, fat: 1.5 },
  { flavor: "스테비아 초콜릿", calories: 98, carbs: 1.6, protein: 18, fat: 1.9 },
  { flavor: "내추럴 스트로베리", calories: 103, carbs: 1, protein: 21, fat: 1.9 },
  { flavor: "홋카이도 밀크", calories: 100, carbs: 1.3, protein: 20, fat: 3.1 },
  { flavor: "초콜렛 민트", calories: 98, carbs: 1.9, protein: 18, fat: 1.9 },
  { flavor: "스트로베리 요거트", calories: 91, carbs: 2.1, protein: 18, fat: 1.2 },
  { flavor: "초콜릿 브라우니", calories: 95, carbs: 1.8, protein: 18, fat: 1.7 },
  { flavor: "커피 카라멜", calories: 101, carbs: 1.9, protein: 19, fat: 1.8 },
  { flavor: "내추럴 바나나", calories: 99, carbs: 2, protein: 19, fat: 1.5 },
  { flavor: "스테비아 바나나", calories: 100, carbs: 2.9, protein: 18, fat: 1.7 },
  { flavor: "내추럴 바닐라", calories: 99, carbs: 1.8, protein: 19, fat: 1.8 },
  { flavor: "블루베리 치즈케이크", calories: 103, carbs: 2, protein: 20, fat: 1.8 },
  { flavor: "솔티드 카라멜", calories: 103, carbs: 1.8, protein: 20, fat: 1.8 },
  { flavor: "초콜릿 오렌지", calories: 101, carbs: 1.9, protein: 20, fat: 1.9 },
  { flavor: "다크초콜릿", calories: 98, carbs: 1.9, protein: 18, fat: 2   },
  { flavor: "멜론밀크", calories: 93, carbs: 1.4, protein: 19, fat: 1.3 },
  { flavor: "통단팟", calories: 102, carbs: 2.1, protein: 19, fat: 2   },
  { flavor: "바나나", calories: 99, carbs: 1.8, protein: 19, fat: 1.6 },
  { flavor: "초콜릿카라멜", calories: 103, carbs: 3.3, protein: 19, fat: 1.6 },
  { flavor: "흑임자", calories: 95, carbs: 1, protein: 19, fat: 1.7   },
  { flavor: "골든시럽", calories: 103, carbs: 1.9, protein: 20, fat: 1.8 },
  { flavor: "초콜릿 바나나", calories: 99, carbs: 1.6, protein: 18, fat: 2 },
  { flavor: "흑당", calories: 97, carbs: 1.9, protein: 19, fat: 1.7 },
  { flavor: "쿠키앤크림", calories: 102, carbs: 1.8, protein: 20, fat: 1.8 },
  { flavor: "티라미슈", calories: 97, carbs: 2.3, protein: 19, fat: 1.5 },
  { flavor: "크렘브륄레", calories: 97, carbs: 2.1, protein: 19, fat: 1.5 },
  { flavor: "얼그레이", calories: 92, carbs: 1.4, protein: 18, fat: 1.7 },
  { flavor: "화이트초콜릿", calories: 99, carbs: 1.9, protein: 19, fat: 1.6 },
  { flavor: "밀크티", calories: 97, carbs: 1, protein: 19, fat: 1.8 },
  { flavor: "팥", calories: 96, carbs: 1.4, protein: 19, fat: 1.8 },
  { flavor: "체리요거트", calories: 97, carbs: 1.9, protein: 19, fat: 1.5 },
  { flavor: "말차라떼", calories: 102, carbs: 2.3, protein: 19, fat: 1.7 }
]

export function NutritionDashboard() {
  const [flavor, setFlavor] = useState(NUTRITION_DATA[0].flavor)

  // 선택된 맛의 데이터
  const nut = NUTRITION_DATA.find(d => d.flavor === flavor)

  // BarChart 에 전달할 형태로 변환
  const chartData = [
    { name: '탄수화물 (g)', value: nut.carbs },
    { name: '단백질 (g)', value: nut.protein },
    { name: '지방 (g)', value: nut.fat },
    { name: '칼로리 (kcal)', value: nut.calories }
  ]

  return (
    <div className="nutrition-dashboard p-4 bg-white rounded shadow">
      {/* 맛 선택 드롭다운 */}
      <div className="mb-4">
        <label className="mr-2 font-semibold">맛 선택:</label>
        <select
          value={flavor}
          onChange={e => setFlavor(e.target.value)}
          className="border rounded p-1"
        >
          {NUTRITION_DATA.map(d => (
            <option key={d.flavor} value={d.flavor}>
              {d.flavor}
            </option>
          ))}
        </select>
      </div>

      {/* 영양성분 막대그래프 */}
      <div className="chart">
        <ResponsiveContainer width="100%" height={400}>
            <BarChart data={chartData} margin={{ top: 20, right: 30, bottom: 20, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" tick={{ fontSize: 12 }} />
            <YAxis />
            <Tooltip formatter={val => val.toLocaleString()} />
            <Legend />
            <Bar dataKey="value" fill="#2a9d8f" />
            </BarChart>
        </ResponsiveContainer>
        </div>
    </div>
  )
}
