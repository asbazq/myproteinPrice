import React from 'react';
import { PriceDashboard } from './PriceDashboard';
import { PriceAnalysis }  from './PriceAnalysis';
import { NutritionDashboard } from './NutritionDashboard'
import './App.css';

function App() {
  const productCode = 'IMPACT_WHEY_10530943';

  return (
    <div className="app-container">
      <h1>Impact Whey Protein 가격 변동</h1>

      <div className="dashboard-container">
        {/* 왼쪽 70%: PriceDashboard */}
        <PriceDashboard code={productCode} />

        {/* 오른쪽 30%: Nutrition + Analysis */}
        <div className="right-column">
          <NutritionDashboard />
          <PriceAnalysis code={productCode} />
        </div>
      </div>
    </div>
  );
}
export default App;