// src/App.js
import React, { useEffect, useState } from 'react';
import Lobby from './Lobby';
import GameBoard from './GameBoard';
import ScoreBoard from './ScoreBoard';
import Timer from './Timer';
import initSocket from './api';

const App = () => {
  const [socket, setSocket] = useState(null);
  const [myId, setMyId] = useState(null);
  const [inLobby, setInLobby] = useState(true);
  const [lobbyPlayers, setLobbyPlayers] = useState({});
  const [gameState, setGameState] = useState(null);


  useEffect(() => {
    const ws = initSocket();
    setSocket(ws);
    ws.onmessage = (evt) => {
      const msg = JSON.parse(evt.data);
      switch (msg.type) {
        // Присвоили свой ID
        case 'lobby/joined':
          setMyId(msg.id);
          break;

        // Обновили состав лобби
        case 'lobby/update':
          setLobbyPlayers(msg.players);
          break;

        // Все готовы — старт игры
        case 'game/start':
          setInLobby(false);
          break;

        // Игровое состояние
        case 'state':
          setGameState(msg.state);
          break;

        case 'lobby/reset':
          // принудительный возврат всех в лобби:
          setInLobby(true);
          setGameState(null);
          break;

        default:
          console.warn('Неизвестный тип сообщения:', msg.type);
      }
    };
    return () => ws.close();
  }, []);

  // игровые команды
  const handleOpen = (x, y) => {
    socket.send(JSON.stringify({
      type: 'game/open',
      payload: { x, y, player: myId }
    }));
  };

  const handleFlag = (x, y) => {
    socket.send(JSON.stringify({
      type: 'game/flag',
      payload: { x, y, player: myId }
    }));
  };

  // при рестарте возвращаем UI в лобби
  const handleRestart = () => {
    socket.send(JSON.stringify({ type: 'game/restart' }));
    // если вы хранили gameState в localStorage, тоже сбросьте его:
    localStorage.removeItem('sapper-state');
  };

  // ранжирование по очкам: [ [id, score], ... ], сортировка по убыванию очков
  const ranking = gameState
    ? Object.entries(gameState.scores)
      .sort(([idA, scoreA], [idB, scoreB]) => {
        const explodedA = gameState.exploded === idA;
        const explodedB = gameState.exploded === idB;
        // сначала не‑взорвавшиеся (false) выше, взорвавшиеся (true) ниже
        if (explodedA !== explodedB) {
          return explodedA ? 1 : -1;
        }
        // если оба в одной категории — сортировка по убыванию очков
        return scoreB - scoreA;
      })
    : [];

  if (inLobby) {
    return (
      <Lobby
        socket={socket}
        myId={myId}
        players={lobbyPlayers}
      />
    );
  }

  if (!gameState) {
    return <div>Загрузка игры…</div>;
  }

  return (
    <div className="app">
      <h1>Сапёр: Королевская битва</h1>
      <Timer timeLeft={gameState['time-left']} />
      <ScoreBoard players={lobbyPlayers} scores={gameState.scores} />
      <GameBoard board={gameState.board}
        onOpen={handleOpen}
        onFlag={handleFlag} />
      {gameState.status === "ended" && (
        <div className="overlay">
          <h2>Игра окончена!</h2>
          <table className="results-table">
            <thead>
              <tr><th>№</th><th>Игрок</th><th>Результат</th></tr>
            </thead>
            <tbody>
              {ranking.map(([id, score], idx) => {
                const nick = lobbyPlayers[id]?.nick || id;
                const cell = (gameState.exploded === id)
                  ? '💥'
                  : score;
                return (
                  <tr key={id} className={id === myId ? 'highlight' : ''} >
                    <td>{idx + 1}</td>
                    <td>{nick}</td>
                    <td>{cell}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          <button onClick={handleRestart} className="restart-button">
            Начать заново
          </button>
        </div>
      )}
    </div>
  );
};

export default App;
