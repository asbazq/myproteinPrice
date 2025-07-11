import React from 'react';
import { RoutineCard } from './RoutineCard';

export function RoutineCollection({ cards, onRemove, onUpdate }) {
  if (!cards.length) {
    return <p className="text-center text-gray-400">아직 카드가 없습니다.</p>;
  }
  return (
    <div className="grid gap-4" style={{
      gridTemplateColumns:'repeat(auto-fill, minmax(280px, 1fr))'
    }}>
      {cards.map(card => (
        <RoutineCard
          key={card.cardId}
          card={card}
          onRemove={onRemove}
          onUpdate={onUpdate}
        />
      ))}
    </div>
  );
}
