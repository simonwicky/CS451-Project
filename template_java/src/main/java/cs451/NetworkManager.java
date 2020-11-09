package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkManager {

    private HashMap<Byte, Host> idToHosts;
    private DatagramSocket socket;
    private byte id;
    private HashMap<Byte, List<Message>> toBeAcked;
    private HashMap<Byte, Lock> locks;

    private Thread retransmitThread;

    public NetworkManager(List<Host> hosts, byte id) {
        this.idToHosts = new HashMap<>();
        this.id = id;
        this.toBeAcked = new HashMap<>();
        this.locks = new HashMap<>();

        try {
            // mapping setup, for fast sending and receiving
            for (Host host : hosts) {
                this.idToHosts.put((byte) host.getId(), host);
                toBeAcked.put((byte) host.getId(), new ArrayList<>());
                locks.put((byte) host.getId(), new ReentrantLock());
            }

            // socket creation
            socket = new DatagramSocket(this.idToHosts.get(id).getPort(),
                    InetAddress.getByName(this.idToHosts.get(id).getIp()));
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Socket could not be created: " + e.getMessage());
        }

        // thread for retransmission
        retransmitThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    retransmit();
                }
            }
        };
    }

    public void start() {
        retransmitThread.start();
    }

    // send a new message. newMsg to indicate if it needs to be added to the
    // toBeAcked list
    private void sendTo(byte neighbourIndex, Message msg, boolean newMsg) {
        try {
            byte[] buf = msg.serialize();

            // datagram setup
            DatagramPacket packet = new DatagramPacket(buf, buf.length,
                    InetAddress.getByName(idToHosts.get(neighbourIndex).getIp()),
                    idToHosts.get(neighbourIndex).getPort());

            // send datagram
            socket.send(packet);
        } catch (SocketException e) {
            System.out.println("Socket closed");
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (msg.type == 0 && newMsg) {
            locks.get(neighbourIndex).lock();
            toBeAcked.get(neighbourIndex).add(msg);
            locks.get(neighbourIndex).unlock();
        }
    }

    private void sendTo(byte neighbourIndex, Message msg) {
        sendTo(neighbourIndex, msg, true);
    }

    public void sendTo(byte neighbourIndex, byte[] data) {
        Message msg = new Message(id, data, (byte) 0);
        sendTo(neighbourIndex, msg, true);
    }

    private void retransmit() {
        for (Map.Entry<Byte, List<Message>> entry : toBeAcked.entrySet()) {
            locks.get(entry.getKey()).lock();
            for (Message msg : entry.getValue()) {
                sendTo(entry.getKey(), msg, false);
            }
            locks.get(entry.getKey()).unlock();
        }
    }

    // return a message
    // might return duplicate if ack is lost. Duplicate will be discarded by upper
    // layer.
    public Message receive() throws IOException {
        byte[] buf = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        Message msg = Message.fromBytes(packet.getData(), packet.getLength());
        if (msg.type == 1) {
            // handle ack
            for (Message m : new ArrayList<>(toBeAcked.get(msg.id))) {
                if (Arrays.equals(m.data, msg.data)) {
                    locks.get(msg.id).lock();
                    toBeAcked.get(msg.id).remove(msg);
                    locks.get(msg.id).unlock();
                }

            }

            // return next message
            return receive();
        } else {
            // ack the msg and pass it to Broadcaster
            sendTo(msg.id, new Message(id, msg.data, (byte) 1));
            return msg;
        }
    }

    public void closeSocket() {
        socket.close();
    }

    // Content agnostic message. Extract type and sender, the rest is content
    // handled by upper layer
    public static class Message {

        private byte id;
        private byte[] data;
        // 0 is actual msg, 1 is ack
        private byte type;

        public Message(byte id, byte[] data, byte type) {
            this.id = id;
            this.data = data;
            this.type = type;
        }

        private byte[] serialize() {

            byte[] out = new byte[2 + data.length];
            out[0] = id;
            out[1] = type;
            System.arraycopy(data, 0, out, 2, data.length);
            return out;
        }

        private static Message fromBytes(byte[] bytes, int nb_bytes) {
            if (nb_bytes < 8) {
                return null;
            }
            byte id;
            byte type;
            byte[] data = new byte[nb_bytes - 2];
            id = bytes[0];
            type = bytes[1];
            System.arraycopy(bytes, 2, data, 0, data.length);

            return new Message(id, data, type);

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
            result = prime * result + Arrays.hashCode(data);
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
            if (!Arrays.equals(data, other.data))
                return false;
            return true;
        }
    }
}
