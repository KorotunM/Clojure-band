import React from 'react';

const ScoreBoard = ({ scores }) => {
  return (
    <div className="scoreboard">
      <p>Игрок 1: {scores[1]}</p>
      <p>Игрок 2: {scores[2]}</p>
    </div>
  );
};

export default ScoreBoard;
