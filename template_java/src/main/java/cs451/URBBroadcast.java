package cs451;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class URBBroadcast extends Broadcaster {

    private final byte id;
    private final int deliver_threshold;

    private HashSet<Message> forward;
    private HashSet<Message> delivered;
    private HashMap<Message, Set<Byte>> acks;

    public URBBroadcast(List<Host> hosts, byte id, long nb_msg) {
        super(hosts, id, nb_msg);

        this.forward = new HashSet<>();
        this.delivered = new HashSet<>();
        this.acks = new HashMap<>();
        this.id = id;
        this.deliver_threshold = (int) Math.ceil(hosts.size() / 2.0);

    }

    // Responsible for broadcast event
    protected void broadcast(byte[] msg) {
        byte[] msg_b = prepareMsg(msg);
        // here, handle message will broadcast to everyone but itself, and handle it as
        // it if has been recevied
        handleMsg(msg_b, id);
    }

    // broadcast to everyone but itself
    private void BEbroadcast(byte[] msg) {
        for (Host host : hosts) {
            if (host.getId() != id) {
                networkManager.sendTo((byte) host.getId(), msg);
            }
        }
    }

    // Responsible for deliver event
    protected ArrayList<Broadcaster.Message> handleMsg(byte[] msg, byte from) {

        Broadcaster.Message m = reconstruct(msg);

        // if we have already deliver, we also forwarded it. So we can simply return
        // here
        if (delivered.contains(m)) {
            return null;
        }

        // forward it if we don't have already
        if (forward.add(m)) {
            BEbroadcast(msg);
        }

        // create the ack set if it doesn't exist'
        if (acks.get(m) == null) {
            acks.put(m, new HashSet<>());
        }
        // check if it as an ack from someone else
        if (acks.get(m).add(from)) {
            // new ack. Since we deliver upon majority, we can check here if it can be
            // delivered.
            // m is guaranteed to be in forward, no need to check
            // m is guaranteed to be undelivered if we got here
            if (acks.get(m).size() >= deliver_threshold) {
                delivered.add(m);
                // clean up acks and forward for space.
                forward.remove(m);
                acks.remove(m);
                ArrayList<Broadcaster.Message> list = new ArrayList<>();
                list.add(m);
                return list;
            }
        }

        return null;

    }

    // add its id before the message. URB semantic
    private byte[] prepareMsg(byte[] msg) {

        byte[] data = new byte[1 + msg.length];
        data[0] = id;
        System.arraycopy(msg, 0, data, 1, msg.length);
        return data;
    }

    private Broadcaster.Message reconstruct(byte[] msg) {

        byte origin = msg[0];
        byte[] msgId_b = new byte[8];
        byte[] data = new byte[msg.length - 1 - msgId_b.length];
        System.arraycopy(msg, 1, msgId_b, 0, msgId_b.length);
        System.arraycopy(msg, 1 + msgId_b.length, data, 0, data.length);
        long msgId = ByteBuffer.wrap(msgId_b).getLong();
        return new Broadcaster.Message(msgId, origin, data);
    }

}
