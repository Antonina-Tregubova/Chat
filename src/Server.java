
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<String, Connection>();

    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {

            String userName;
            Message answer;
            do {
                connection.send(new Message(MessageType.NAME_REQUEST));
                answer = connection.receive();
                userName = answer.getData();
            }
            while (!(answer.getType().equals(MessageType.USER_NAME)) || userName.isEmpty() || connectionMap.containsKey(userName));
            connectionMap.put(userName, connection);
            connection.send(new Message(MessageType.NAME_ACCEPTED));
            return userName;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            Message message = null;
            for (String name: connectionMap.keySet()) {
                if(!name.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, name));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (1 > 0) {
                Message answer = connection.receive();
                if (answer.getType() == (MessageType.TEXT)) {
                    String newMassage = userName + ":" + " " + answer.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, newMassage));
                }
                else {
                    ConsoleHelper.writeMessage("Ошибка, неверный формат ввода");
                }
            }
        }

        public void run() {
            ConsoleHelper.writeMessage("New connection with " + socket.getRemoteSocketAddress());
            String userName = null;
            try (Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);}
            catch (Exception e) {
                e.printStackTrace();
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом.");
            }
            finally {
                if((userName != null) && (!userName.isEmpty())) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                }
            }
            ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");
        }
    }

    public static void sendBroadcastMessage(Message message){
        try {
            for (Connection connection: connectionMap.values()) {
                connection.send(message);
            }
        } catch (IOException e) {
            System.out.println("Сообщение не может быть отправлено.");
        }
    }

    public static void main(String[] args) {

        try (
                ServerSocket ss = new ServerSocket(ConsoleHelper.readInt())) {
            ConsoleHelper.writeMessage("Серевер запущен.");
            while (1 > 0) {
                Handler handler = new Handler(ss.accept());
                handler.start();
            }
        } catch (IOException e) {
            System.out.println("Произошла ошибка.");
        }

    }
}
//