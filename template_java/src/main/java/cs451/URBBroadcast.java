package cs451;

import java.io.IOException;
import java.util.List;

public class URBBroadcast implements Broadcaster {

    private NetworkManager networkManager;
    private List<Host> hosts;
    private int nb_host;
    private int id;

    private List<Long> messages;
    private List<Long> delivered;
    private List<Long> acks;

    public URBBroadcast(List<Host> hosts, int id) {
        this.networkManager = new NetworkManager(hosts, id);
        this.hosts = hosts;
        this.nb_host = hosts.size();
        this.id = id;
    }

    public void start(int n) {
        try {
            for (Host host : hosts) {
                if (host.getId() != id) {
                    networkManager.sendTo(host.getId(), 1);
                }
            }
            long[] recv = networkManager.receive();
            System.out.println("Received : " + recv[1] + " from "+ recv[0]);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        networkManager.closeSocket();
    }
}
