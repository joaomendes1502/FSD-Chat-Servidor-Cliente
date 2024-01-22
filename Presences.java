import java.util.*;
import java.util.concurrent.TimeUnit;


public class Presences {

    private static Hashtable<String, IPInfo> presencesUsers = new Hashtable<String, IPInfo>();
    private static int cont = 0;
    private int timeOut;

    public Presences(int timeOut){
        this.timeOut = timeOut;
    }

    public Vector<String> getPresences(String IPAddress, String userCliente) {

        Date actualTime = new Date();
        cont = cont+1;

        System.out.println("cont = "+ cont);

        //Assume-se que o IP e valido!!!!!
        synchronized(this) {
            if (presencesUsers.containsKey(IPAddress)) {
                IPInfo newIp = presencesUsers.get(IPAddress);
                newIp.setLastSeen(actualTime);
            }
            else {
                IPInfo newIP = new IPInfo(IPAddress, actualTime, userCliente);
                presencesUsers.put(IPAddress,newIP);
            }
        }
        return getOnlineUsersIPList();
    }

    public Vector<String> getOnlineUsersIPList(){
        Vector<String> result = new Vector<String>();
        for (Enumeration<IPInfo> e = presencesUsers.elements(); e.hasMoreElements(); ) {
            IPInfo element = e.nextElement();
            if (!element.timeOutPassed(timeOut)) {
                result.add(element.getIP());
            }
        }
        return result;
    }

    public Vector<String> getOnlineUsersList(){
        Vector<String> result = new Vector<String>();
        for (Enumeration<IPInfo> e = presencesUsers.elements(); e.hasMoreElements(); ) {
            IPInfo element = e.nextElement();
            if (!element.timeOutPassed(timeOut)) {
                result.add(element.getUserCliente());
            }
        }
        return result;
    }
}

class IPInfo {

    private String ip;
    private Date lastSeen;
    private String userCliente;

    public void setUserCliente(String userCliente) {
        this.userCliente = userCliente;
    }

    public String getUserCliente() {
        return userCliente;
    }

    public IPInfo(String ip, Date lastSeen, String userCliente) {
        this.ip = ip;
        this.lastSeen = lastSeen;
        this.userCliente = userCliente;
    }

    public String getIP () {
        return this.ip;
    }

    public void setLastSeen(Date lastSeen){
        this.lastSeen = lastSeen;
    }

    public boolean timeOutPassed(int timeout){
        boolean result = false;
        Date now = new Date();
        long diffInSeconds = (now.getTime()-this.lastSeen.getTime())/1000;
        if (diffInSeconds >= timeout)
            result = true;
        return result;
    }
}