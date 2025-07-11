import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';
import './App.css';

export function RoutineCard({ card, onRemove, onUpdate }) {
  const [flipped, setFlipped]     = useState(false);
  const [likes, setLikes]         = useState(card.likes    ?? 0);
  const [dislikes, setDislikes]   = useState(card.dislikes ?? 0);
  const ref                       = useRef();
  const uuid                      = localStorage.getItem('uuid');
  const [liked, setLiked]         = useState(card.liked   ?? false);
  const [disliked, setDisliked]   = useState(card.disliked?? false);

  useEffect(() => {
    if (ref.current) ref.current.classList.add('drop-in');
  }, []);

  const handleFlip = e => {
    e.stopPropagation();
    setFlipped(f => !f);
  };

    const handleLike = () => {
        axios.post(`/api/routine/recommend/${card.cardId}`, null, {
            headers: { 'X-User-UUID': uuid }
        })
        .then(res => {
            if (!liked) {
            // 새로 추천
            setLikes(likes + 1);
            // 만약 반대 상태(disliked)면 되돌리기
            if (disliked) {
                setDislikes(dislikes - 1);
                setDisliked(false);
            }
            } else {
            // 추천 취소
            setLikes(likes - 1);
            }
            setLiked(!liked);
            onUpdate?.(card.cardId, { likes: likes + (liked ? -1 : +1) });
        })
        .catch(err => console.error(err));
    };

    const handleDislike = () => {
        axios.post(`/api/routine/dislike/${card.cardId}`, null, {
            headers: { 'X-User-UUID': uuid }
        })
        .then(res => {
            if (!disliked) {
            setDislikes(dislikes + 1);
            if (liked) {
                setLikes(likes - 1);
                setLiked(false);
            }
            } else {
            setDislikes(dislikes - 1);
            }
            setDisliked(!disliked);
            onUpdate?.(card.cardId, { dislikes: dislikes + (disliked ? -1 : +1) });
        })
        .catch(err => console.error(err));
        };

    const handleBlock = () => {
        axios.post(`/api/routine/block/${card.fromUuid}`, null, {
        headers: { 'X-User-UUID': uuid }
        })
        .then(() => onRemove?.(card.cardId))
        .catch(console.error);
    };


  if (card.blocked) return null;

  return (
    <div
      ref={ref}
      className={`routine-card border-${card.emotion} flip-container ${flipped ? 'flipped' : ''}`}
    >
      <div className="flipper">
        {/* Front */}
        <div className="front">
          {/* ...헤더/바디/노트... */}
          <div className="routine-card__footer">
            <button
                className={`likes ${liked ? 'active' : ''}`}
                onClick={handleLike}
                >
                ❤️ {likes}
            </button>
            <button
                className={`reviews ${disliked ? 'active' : ''}`}
                onClick={handleDislike}
                >
                👎 {dislikes}
            </button>
            <button onClick={handleBlock}>🚫 차단</button>
            <button onClick={handleFlip}>🔄 리뷰 보기</button>
          </div>
        </div>
        {/* Back */}
        <div className="back">
          <div className="back-content">
            <h4>My Review</h4>
            <p>{card.myReview || '리뷰를 남겨보세요!'}</p>
            <button onClick={handleFlip}>◀︎ 돌아가기</button>
          </div>
        </div>
      </div>
    </div>
  );
}
