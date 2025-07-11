import React, { useState, useRef } from 'react';
import axios from 'axios';

// 카테고리별 운동 목록
const EXERCISE_CATEGORIES = {
  등: ['데드리프트','랫 풀 다운','턱걸이','바벨 로우','케이블 로우','바벨 풀오버'],
  가슴: ['벤치 프레스','딥스','플라이','덤벨 풀오버','팔굽혀펴기'],
  어깨: ['밀리터리 프레스','래터럴 레이즈','업라이트 로우'],
  하체: ['스쿼트','레그 프레스','카프레이즈'],
  복근: ['크런치','AB 롤아웃','윗몸 일으키기','바이시클 메뉴버'],
  삼두: ['스컬 크러셔','트라이셉스 푸시 다운'],
  이두: ['바벨 컬','덤벨 컬'],
};

export function RoutineSender({ onSent }) {
  const [sent, setSent] = useState(false);
  const [emotion, setEmotion] = useState('');
  const [note, setNote] = useState('');
  const [exercises, setExercises] = useState([
    { category:'', name:'', custom:'', sets:'', reps:'', intensity:'' }
  ]);
  const ref = useRef();

  const handleAddExercise = () => {
    setExercises([...exercises, { category:'', name:'', custom:'', sets:'', reps:'', intensity:'' }]);
  };
  const handleChange = (idx, field, value) => {
    const list = [...exercises];
    list[idx][field] = value;
    if (field==='category') list[idx].name = '';
    setExercises(list);
  };

  const handleSend = () => {
    const uuid = localStorage.getItem('uuid');
    const payload = {
      emotion,
      note,
      routine: exercises.map(e=>({
        name: e.name==='기타'? e.custom : e.name,
        sets: Number(e.sets),
        reps: Number(e.reps),
        intensity: e.intensity||null
      }))
    };
    axios.post('/api/routine/upload', payload, { headers:{ 'X-User-UUID': uuid } })
      .then(()=> {
        // 전송 이펙트
        if (ref.current) {
          ref.current.classList.add('send-up');
          ref.current.addEventListener('animationend', ()=>{
            setSent(true);
            onSent();
          }, { once:true });
        }
      })
      .catch(err=> console.error('업로드 에러:', err));
  };

  return (
    <div ref={ref} className="card-container">
      <h2 className="text-lg font-bold mb-2">내 루틴 보내기</h2>
      <input
        className="input mb-2"
        placeholder="감정 요약 (예: 🔥 집중)"
        value={emotion}
        onChange={e=>setEmotion(e.target.value)}
      />
      <textarea
        className="input mb-4"
        placeholder="루틴 설명"
        rows={2}
        value={note}
        onChange={e=>setNote(e.target.value)}
      />

      {exercises.map((ex, idx)=>(
        <div key={idx} className="mb-3">
          <select
            className="select mb-1"
            value={ex.category}
            onChange={e=>handleChange(idx,'category',e.target.value)}
          >
            <option value="">카테고리 선택</option>
            {Object.keys(EXERCISE_CATEGORIES).map(cat=>
              <option key={cat} value={cat}>{cat}</option>
            )}
          </select>

          {ex.category && (
            <select
              className="select mb-1"
              value={ex.name}
              onChange={e=>handleChange(idx,'name',e.target.value)}
            >
              <option value="">운동 선택</option>
              {EXERCISE_CATEGORIES[ex.category].map(name=>
                <option key={name} value={name}>{name}</option>
              )}
              <option value="기타">기타</option>
            </select>
          )}

          {ex.name==='기타' && (
            <input
              className="input mb-1"
              placeholder="직접 입력"
              value={ex.custom}
              onChange={e=>handleChange(idx,'custom',e.target.value)}
            />
          )}

          <input
            className="input mb-1"
            placeholder="세트"
            value={ex.sets}
            onChange={e=>handleChange(idx,'sets',e.target.value)}
          />
          <input
            className="input mb-1"
            placeholder="횟수"
            value={ex.reps}
            onChange={e=>handleChange(idx,'reps',e.target.value)}
          />
          <input
            className="input mb-1"
            placeholder="강도 (예: 75%)"
            value={ex.intensity}
            onChange={e=>handleChange(idx,'intensity',e.target.value)}
          />
        </div>
      ))}

      <button className="button mb-2" onClick={handleAddExercise}>
        운동 추가
      </button>
      <button className="button" onClick={handleSend}>
        루틴 보내기
      </button>
      {sent && <p className="mt-2 text-green-400">루틴이 전송되었습니다!</p>}
    </div>
  );
}
