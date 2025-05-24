#!/usr/bin/env swipl

:- initialization(main, main).
:- use_module(library(http/json)).
:- use_module(library(http/json_convert)).
:- use_module(library(socket)).
:- use_module(library(random)).
:- use_module(library(thread)).
:- dynamic my_id/1.

main :-
    sleep(2),
    connect_bot('ws://localhost:8080/ws').

connect_bot(Url) :-
    catch(
        (tcp_connect(localhost:8080, Stream, []),
         format("Connected to server~n", []),
         % Отправляем HTTP запрос для WebSocket
         format(Stream, "GET /ws HTTP/1.1\r\n", []),
         format(Stream, "Host: localhost:8080\r\n", []),
         format(Stream, "Upgrade: websocket\r\n", []),
         format(Stream, "Connection: Upgrade\r\n", []),
         format(Stream, "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n", []),
         format(Stream, "Sec-WebSocket-Version: 13\r\n", []),
         format(Stream, "\r\n", []),
         flush_output(Stream),
         format("WebSocket handshake sent~n", []),
         % Читаем ответ сервера
         read_line_to_codes(Stream, Response),
         format("Server response: ~s~n", [Response]),
         % Пропускаем остальные заголовки
         skip_headers(Stream),
         format("Headers skipped~n", []),
         thread_create(handle_incoming(Stream), _, [detached(true)]),
         format("Incoming message handler started~n", []),
         send_nick_and_ready(Stream),
         loop_play(Stream)),
        Error,
        (format("Connection error: ~w~n", [Error]), fail)
    ).

skip_headers(Stream) :-
    read_line_to_codes(Stream, Line),
    ( Line = [] -> true
    ; skip_headers(Stream)
    ).

handle_incoming(Stream) :-
    catch(
        (repeat,
         format("Waiting for incoming message...~n", []),
         read_websocket_frame(Stream, Frame),
         format("Received frame: ~w~n", [Frame]),
         ( Frame.opcode == 1 -> % text frame
             atom_codes(Json, Frame.data),
             format("Raw JSON: ~s~n", [Json]),
             catch(
                 atom_json_dict(Json, Dict, []),
                 Error,
                 (format("JSON parsing error: ~w~n", [Error]), fail)
             ),
             format("Parsed message: ~w~n", [Dict]),
             ( _{type:"lobby/update", players:Players} :< Dict ->
                 format("Lobby update: ~w~n", [Players])
             ; _{type:"state", state:State} :< Dict ->
                 format("Game state update: ~w~n", [State])
             ; true
             )
         ; format("Non-text frame: ~w~n", [Frame])
         ),
         fail),
        Error,
        (format("Error in handle_incoming: ~w~n", [Error]), fail)
    ).

read_websocket_frame(Stream, frame(Opcode, Data)) :-
    get_byte(Stream, FirstByte),
    get_byte(Stream, SecondByte),
    Opcode is FirstByte /\ 0x0F,
    Length is SecondByte /\ 0x7F,
    ( Length == 126 ->
        get_byte(Stream, Length1),
        get_byte(Stream, Length2),
        FrameLength is Length1 * 256 + Length2
    ; Length == 127 ->
        get_byte(Stream, Length1),
        get_byte(Stream, Length2),
        get_byte(Stream, Length3),
        get_byte(Stream, Length4),
        FrameLength is Length1 * 16777216 + Length2 * 65536 + Length3 * 256 + Length4
    ; FrameLength = Length
    ),
    read_n_bytes(Stream, FrameLength, Data).

read_n_bytes(_, 0, []).
read_n_bytes(Stream, N, [Byte|Rest]) :-
    N > 0,
    get_byte(Stream, Byte),
    N1 is N - 1,
    read_n_bytes(Stream, N1, Rest).

send_nick_and_ready(Stream) :-
    sleep(1),
    format("Sending nickname and ready~n", []),
    with_mutex(setup,
      (
        % ждём идентификатор
        repeat,
        format("Waiting for message...~n", []),
        read_websocket_frame(Stream, Frame),
        format("Received frame: ~w~n", [Frame]),
        ( Frame.opcode == 1 -> % text frame
            atom_codes(Json, Frame.data),
            format("Raw JSON: ~s~n", [Json]),
            catch(
                atom_json_dict(Json, Dict, []),
                Error,
                (format("JSON parsing error: ~w~n", [Error]), fail)
            ),
            format("Parsed dict: ~w~n", [Dict]),
            ( _{type:"lobby/joined", id:ID} :< Dict ->
                assertz(my_id(ID)),
                format("My ID: ~w~n", [ID]),
                send_json(Stream, _{type:"lobby/set-nick", payload:"bot-pro"}),
                format("Sent nickname~n", []),
                sleep(1),
                send_json(Stream, _{type:"lobby/toggle-ready"}),
                format("Sent ready~n", []),
                !
            ; format("Not a lobby/joined message~n", [])
            )
        ; format("Not a text frame~n", [])
        )
      )
    ).

loop_play(Stream) :-
    repeat,
    sleep(1),
    random_between(0, 19, X),
    random_between(0, 19, Y),
    my_id(ID),
    Msg = _{type:"game/open", payload:_{x:X, y:Y, player:ID}},
    send_json(Stream, Msg),
    format("Opened cell: ~w ~w~n", [X, Y]),
    sleep(1),
    fail.

send_json(Stream, Dict) :-
    catch(
        atom_json_dict(Json, Dict, []),
        Error,
        (format("JSON encoding error: ~w~n", [Error]), fail)
    ),
    send_websocket_frame(Stream, 1, Json). % 1 = text frame

send_websocket_frame(Stream, Opcode, Data) :-
    atom_codes(Data, Codes),
    length(Codes, Length),
    ( Length < 126 ->
        put_byte(Stream, 0x80 \/ Opcode), % FIN bit set
        put_byte(Stream, Length)
    ; Length < 65536 ->
        put_byte(Stream, 0x80 \/ Opcode),
        put_byte(Stream, 126),
        Length1 is Length // 256,
        Length2 is Length mod 256,
        put_byte(Stream, Length1),
        put_byte(Stream, Length2)
    ; put_byte(Stream, 0x80 \/ Opcode),
      put_byte(Stream, 127),
      Length1 is Length // 16777216,
      Length2 is (Length // 65536) mod 256,
      Length3 is (Length // 256) mod 256,
      Length4 is Length mod 256,
      put_byte(Stream, Length1),
      put_byte(Stream, Length2),
      put_byte(Stream, Length3),
      put_byte(Stream, Length4)
    ),
    maplist(put_byte(Stream), Codes),
    flush_output(Stream).
