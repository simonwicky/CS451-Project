package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

public abstract class Broadcaster {

    protected StringBuffer log;
    protected NetworkManager networkManager;
    protected List<Host> hosts;

    private Thread recvThread;


    protected Broadcaster(List<Host> hosts, int id){
        this.networkManager = new NetworkManager(hosts, id);
        this.hosts = hosts;
        this.log = new StringBuffer();

        // setup receiving thread
        recvThread = (new Thread() {
            @Override
            public void run() {
                recv();
            }
        });
    }

    //function that will broadcast and handle msg. Unique to each type of broadcaster.
    abstract protected void run();
    abstract protected void handleMsg(long[] msg);

    public void start() {
        // recvThread
        recvThread.start();

        // sendThread
        run();

    }

    public void stop() {
        networkManager.closeSocket();
    }


    public String getLog() {
        return log.toString();
    }

    protected void logBroadcast(long n) {
        log.append("b ");
        log.append(n);
        log.append("\n");
        //Debug
        //System.out.println("b " + n);
    }

    protected void logDeliver(long[] msg) {
        log.append("d ");
        log.append(msg[0]);
        log.append(" ");
        log.append(msg[1]);
        log.append("\n");
        //Debug
        //System.out.println("d " + msg[0] + " " + msg[1]);
    }

    // Function that handle receiving, pass on to handleMsg for heart of broadcast algorithm
    private void recv() {
        while (true) {
            try {
                long[] recv = networkManager.receive();
                handleMsg(recv);
            } catch (SocketException e) {
                System.out.println("Socket closed");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    

}
