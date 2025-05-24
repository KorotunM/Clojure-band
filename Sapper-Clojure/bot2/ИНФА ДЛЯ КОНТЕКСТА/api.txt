const initSocket = (onMessage) => {
    const socket = new WebSocket("ws://localhost:8080/ws");
  
    socket.onopen = () => {
      console.log("WebSocket подключён");
    };
  
    socket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      onMessage(data);
    };
  
    socket.onclose = () => {
      console.log("WebSocket отключён");
    };
  
    return socket;
  };
  
  export default initSocket;
  