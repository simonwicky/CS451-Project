package cs451;

import java.io.IOException;
import java.util.List;

public class URBBroadcast implements Broadcaster {

    private NetworkManager networkManager;
    private List<Host> hosts;
    private int id;

    public URBBroadcast(List<Host> hosts, int id) {
        this.networkManager = new NetworkManager(hosts, id);
        this.hosts = hosts;
        this.id = id;
    }

    public void start() {
        try {
            for (Host host : hosts) {
                if (host.getId() != id) {
                    networkManager.sendTo(host.getId(), 1l);
                }
            }
            System.out.println("Received : " + networkManager.receive());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        networkManager.closeSocket();
    }
}
