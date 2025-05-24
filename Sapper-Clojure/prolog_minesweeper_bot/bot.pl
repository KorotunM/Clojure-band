:- use_module(library(http/websocket)).
:- use_module(library(http/json)).
:- use_module(library(http/json_convert)).
:- use_module(library(random)).
:- use_module(library(thread)).

:- [logic].
:- [config].

start_bot :-
    connect_ws(WS),
    random_between(1000, 9999, Num),
    format(string(Nick), "prolog-bot-~w", [Num]),
    json_write_dict(current_output, _{type:"lobby/set-nick", payload:Nick}), nl,
    ws_send_json(WS, json(_{type:"lobby/set-nick", payload:Nick})),
    ws_send_json(WS, json(_{type:"lobby/toggle-ready", payload:null})),
    thread_create(listen_loop(WS), _, [detached(true)]),
    thread_create(game_loop(WS), _, [detached(true)]).

connect_ws(WS) :-
    catch(
        ws_open("ws://localhost:8080", WS, []),
        Error,
        (print_message(error, Error), sleep(2), connect_ws(WS))
    ).

listen_loop(WS) :-
    repeat,
    (   catch(ws_receive(WS, Message, [format(json)]), _, fail),
        handle_message(Message, WS),
        fail
    ;   true).

handle_message(json(Message), WS) :-
    log(received(Message)),
    (   Message.type = "game/update" ->
            update_board(Message.payload),
            (Message.payload.game_over = true -> restart_game(WS) ; true)
    ;   Message.type = "lobby/reset" ->
            ws_send_json(WS, json(_{type:"lobby/toggle-ready", payload:null}))
    ;   true
    ).

restart_game(WS) :-
    reset_board,
    ws_send_json(WS, json(_{type:"game/restart", payload:null})).

game_loop(WS) :-
    repeat,
    sleep(Delay),
    (   random_unknown_cell(X, Y) ->
            ws_send_json(WS, json(_{type:"game/open", payload:_{x:X, y:Y}})),
            increment_stat(opened),
            log(sent(open(X,Y)))
    ;   true),
    fail.
