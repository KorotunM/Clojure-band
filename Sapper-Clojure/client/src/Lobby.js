import React, { useState } from 'react';

const Lobby = ({ socket, myId, players }) => {
  const [nick, setNick] = useState('');

  const count = Object.keys(players).length;
  
  const changeNick = () => {
    socket.send(JSON.stringify({
      type:    'lobby/set-nick',
      payload: nick
    }));
  };

  const toggleReady = () => {
    socket.send(JSON.stringify({
      type:    'lobby/toggle-ready'
    }));
  };

  return (
    <div className="lobby">
      <h1>Игровое лобби</h1>
      <div className="nick-input">
        <input
          className="nick-input-field"
          value={nick}
          onChange={e => setNick(e.target.value)}
          placeholder="Введите желаемый ник"
        />
        <button className="nick-input-button" onClick={changeNick}>OK</button>
      </div>
      <h2 className="lobby-count">Игроков: {count}</h2>
      <ul className="lobby-list">
        {Object.entries(players).map(([id, p]) => (
          <li key={id} className={id === myId ? 'self' : ''}>
            {p.nick} {p.ready ? '✔️' : '❌'}
          </li>
        ))}
      </ul>

      <button className="ready-button" onClick={toggleReady}>
        Готов / Не готов
      </button>
    </div>
  );
};

export default Lobby;