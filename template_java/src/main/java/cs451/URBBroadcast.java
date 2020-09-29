package cs451;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class URBBroadcast extends Broadcaster {


    private long nb_msg;

    private List<long[]> messages;
    private List<long[]> delivered;
    private List<long[]> acks;



    public URBBroadcast(List<Host> hosts, int id, String config) {
        super(hosts, id);

        this.messages = new ArrayList<>();
        this.delivered = new ArrayList<>();
        this.acks = new ArrayList<>();   

        // scanning config
        try {
            nb_msg = new Scanner(new File(config)).nextLong();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }




    // Function that handle sending
    protected void run() {
        for (long i = 0; i < nb_msg; ++i) {
            broadcast(ByteBuffer.allocate(8).putLong(i).array());
        }
    }

    private void broadcast(byte[] msg) {
        for (Host host : hosts) {
            networkManager.sendTo(host.getId(), msg);
        }
        logBroadcast(ByteBuffer.wrap(msg).getLong());
    }

    protected void handleMsg(byte[] msg, int id) {
        logDeliver(ByteBuffer.wrap(msg).getLong(), id);
    }




}
