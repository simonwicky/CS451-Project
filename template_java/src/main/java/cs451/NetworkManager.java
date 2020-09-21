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

    private HashMap<Integer, Host> idToHosts;
    private HashMap<HostInfo, Integer> hostToId;
    private DatagramSocket socket;

    public NetworkManager(List<Host> hosts, int id) {
        this.idToHosts = new HashMap<>();
        this.hostToId = new HashMap<>();

        try {
            //mapping setup, for fast sending and receiving
            for (Host host : hosts){
                this.idToHosts.put(host.getId(), host);
                HostInfo info = new HostInfo(InetAddress.getByName(host.getIp()),host.getPort());
                this.hostToId.put(info, host.getId());
            }

            //socket creation
            socket = new DatagramSocket(this.idToHosts.get(id).getPort(), InetAddress.getByName(this.idToHosts.get(id).getIp()));
        } catch (SocketException | UnknownHostException e) {
            System.out.println("Socket could not be created: " + e.getMessage());
        }
    }

    public void sendTo(int neighbourIndex, long msg){
        try {
            //long to byte
            byte[] buf = ByteBuffer.allocate(8).putLong(msg).array();

            //datagram setup
            DatagramPacket packet = new DatagramPacket(buf, 8, InetAddress.getByName(idToHosts.get(neighbourIndex).getIp()), idToHosts.get(neighbourIndex).getPort());

            //send datagram
                socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //return an array of two longs, id of sender and message
    public long[] receive() throws IOException {
        byte[] buf = new byte[8];
        DatagramPacket packet = new DatagramPacket(buf,8);
        socket.receive(packet);
        int id = hostToId.get(new HostInfo(packet.getAddress(), packet.getPort()));
        return new long[] {id, ByteBuffer.wrap(buf).getLong()};
    }

    public void closeSocket() {
        socket.close();
    }

    //helper class for a fast lookup Host to Id
    private static class HostInfo {
        private InetAddress ip;
        private int port;

        public HostInfo(InetAddress ip, int port){
            this.ip = ip;
            this.port = port;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            HostInfo other = (HostInfo) obj;
            if (ip == null) {
                if (other.ip != null)
                    return false;
            } else if (!ip.equals(other.ip))
                return false;
            if (port != other.port)
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((ip == null) ? 0 : ip.hashCode());
            result = prime * result + port;
            return result;
        }
    }
}
