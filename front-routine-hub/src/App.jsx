import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { NavLink } from 'react-router-dom';

import { PriceDashboard }  from './PriceDashboard';
import { PriceAnalysis }   from './PriceAnalysis';
import { NutritionDashboard } from './NutritionDashboard';
import OneRMCalculator      from './OneRMCalculator';
import DiscountBanner       from './DiscountBanner';
import ReferralInfo         from './ReferralInfo';
import './App.css';

const productCode = 'IMPACT_WHEY_10530943';

// 메인 화면(가격 대시보드)
function Home() {
  return (
    <>
      <h1>Impact Whey Protein 가격 변동</h1>

      <div className="dashboard-container">
        <PriceDashboard code={productCode} />
        <div className="right-column">
          <NutritionDashboard />
          <PriceAnalysis code={productCode} />
        </div>
      </div>

      <ReferralInfo />
    </>
  );
}

export default function App() {
  return (
    <div className="app-container">
      <DiscountBanner />

      {/* 네비게이션 */}
      <nav className="main-nav">
        <NavLink to="/" end className="nav-link">
          가격 대시보드
        </NavLink>
        <NavLink to="/1rm" className="nav-link">
          1RM 계산기
        </NavLink>
      </nav>

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/1rm" element={<OneRMCalculator />} />
        {/* <Route path="*" element={<div>404 Not Found</div>} /> */}
      </Routes>
    </div>
  );
}
