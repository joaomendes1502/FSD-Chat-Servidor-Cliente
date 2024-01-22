import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ServidorThread {

    private ServerSocket socketservidor;
    static int DEFAULT_PORT=2000;
    static int TIME_OUT=120;
    static Presences presencesCliente;

    public ServidorThread(ServerSocket socketservidor) {
        this.socketservidor = socketservidor; //construtor
    }

    public void iniciarServidor() {

        try {
            while (!socketservidor.isClosed()) {

                Socket socket = socketservidor.accept();  //esperar que um cliente se conecte, o comando accept() espera por novas solicitações de conexões.
                System.out.println("Um novo cliente juntou-se ao servidor!");

                HandlerCliente handlerCliente = new HandlerCliente(socket, presencesCliente);

                Thread thread = new Thread(handlerCliente);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Erro na execução do servidor: " + e);
        }

    }

    public void fecharSocketServidor(){
        try {
            if (socketservidor != null) {
                socketservidor.close();
            }
        } catch (IOException e) {
            e.printStackTrace(); //print de um erro
        }
    }

    public static void main(String[] args) throws IOException {

        ServerSocket socketServidor = null;
        int porta = DEFAULT_PORT;
        int timeout = TIME_OUT;

        Scanner scanner = new Scanner(System.in);
        System.out.println("1- Utilizar porta default");
        System.out.println("2- Inserir uma porta");
        int valorporta = scanner.nextInt();
        if (valorporta == 2){
            System.out.print("Insira uma porta: ");
            porta = scanner.nextInt();
        }
        System.out.println("1- Utilizar timeout padrão (120 segundos)");
        System.out.println("2- Inserir valor timeout");
        int valortimeout = scanner.nextInt();
        if (valortimeout  == 2){
            System.out.print("Insira valor timeout: ");
            timeout = scanner.nextInt();
        }
        presencesCliente = new Presences(timeout);



        try {
            socketServidor = new ServerSocket(porta);
            System.out.println("Servidor iniciado com sucesso no endereço " + socketServidor.getInetAddress() + ":" + socketServidor.getLocalPort());
        } catch (IOException e) {
            System.out.println("Erro ao iniciar o servidor: " + e);
            System.exit(1);
        }


        //o servidor esta a espera de clientes que façam conexão a esta porta
        ServidorThread server = new ServidorThread(socketServidor);
        server.iniciarServidor();


    }

}