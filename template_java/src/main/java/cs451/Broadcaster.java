package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class Broadcaster {

    protected StringBuffer log;
    protected NetworkManager networkManager;
    protected List<Host> hosts;

    private Thread recvThread;
    private long nb_msg;

    protected Broadcaster(List<Host> hosts, int id, long nb_msg) {
        this.networkManager = new NetworkManager(hosts, id);
        this.hosts = hosts;
        this.log = new StringBuffer();
        this.nb_msg = nb_msg;

        // setup receiving thread
        recvThread = (new Thread() {
            @Override
            public void run() {
                recv();
            }
        });
    }

    // function that will broadcast and handle msg. Unique to each type of
    // broadcaster.
    abstract protected void broadcast(byte[] msg);

    abstract protected ArrayList<Message> handleMsg(byte[] msg, int id);

    public void start() {
        // recvThread
        recvThread.start();

        // sendThread
        for (long i = 1; i <= nb_msg; ++i) {
            broadcast(ByteBuffer.allocate(8).putLong(i).array());

            // TOCLEAN : DEBUG ONLY
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // TOCLEAN
        }

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
        // Debug
        // System.out.println("b " + n);
    }

    protected void logDeliver(Message m) {
        log.append("d ");
        log.append(m.getId());
        log.append(" ");
        log.append(m.getData());
        log.append("\n");
        // Debug
        // System.out.println("d " + id + " " + msg);
    }

    // Function that handle receiving, pass on to handleMsg for heart of broadcast
    // algorithm
    private void recv() {
        while (true) {
            try {
                NetworkManager.Message recv = networkManager.receive();
                ArrayList<Message> msgs = handleMsg(recv.getData(), recv.getId());
                if (msgs != null) {
                    for (Message m : msgs) {
                        logDeliver(m);
                    }
                }
            } catch (SocketException e) {
                System.out.println("Socket closed");
                return;
            } catch (IOException e) {
                System.out.println("IOException");
                e.printStackTrace();
            }
        }
    }

    public static class Message {
        private final long data;
        private final int id;

        public Message(long data, int id) {
            this.data = data;
            this.id = id;
        }

        public long getData() {
            return data;
        }

        public int getId() {
            return id;
        }

    }

}
