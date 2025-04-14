import React, { useEffect, useState } from 'react';
import GameBoard from './GameBoard';
import ScoreBoard from './ScoreBoard';
import Timer from './Timer';
import initSocket from './api';

const App = () => {
  const [gameState, setGameState] = useState(null);
  const [socket, setSocket] = useState(null);

  useEffect(() => {
    const ws = initSocket((newState) => {
      setGameState(newState);
      localStorage.setItem('sapper-state', JSON.stringify(newState));
    });
    setSocket(ws);

    return () => ws.close();
  }, []);

  const handleOpen = (x, y) => {
    socket?.send(JSON.stringify({ action: "open", x, y, player: 1 }));
  };

  const handleFlag = (x, y) => {
    socket?.send(JSON.stringify({ action: "flag", x, y, player: 1 }));
  };

  if (!gameState) return <div>Загрузка игры...</div>;

  return (
    <div className="app">
      <h1>Сапёр: Дуэль</h1>
      <Timer timeLeft={gameState['time-left']} />
      <ScoreBoard scores={gameState.scores} />
      <GameBoard board={gameState.board} onOpen={handleOpen} onFlag={handleFlag} />
      {gameState.status === "ended" && (
        <div className="overlay">
          <h2>Игра окончена!</h2>
          <p>Победитель: Игрок {gameState.winner || '—'}</p>
        </div>
      )}
    </div>
  );
};

export default App;
