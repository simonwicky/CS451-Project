package cs451;

import java.util.ArrayList;
import java.util.List;

public class FIFOBroadcast extends URBBroadcast {

    public FIFOBroadcast(List<Host> hosts, int id, long nb_msg) {
        super(hosts, id, nb_msg);
    }

    protected void broadcast(byte[] msg) {
        super.broadcast(msg);
    }

    protected ArrayList<Broadcaster.Message> handleMsg(byte[] msg, int from) {
        ArrayList<Broadcaster.Message> message = super.handleMsg(msg, from);
        if (message != null) {
            // do fifo magic
            return message;
        } else {
            return null;
        }
    }
}
