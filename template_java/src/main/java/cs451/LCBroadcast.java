package cs451;

import java.util.ArrayList;
import java.util.List;

public class LCBroadcast extends URBBroadcast {

    public LCBroadcast(List<Host> hosts, int id, long nb_msg, String[] config) {
        super(hosts, id, nb_msg);

        // handle cofing here
    }

    protected void broadcast(byte[] msg) {
        super.broadcast(msg);
    }

    protected ArrayList<Broadcaster.Message> handleMsg(byte[] msg, int from) {
        ArrayList<Broadcaster.Message> message = super.handleMsg(msg, from);
        if (message != null) {
            // do LC magic
            return message;
        } else {
            return null;
        }
    }
}
