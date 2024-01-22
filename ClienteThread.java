import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Scanner;

public class ClienteThread {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String userCliente;
    static int DEFAULT_PORT=2000;
    int tempoSessionUpdate = 30000;
    private static String SERVICE_NAME = "/PrivateMessaging";
    private boolean msgPriv;
    private static PublicKey chavePublica;
    private static PrivateKey chavePrivada;
    private static HashMap<String, PublicKey> publicKeys = new HashMap<>();
    private static HashMap<String, String> userIPClientes = new HashMap<>();

    public ClienteThread(Socket socket, String userCliente, boolean msgPriv) {
        try {
            this.socket = socket;
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.userCliente = userCliente;
            this.msgPriv = msgPriv;
        } catch (IOException e) {
            fecharTudo(socket, bufferedReader, bufferedWriter);
        }
    }

    public void enviarMensagem() { //enviar mensagens enquanto o cliente ainda tem conexao com o servidor
        try {
            bufferedWriter.write(userCliente);
            bufferedWriter.newLine();
            bufferedWriter.flush();

            String encodedString = Base64.getEncoder().encodeToString(chavePublica.getEncoded());

            bufferedWriter.write("SESSION_UPDATE_REQUEST: " + userCliente + ":" +  msgPriv + ":" + encodedString); // iniciar sessão
            bufferedWriter.newLine();
            bufferedWriter.flush();

            LoopSessionUpdate loopSessionUpdate = new LoopSessionUpdate(userCliente,bufferedWriter,tempoSessionUpdate);
            loopSessionUpdate.start();

            Scanner scan = new Scanner(System.in);
            while (socket.isConnected()) {
                String mensagemEnviada = scan.nextLine();
                if(mensagemEnviada.equalsIgnoreCase("/msg")){
                    System.out.print("Insira o username do utilizador que quer enviar a mensagem: ");
                    Scanner scanner = new Scanner(System.in);
                    String ipDestino = userIPClientes.get(scanner.nextLine());
                    System.out.print("Insira a mensagem: ");
                    String msgEnviada = scanner.nextLine();

                    PrivateMessagingSecureInterface ref = (PrivateMessagingSecureInterface) LocateRegistry.getRegistry(ipDestino).lookup(SERVICE_NAME);
                    String recebeu = ref.sendMessage(userCliente, msgEnviada);
                    if(recebeu == null){
                        System.out.println("Este utilizador não quer receber mensagens");
                    }else{
                        System.out.println("Mensagem enviada a " + recebeu);
                    }

                }else if(mensagemEnviada.equalsIgnoreCase("/msgSegura")) {
                    System.out.print("Insira o username do utilizador que quer enviar a mensagem: ");
                    Scanner scanner = new Scanner(System.in);
                    String ipDestino = userIPClientes.get(scanner.nextLine());
                    System.out.print("Insira a mensagem: ");
                    String msgEnviada = scanner.nextLine();

                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(msgEnviada.getBytes());
                    byte[] digest = md.digest();

                    Cipher cipher = Cipher.getInstance("RSA");
                    cipher.init(Cipher.ENCRYPT_MODE, chavePrivada);
                    cipher.update(digest);

                    byte[] cipherText = cipher.doFinal();


                    String msgBase = Base64.getEncoder().encodeToString(cipherText);


                    PrivateMessagingSecureInterface ref = (PrivateMessagingSecureInterface) LocateRegistry.getRegistry(ipDestino).lookup(SERVICE_NAME);

                    String recebeu = ref.sendMessageSecure(userCliente, msgEnviada, msgBase);
                    if (recebeu == null) {
                        System.out.println("Este utilizador não quer receber mensagens");
                    } else {
                        System.out.println("Mensagem segura enviada a " + recebeu);
                    }
                }else {
                    bufferedWriter.write("AGENT_POST: " + userCliente + ":" + mensagemEnviada); //a mensagem enviada para os outros aparecerá assim
                    bufferedWriter.newLine();
                    bufferedWriter.flush();
                }

            }
        } catch (IOException e) {
            fecharTudo(socket, bufferedReader, bufferedWriter);
        } catch (NotBoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public void mensagensClientes() { //new thread porque vai ser uma block operation, não vamos estar à espera de mensagens para enviar uma
        new Thread(new Runnable() {
            @Override
            public void run() {
                String mensagensDoGrupo;

                while (socket.isConnected()) {
                    try {
                        mensagensDoGrupo = bufferedReader.readLine();
                        String[] split = mensagensDoGrupo.split(":");
                        String protocolo = split[0];

                        int pos = mensagensDoGrupo.indexOf(':');
                        String mensagem = mensagensDoGrupo.substring(pos + 2);
                        if (protocolo.equalsIgnoreCase("SESSION_UPDATE")) {
                            if(mensagem.startsWith("/*cliente*/")){
                                String info = mensagem.substring(11);
                                String[] split_info = info.split("-");

                                //System.out.println(split_info[0] + " - " + split_info[1] + " - "+ split_info[2] + " - "+ split_info[3]);
                                if(!userIPClientes.keySet().contains(split_info[0])){
                                    int p = split_info[1].indexOf(":");
                                    userIPClientes.put(split_info[0], split_info[1].substring(1, p));
                                }
                                if(!publicKeys.keySet().contains(split_info[0])){
                                    //Temos que converter de Base64 para PublicKey
                                    byte[] decodedBytes = Base64.getDecoder().decode(split_info[3]);
                                    KeyFactory factory = KeyFactory.getInstance("RSA","SunRsaSign");
                                    PublicKey public_key = (PublicKey) factory.generatePublic(new X509EncodedKeySpec(decodedBytes));
                                    publicKeys.put(split_info[0], public_key);
                                }
                                System.out.println(split_info[0] + " - " + split_info[1] + " - "+ split_info[2]);
                            }
                            else{
                                System.out.println(mensagem);

                            }
                        } else if (protocolo.equalsIgnoreCase("SESSION_TIMEOUT")) {
                            System.out.println(mensagem);

                        } else if (protocolo.equalsIgnoreCase("AGENT_POST")) {
                            System.out.println(mensagem);

                        }else if (protocolo.equalsIgnoreCase("Servidor")){
                            System.out.println(mensagensDoGrupo);
                        }
                    } catch (IOException e) {
                        fecharTudo(socket, bufferedReader, bufferedWriter);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    } catch (InvalidKeySpecException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchProviderException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        }).start();  //basicamente vamos estar à espera das mensagens dos outros clientes, mas podemos escrever mensagens na mesma

    }

    public void fecharTudo(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {

        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTempoSessionUpdate(int tempoSessionUpdate) {
        this.tempoSessionUpdate = tempoSessionUpdate;
    }

    public int getTempoSessionUpdate() {
        return tempoSessionUpdate;
    }

    public static void main(String[] args) throws IOException {



        int porta = DEFAULT_PORT;

        Scanner scan = new Scanner(System.in);
        System.out.print("Insira o seu username: ");
        String userCliente = scan.nextLine();
        Scanner scanner = new Scanner(System.in);
        System.out.println("1- Utilizar porta default");
        System.out.println("2- Inserir uma porta");
        int valorporta = scanner.nextInt();
        if (valorporta == 2){
            System.out.print("Insira uma porta: ");
            porta = scanner.nextInt();
        }
        System.out.println("1- Utilizar Session Update padrão (30 segundos)");
        System.out.println("2- Inserir valor de Session Update");
        int valor = scanner.nextInt();
        int t = 30000;
        if (valor == 2){
            System.out.print("Insira um valor: ");
            t = scanner.nextInt() * 1000;
        }
        System.out.println("1- Receber mensagens privadas");
        System.out.println("2- Não receber mensagens privadas");
        int pioledo = scanner.nextInt();
        boolean msgPriv;
        if(pioledo == 1){
            msgPriv = true;
        }else{
            msgPriv = false;
        }
        // Criar registo se for com clientes em pcs diferentes

            try {
                LocateRegistry.createRegistry(1099);
                PrivateMessagingSecureInterface ref = new PrivateMessagingSecure(userCliente, msgPriv, publicKeys);
                LocateRegistry.getRegistry("127.0.0.1", 1099).rebind(SERVICE_NAME, ref);
            } catch (RemoteException e) {
                System.out.println("erro no registo");
            }

        //Criar registo se for com clientes no mesmo pc
        //Usamos o username para guardar o registo em vez do IP uma vez que no localhost o ip é o mesmo para todos
        /*try {
            LocateRegistry.createRegistry(1099);
            //Criamos a referencia remota do cliente
            PrivateMessagingInterface ref = new PrivateMessaging(userCliente, msgPriv);
            //Guardamos no registo 127.0.0.1:1099 o registo com o seu username e referencia
            LocateRegistry.getRegistry("127.0.0.1", 1099).rebind(userCliente, ref);
            System.out.println("Registo criado!");
        } catch (RemoteException e) { //Se ja existir o registo, coloca um novo com outro username
            PrivateMessagingInterface ref = null;
            try {
                ref = new PrivateMessaging(userCliente, msgPriv);
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
            try {
                LocateRegistry.getRegistry("127.0.0.1", 1099).rebind(userCliente, ref);
                System.out.println("Registo iniciado!");
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }*/

        //Geramos as chaves publicas e privadas
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(1024);
            KeyPair pair = keyPairGen.generateKeyPair();
            chavePublica = pair.getPublic();
            chavePrivada = pair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }


        System.out.println("Bem vindo " + userCliente + "!");
        Socket socket = new Socket("localhost", porta); //quem for cliente mete o ip do servidor, o servidor tbm
        ClienteThread cliente = new ClienteThread(socket, userCliente, msgPriv);
        cliente.setTempoSessionUpdate(t);
        cliente.mensagensClientes();
        cliente.enviarMensagem();

    }
}