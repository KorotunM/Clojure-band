import React from 'react';

const Cell = ({ x, y, cell, onOpen, onFlag }) => {
  const handleClick = () => onOpen(x, y);
  const handleRightClick = (e) => {
    e.preventDefault();
    onFlag(x, y);
  };

  let className = 'cell';
  let content = '';

  if (cell.opened) {
    className += ' open';
    if (cell.mine) content = '💥';
    else if (cell.adjacent > 0) content = cell.adjacent;
  } else if (cell.flagged) {
    content = '🚩';
  }

  return (
    <div
      className={className}
      onClick={handleClick}
      onContextMenu={handleRightClick}
    >
      {content}
    </div>
  );
};

export default Cell;
