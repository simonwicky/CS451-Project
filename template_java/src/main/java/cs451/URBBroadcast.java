package cs451;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class URBBroadcast extends Broadcaster {

    private final int id;
    private final int deliver_threshold;

    private List<Message> forward;
    private HashSet<Message> delivered;
    private HashMap<Message, Set<Integer>> acks;

    public URBBroadcast(List<Host> hosts, int id, long nb_msg) {
        super(hosts, id, nb_msg);

        this.forward = new ArrayList<>();
        this.delivered = new HashSet<>();
        this.acks = new HashMap<>();
        this.id = id;
        this.deliver_threshold = (int) Math.ceil(hosts.size() / 2.0);

    }

    // Responsible for broadcast event
    protected void broadcast(byte[] msg) {
        byte[] msg_b = prepareMsg(msg);
        Broadcaster.Message m = reconstruct(msg_b);
        forward.add(m);
        BEbroadcast(msg_b);
        logBroadcast(ByteBuffer.wrap(msg).getLong());
    }

    private void BEbroadcast(byte[] msg) {
        for (Host host : hosts) {
            networkManager.sendTo(host.getId(), msg);
        }
    }

    // Responsible for deliver event
    protected ArrayList<Broadcaster.Message> handleMsg(byte[] msg, int from) {

        Broadcaster.Message m = reconstruct(msg);

        if (!forward.contains(m)) {
            forward.add(m);
            BEbroadcast(msg);
        }

        if (acks.get(m) == null) {
            acks.put(m, new HashSet<>());
        }
        if (acks.get(m).add(from)) {
            // new ack. Since we deliver upon majority, we can check here if it can be
            // delivered.
            if (acks.get(m).size() >= deliver_threshold && forward.contains(m) && !delivered.add(m)) {
                ArrayList<Broadcaster.Message> list = new ArrayList<>();
                list.add(m);
                return list;
            }
        }

        return null;

    }

    private byte[] prepareMsg(byte[] msg) {

        byte[] id_b = ByteBuffer.allocate(4).putInt(id).array();
        byte[] data = new byte[id_b.length + msg.length];
        System.arraycopy(id_b, 0, data, 0, id_b.length);
        System.arraycopy(msg, 0, data, id_b.length, msg.length);
        return data;
    }

    private Broadcaster.Message reconstruct(byte[] msg) {

        byte[] origin_b = new byte[4];
        byte[] msgId_b = new byte[8];
        byte[] data = new byte[msg.length - origin_b.length - msgId_b.length];
        System.arraycopy(msg, 0, origin_b, 0, origin_b.length);
        System.arraycopy(msg, origin_b.length, msgId_b, 0, msgId_b.length);
        System.arraycopy(msg, origin_b.length + msgId_b.length, data, 0, data.length);
        int origin = ByteBuffer.wrap(origin_b).getInt();
        long msgId = ByteBuffer.wrap(msgId_b).getLong();
        return new Broadcaster.Message(msgId, origin, data);
    }

    // private class Message {
    // private final byte[] data;
    // private final int id;

    // public Message(byte[] data, int id) {
    // this.data = data;
    // this.id = id;
    // }

    // }

}
