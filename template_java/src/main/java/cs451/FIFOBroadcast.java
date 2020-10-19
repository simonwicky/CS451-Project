package cs451;

import java.util.ArrayList;
import java.util.List;

public class FIFOBroadcast extends URBBroadcast {

    private long[] vc;
    private List<Broadcaster.Message> pending;

    public FIFOBroadcast(List<Host> hosts, int id, long nb_msg) {
        super(hosts, id, nb_msg);
        this.vc = new long[hosts.size() + 1];
        for (int i = 0; i < vc.length; i++) {
            vc[i] = 1;
        }
        this.pending = new ArrayList<>();
    }

    protected void broadcast(byte[] msg) {
        super.broadcast(msg);
    }

    protected ArrayList<Broadcaster.Message> handleMsg(byte[] msg, int from) {
        ArrayList<Broadcaster.Message> message = super.handleMsg(msg, from);
        ArrayList<Broadcaster.Message> deliveredMessage = new ArrayList<>();
        if (message != null) {
            boolean newMsg = false;
            // check for delivered message
            for (Broadcaster.Message m : message) {
                if (vc[m.getId()] == m.getMsgId()) {
                    deliveredMessage.add(m);
                    newMsg = true;
                    vc[m.getId()]++;
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
                    if (vc[m.getId()] == m.getMsgId()) {
                        deliveredMessage.add(m);
                        newMsg = true;
                        pending.remove(m);
                        vc[m.getId()]++;
                    }
                }

            }

            return deliveredMessage;
        } else {
            return null;
        }
    }
}
