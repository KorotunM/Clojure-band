import React from 'react';

const Timer = ({ timeLeft }) => {
  const minutes = Math.floor(timeLeft / 60).toString().padStart(2, '0');
  const seconds = (timeLeft % 60).toString().padStart(2, '0');

  return (
    <div className="timer">
      ⏱ Время: {minutes}:{seconds}
    </div>
  );
};

export default Timer;
