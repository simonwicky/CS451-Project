package cs451;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LCBroadcast extends URBBroadcast {

    private long[] vc;
    private Lock vcLock;
    private long lsn;
    private byte id;

    private HashMap<Byte, byte[]> dependencies;

    private List<Broadcaster.Message> pending;

    public LCBroadcast(List<Host> hosts, byte id, long nb_msg, String[] config) {
        super(hosts, id, nb_msg);
        // dependency setup
        this.dependencies = new HashMap<>();
        for (String s : config) {
            String[] s2 = s.split(" ");
            byte id_dep = (byte) Integer.parseInt(s2[0]);
            this.dependencies.put(id_dep, new byte[s2.length]);
            for (int i = 0; i < s2.length; i++) {
                this.dependencies.get(id_dep)[i] = (byte) Integer.parseInt(s2[i]);
            }
        }

        this.vc = new long[hosts.size() + 1];
        for (int i = 0; i < vc.length; i++) {
            vc[i] = 0;
        }

        this.pending = new ArrayList<>();
        this.vcLock = new ReentrantLock();
        this.lsn = 0;
        this.id = id;

    }

    protected void broadcast(byte[] msg) {
        super.broadcast(encodeVC(msg));
    }

    protected ArrayList<Broadcaster.Message> handleMsg(byte[] msg, byte from) {
        ArrayList<Broadcaster.Message> message = super.handleMsg(msg, from);
        ArrayList<Broadcaster.Message> deliveredMessage = new ArrayList<>();
        if (message != null) {
            boolean newMsg = false;

            for (Broadcaster.Message m : message) {
                if (canDeliver(m)) {
                    deliveredMessage.add(m);
                    newMsg = true;
                    vcLock.lock();
                    vc[m.getId()]++;
                    vcLock.unlock();
                } else {
                    pending.add(m);
                }
            }

            // check in pending
            // if no message is delivered from the fresh ones, no need to check, no changes
            // if a message from pending is delivered, mabye a previous one can be too,
            // hence redo the loop
            while (newMsg) {
                newMsg = false;
                for (Broadcaster.Message m : new ArrayList<>(pending)) {
                    if (canDeliver(m)) {
                        pending.remove(m);
                        deliveredMessage.add(m);
                        newMsg = true;
                        vcLock.lock();
                        vc[m.getId()]++;
                        vcLock.unlock();
                    }
                }

            }

            return deliveredMessage;
        } else {
            return null;
        }
    }

    private byte[] encodeVC(byte[] msg) {
        byte[] data = new byte[msg.length + 8 * vc.length];
        System.arraycopy(msg, 0, data, 0, msg.length);
        vcLock.lock();
        for (int i = 0; i < vc.length; i++) {
            byte[] vc_b = ByteBuffer.allocate(8).putLong(vc[i]).array();
            System.arraycopy(vc_b, 0, data, msg.length + 8 * i, vc_b.length);
        }
        vcLock.unlock();
        byte[] lsn_b = ByteBuffer.allocate(8).putLong(lsn).array();
        System.arraycopy(lsn_b, 0, data, msg.length + 8 * id, lsn_b.length);
        lsn += 1;
        return data;
    }

    private long[] decodeVC(byte[] data) {
        long[] vectorClock = new long[vc.length];
        for (int i = 0; i < vc.length; i++) {
            byte[] b = new byte[8];
            System.arraycopy(data, 8 * i, b, 0, 8);
            vectorClock[i] = ByteBuffer.wrap(b).getLong();
        }
        return vectorClock;
    }

    private boolean canDeliver(Broadcaster.Message m) {
        long[] vc_m = decodeVC(m.getData());
        byte[] dep = dependencies.get(m.getId());
        for (int d : dep) {
            if (!(vc[d] >= vc_m[d])) {
                return false;
            }
        }
        return true;
    }
}
