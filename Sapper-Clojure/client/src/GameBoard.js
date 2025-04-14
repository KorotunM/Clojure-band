import React from 'react';
import Cell from './Cell';

const GameBoard = ({ board, onOpen, onFlag }) => {
  // Проверяем есть ли board вообще
  if (!board) {
    return <div>Загрузка поля...</div>; // можно просто return null если не хочешь показывать текст
  }

  return (
    <div className="board">
      {board.map((row, x) => (
        <div className="row" key={x}>
          {row.map((cell, y) => (
            <Cell
              key={`${x}-${y}`}
              x={x}
              y={y}
              cell={cell}
              onOpen={onOpen}
              onFlag={onFlag}
            />
          ))}
        </div>
      ))}
    </div>
  );
};

export default GameBoard;
