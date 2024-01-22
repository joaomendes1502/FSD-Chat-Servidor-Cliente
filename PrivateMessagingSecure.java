import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

public class PrivateMessagingSecure extends UnicastRemoteObject implements PrivateMessagingSecureInterface {

	private String userCliente;
    private boolean msgPriv;
    private HashMap<String, PublicKey> publicKeys;


    public PrivateMessagingSecure(String userCliente, boolean msgPriv, HashMap<String, PublicKey> publicKeys) throws RemoteException {
		super();
		this.userCliente = userCliente;
        this.msgPriv = msgPriv;
        this.publicKeys = publicKeys;
	}
	
	public String sendMessage(String clienteMsg, String msg) throws RemoteException {
        if(this.msgPriv == true){
            System.out.println("Mensagem privada de " + clienteMsg + ": " + msg); //E ESTA MENSAGEM
            return this.userCliente;
        }else{
            return null; 
        }
	}

    public String sendMessageSecure(String enviou, String msg, String assinatura){
        if(this.msgPriv == true){
            try{
                byte[] decodedBytes = Base64.getDecoder().decode(assinatura);
                Cipher cipher = Cipher.getInstance("RSA");

                PublicKey pk = publicKeys.get(enviou);
                System.out.println(pk);

                cipher.init(Cipher.DECRYPT_MODE, pk);
                byte[] decipheredDigest = cipher.doFinal(decodedBytes);

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(msg.getBytes());
                byte[] digest = md.digest();

                if(Arrays.equals(decipheredDigest, digest)){
                    System.out.println("Mensagem privada de " + enviou + ": " + msg);
                    System.out.println("Esta mensagem é segura. ");
                }else{
                    System.out.println(enviou + " tentou enviar uma mensagem que sofreu alterações!");
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
            return this.userCliente;
        }else{
            return null;
        }
    }

}
