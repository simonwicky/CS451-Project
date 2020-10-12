package cs451;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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
        forward.add(new Message(msg, id));
        BEbroadcast(prepareMsg(msg));
        logBroadcast(ByteBuffer.wrap(msg).getLong());
    }

    private void BEbroadcast(byte[] msg) {
        for (Host host : hosts) {
            networkManager.sendTo(host.getId(), msg);
        }
    }

    // Responsible for deliver event
    protected ArrayList<Broadcaster.Message> handleMsg(byte[] msg, int from) {

        Message m = reconstruct(msg);

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
                Broadcaster.Message deliveredMsg = new Broadcaster.Message(ByteBuffer.wrap(m.data).getLong(), m.id);
                ArrayList<Broadcaster.Message> list = new ArrayList<>();
                list.add(deliveredMsg);
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

    private Message reconstruct(byte[] msg) {

        byte[] origin_b = new byte[4];
        byte[] data = new byte[msg.length - origin_b.length];
        System.arraycopy(msg, 0, origin_b, 0, origin_b.length);
        System.arraycopy(msg, origin_b.length, data, 0, data.length);
        int origin = ByteBuffer.wrap(origin_b).getInt();
        return new Message(data, origin);
    }

    private class Message {
        private final byte[] data;
        private final int id;

        public Message(byte[] data, int id) {
            this.data = data;
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(data);
            result = prime * result + id;
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
            if (id != other.id)
                return false;
            return true;
        }

    }

}
