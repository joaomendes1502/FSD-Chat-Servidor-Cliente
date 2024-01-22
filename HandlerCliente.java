import java.io.*;
import java.util.ArrayList;
import java.net.Socket;
import java.io.BufferedReader;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Vector;


public class HandlerCliente extends Thread{// as instancias vao ser executadas por diferentes threads

    public static ArrayList<HandlerCliente> handlerClientes = new ArrayList<>(); //quando um cliente manda a mensagem, vamos percorrer o array para enviar a mensagem os clientes logados, static pq queremos que pertença à classe e nao apenas aos objetos da classe

    static ArrayList<String> listaMensagens = new ArrayList<>();
    private Socket socket; //socket que nos vai ser passado do nosso Servidor
    private BufferedReader bufferedReader; //enviar dados, ler as mensagens enviadas pelo cliente
    private PrintWriter printWriter; //enviar dados,
    private String userCliente;
    private String msgPriv;
    Presences presencesClients;
    private String chavePublica;
    public HandlerCliente(Socket socket, Presences presencesClients){ //socket que nos foi passado pelo servidor
        try {
            this.socket = socket;
            this.presencesClients = presencesClients;
            this.printWriter = new PrintWriter(socket.getOutputStream()); //usamos esta stream para escrever
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //usamos esta stream pra ler
            this.userCliente = bufferedReader.readLine();
            handlerClientes.add(this); //adiciona o nosso cliente ao array, à lista de clientes
            printWriter.flush();
            clientesLogados();
            enviarMensagem("Servidor: " + userCliente + " entrou no chat!");
        } catch (IOException e) {
            fecharTudo(socket, bufferedReader, printWriter);

        }

    }

    public void updateAgentPost(String newMessage, String msgClient) {
        for (HandlerCliente u : handlerClientes) {
            if(!u.userCliente.equals(msgClient)){
                u.printWriter.println("AGENT_POST: " + newMessage );
                u.printWriter.flush();
            }
        }

    }

    public void updateSessao(String msgClient) {
        for (HandlerCliente user : handlerClientes) {
            for(HandlerCliente u : handlerClientes){
                    u.printWriter.println("SESSION_UPDATE: /*cliente*/" + user.userCliente + "-" + user.socket.getRemoteSocketAddress().toString() + "-" + String.valueOf(user.msgPriv) + "-" + user.chavePublica);
                }
        }

        String msgs = "";
        for (String s : listaMensagens) {
            msgs = msgs.concat(s + " | ");
        }
        for (HandlerCliente u : handlerClientes) {
            if(u.userCliente.equals(userCliente)){
                u.printWriter.println("SESSION_UPDATE: Ultimas Mensagens:  [" + msgs + "]" );
                u.printWriter.flush();
            }
        }

    }


    public void lerProtocolo(String mensagem) {
        String[] split = mensagem.split(":");
        String protocolo = split[0];

        int pos = mensagem.indexOf(':');
        String msg = mensagem.substring(pos + 2);
        String msgClient = msg.split(":")[0];
        if (protocolo.equalsIgnoreCase("AGENT_POST")) {
            if (listaMensagens.size() >= 10) {
                listaMensagens.remove(0);
            }
            listaMensagens.add(msg);
            atualizarPresenca();
            updateAgentPost(msg, msgClient);
            updateSessao("");


        } else if (protocolo.equalsIgnoreCase("SESSION_UPDATE_REQUEST")) {
            if(msg.length() >= 1){
                String[] split2 = msg.split(":");
                this.userCliente = split2[0];
                System.out.println(split2[1]);
                if(split2[1].equalsIgnoreCase("true")){
                    this.msgPriv = "pode receber";
                }else{
                    this.msgPriv = "não pode receber";
                }
                chavePublica = split2[2];


                if (!handlerClientes.contains(this)) {
                    handlerClientes.add(this);
                }
                atualizarPresenca();
                updateSessao(msgClient);
            }else{
                atualizarPresenca();
                updateSessao(msgClient);
            }
        }
    } //o objetivo do tcp é enviar um cabeçalho que é

    public void timeoutSessao() {
        // Retorna uma lista dos usernames que estão ativos e dá reset ao timeout deste
        // cliente
        Vector<String> usersOnline = presencesClients.getOnlineUsersList();

        Iterator<HandlerCliente> iterator = handlerClientes.iterator();
        while (iterator.hasNext()) {
            HandlerCliente u = iterator.next();
            if (!usersOnline.contains(u.userCliente)) {
                System.out.println(u.userCliente + " esta inativo");

                System.out.println("Este cliente vai se desconectar");
                u.printWriter.println("SESSION_TIMEOUT: A sua sessão vai terminar");
                u.printWriter.flush();
                try {
                    u.bufferedReader.close();
                    u.printWriter.flush();
                    u.printWriter.close();
                    u.socket.close();
                    iterator.remove();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void atualizarPresenca() {
        presencesClients.getPresences(socket.getRemoteSocketAddress().toString(), this.userCliente);
    }


    @Override   //tudo neste método run é o que está a correr numa thread diferente, se nao usarmos multithreading o nosso programa vai estar parado à espera de mensagens dos clientes
    public void run() {
        String mensagemCliente; //string usada pra reter a mensagem do cliente

        new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                timeoutSessao();
            }
        }, 1000 * 5, 1000 * 5);

        while(socket.isConnected()) { //vamos ''escutar'' mensagens dos clientes enquanto houver ainda conexao com o cliente
            try{
                mensagemCliente = bufferedReader.readLine(); //o programa espera aqui até recebermos uma mensagem do cliente, é por isso que fazemos isto em uma thread diferente
                lerProtocolo(mensagemCliente);

            } catch (IOException e) {
                fecharTudo(socket, bufferedReader, printWriter);
                break;
            }
        }
    }

    public void enviarMensagem(String mensagemEnviada) { //enviar mensagem para todos os clientes logados
        for (HandlerCliente handlerCliente : handlerClientes) {
            if(!handlerCliente.userCliente.equals(userCliente)) { //queremos enviar a mensagem para todos, exceto para o cliente que a enviou
                handlerCliente.printWriter.println(mensagemEnviada);
                handlerCliente.printWriter.flush(); //usamos o flush() para guardar todos os dados do buffer
            }
        }
    }

    public void removerHandlerCliente() {
        handlerClientes.remove(this); //remove o cliente do chat quando o mesmo sai do programa
        enviarMensagem(userCliente + "saiu do chat!");
    }

    public void fecharTudo (Socket socket, BufferedReader bufferedReader, PrintWriter printWriter) {
        removerHandlerCliente();

        try{
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (printWriter != null) {
                printWriter.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void clientesLogados(){
        this.printWriter.println("Utilizadores Online: ");
        this.printWriter.flush();
        for(HandlerCliente clientesLogados : handlerClientes){ //percorre a lista dos clientes que estabeleceram conexao
            this.printWriter.println(clientesLogados.userCliente); //em cada linha mostra os clientes logados
            this.printWriter.flush();
        }
    }

}
