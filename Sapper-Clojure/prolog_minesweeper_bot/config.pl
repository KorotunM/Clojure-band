% Время ожидания между ходами (в секундах)
delay(1).

% Получить задержку
:- dynamic delay/1.
:- multifile delay/1.
get_delay(D) :- delay(D).
