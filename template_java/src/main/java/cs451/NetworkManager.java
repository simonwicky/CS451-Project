package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkManager {

    private HashMap<Integer, Host> idToHosts;
    private DatagramSocket socket;
    private int id;
    private HashMap<Integer, List<Message>> toBeAcked;
    private HashMap<Integer, Lock> locks;

    public NetworkManager(List<Host> hosts, int id) {
        this.idToHosts = new HashMap<>();
        this.id = id;
        this.toBeAcked = new HashMap<>();
        this.locks = new HashMap<>();

        try {
            // mapping setup, for fast sending and receiving
            for (Host host : hosts) {
                this.idToHosts.put(host.getId(), host);
                toBeAcked.put(host.getId(), new ArrayList<>());
                locks.put(host.getId(), new ReentrantLock());
            }

            // socket creation
            socket = new DatagramSocket(this.idToHosts.get(id).getPort(),
                    InetAddress.getByName(this.idToHosts.get(id).getIp()));
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Socket could not be created: " + e.getMessage());
        }
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    retransmit();
                }
            }
        }.start();
    }

    private void sendTo(int neighbourIndex, Message msg, boolean newMsg) {
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

    private void sendTo(int neighbourIndex, Message msg) {
        sendTo(neighbourIndex, msg, true);
    }

    public void sendTo(int neighbourIndex, byte[] data) {
        Message msg = new Message(id, data, 0);
        sendTo(neighbourIndex, msg);
    }

    private void retransmit() {
        System.out.println("Beginning retransmission");
        for (Map.Entry<Integer, List<Message>> entry : toBeAcked.entrySet()) {
            locks.get(entry.getKey()).lock();
            for (Message msg : entry.getValue()) {
                sendTo(entry.getKey(), msg, false);
                // System.out.println("Retransmitting lost message");
            }
            locks.get(entry.getKey()).unlock();
        }
    }

    // return a message
    public Message receive() throws IOException {
        byte[] buf = new byte[1000];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        Message msg = Message.fromBytes(packet.getData(), packet.getLength());
        if (msg.type == 1) {
            // debug
            // System.out.println("ACK from " + msg.id);

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
            sendTo(msg.id, new Message(id, msg.data, 1));
            return msg;
        }
    }

    public void closeSocket() {
        socket.close();
    }

    public static class Message {

        private int id;
        private byte[] data;
        // 0 is actual msg, 1 is ack
        private int type;

        public Message(int id, byte[] data, int type) {
            this.id = id;
            this.data = data;
            this.type = type;
        }

        private byte[] serialize() {
            byte[] id_serial = ByteBuffer.allocate(4).putInt(id).array();
            byte[] type_serial = ByteBuffer.allocate(4).putInt(type).array();
            byte[] out = new byte[id_serial.length + type_serial.length + data.length];
            System.arraycopy(id_serial, 0, out, 0, id_serial.length);
            System.arraycopy(type_serial, 0, out, id_serial.length, type_serial.length);
            System.arraycopy(data, 0, out, id_serial.length + type_serial.length, data.length);
            return out;
        }

        private static Message fromBytes(byte[] bytes, int nb_bytes) {
            if (nb_bytes < 8) {
                return null;
            }
            byte[] id_b = new byte[4];
            byte[] type_b = new byte[4];
            byte[] data = new byte[nb_bytes - 8];
            System.arraycopy(bytes, 0, id_b, 0, id_b.length);
            System.arraycopy(bytes, id_b.length, type_b, 0, type_b.length);
            System.arraycopy(bytes, id_b.length + type_b.length, data, 0, data.length);

            int id = ByteBuffer.wrap(id_b).getInt();
            int type = ByteBuffer.wrap(type_b).getInt();
            return new Message(id, data, type);

        }

        public int getId() {
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
