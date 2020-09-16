package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

public class NetworkManager {

    private int id;
    private HashMap<Integer, Host> hosts;
    private DatagramSocket socket;

    public NetworkManager(List<Host> hosts, int id) {
        this.hosts = new HashMap<>();
        for (Host host : hosts){
            this.hosts.put(host.getId(), host);
        }
        try {
            socket = new DatagramSocket(this.hosts.get(id).getPort(), InetAddress.getByName(this.hosts.get(id).getIp()));
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Socket could not be created: " + e.getMessage());
        }
        this.id = id;
    }

    public void sendTo(int neighbourIndex, long msg) throws IOException {
        //long to byte
        byte[] buf = ByteBuffer.allocate(8).putLong(msg).array();

        //datagram setup
        DatagramPacket packet = new DatagramPacket(buf, 8, InetAddress.getByName(hosts.get(neighbourIndex).getIp()), hosts.get(neighbourIndex).getPort());

        //send datagram
        socket.send(packet);
    }

    public long receive() throws IOException{
        byte[] buf = new byte[8];
        DatagramPacket packet = new DatagramPacket(buf,8);
        socket.receive(packet);
        return ByteBuffer.wrap(buf).getLong();
    }

    public void closeSocket() {
        socket.close();
    }
}
