:- dynamic cell/3.
:- dynamic board_size/2.
:- dynamic stat/2.

reset_board :-
    retractall(cell(_, _, _)),
    retractall(board_size(_, _)),
    assertz(stat(opened, 0)),
    assertz(stat(flags, 0)),
    assertz(stat(games, 0)).

update_board(Payload) :-
    retractall(cell(_,_,_)),
    retractall(board_size(_, _)),
    assertz(board_size(Payload.width, Payload.height)),
    forall(member(Row, Payload.board), (
        nth0(Y, Payload.board, Row),
        nth0(X, Row, Value),
        asserta(cell(X, Y, Value))
    )).

random_unknown_cell(X, Y) :-
    findall((X1,Y1), cell(X1,Y1,"unknown"), Unknowns),
    Unknowns \= [],
    random_member((X,Y), Unknowns).

increment_stat(Key) :-
    (retract(stat(Key, V)) -> V1 is V + 1 ; V1 = 1),
    asserta(stat(Key, V1)).

log(Event) :-
    get_time(TS),
    format_time(atom(Time), '%Y-%m-%d %H:%M:%S', TS),
    format("~w ~w~n", [Time, Event]).
