package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Broadcaster {

    protected StringBuffer log;
    protected NetworkManager networkManager;
    protected List<Host> hosts;

    private Thread recvThread;
    private long nb_msg;

    private AtomicLong nb_delivered;

    protected Broadcaster(List<Host> hosts, byte id, long nb_msg) {
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

        nb_delivered = new AtomicLong();
    }

    // function that will broadcast msg. Unique to each type of
    // broadcaster.
    abstract protected void broadcast(byte[] msg);

    // function that will handle msg. Unique to each type of
    // broadcaster. Some Broadcaster will return many messages upon reception of
    // a single one, e.g FIFO, hence the list
    abstract protected ArrayList<Message> handleMsg(byte[] msg, byte id);

    public void start() {

        // perfectlinks start
        networkManager.start();

        // recvThread
        recvThread.start();

        // sendThread
        for (long i = 1; i <= nb_msg; ++i) {
            broadcast(ByteBuffer.allocate(8).putLong(i).array());
            logBroadcast(i);
        }
        while (nb_delivered.get() < nb_msg)
            ;

    }

    public void stop() {
        networkManager.closeSocket();
    }

    public String getLog() {
        return log.toString();
    }

    protected void logBroadcast(long n) {
        log.append("b " + n + "\n");
    }

    protected void logDeliver(Message m) {
        log.append("d " + m.getId() + " " + m.getMsgId() + "\n");
        nb_delivered.incrementAndGet();
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

    // type for generic broadcasted message. Extract original sender ID and actual
    // message (msgId). The rest is Broadcaster specific
    //
    // Following above definition, two messages are equal iff they have the same id
    // and msgId
    public static class Message {
        private final long msgId;
        private final byte id;
        private final byte[] data;

        public Message(long msgId, byte id, byte[] data) {
            this.msgId = msgId;
            this.id = id;
            this.data = data;
        }

        public long getMsgId() {
            return msgId;
        }

        public byte getId() {
            return id;
        }

        public byte[] getData() {
            return data;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            result = prime * result + (int) (msgId ^ (msgId >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Message other = (Message) obj;
            if (id != other.id)
                return false;
            if (msgId != other.msgId)
                return false;
            return true;
        }

    }

}
