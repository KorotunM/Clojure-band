import React from 'react';

const ScoreBoard = ({ players, scores }) => (
  <div className="scoreboard">
    {Object.entries(scores).map(([id, score]) => (
      <p key={id}>
        {players[id]?.nick || id}: {score}
      </p>
    ))}
  </div>
);

export default ScoreBoard;
