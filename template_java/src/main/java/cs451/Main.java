package cs451;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Main {

    private static Broadcaster broadcaster;
    private static Path path;

    private static void handleSignal() {
        // immediately stop network packet processing
        broadcaster.stop();
        System.out.println("Immediately stopping network packet processing.");

        // write/flush output file if necessary
        System.out.println("Writing output.");
        String log = broadcaster.getLog();
        System.out.println(log);
        try {
            Files.writeString(path, log, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                handleSignal();
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        initSignalHandlers();

        // example
        long pid = ProcessHandle.current().pid();
        System.out.println("My PID is " + pid + ".");
        System.out
                .println("Use 'kill -SIGINT " + pid + " ' or 'kill -SIGTERM " + pid + " ' to stop processing packets.");

        System.out.println("My id is " + parser.myId() + ".");
        System.out.println("List of hosts is:");
        for (Host host : parser.hosts()) {
            System.out.println(host.getId() + ", " + host.getIp() + ", " + host.getPort());
        }

        System.out.println("Barrier: " + parser.barrierIp() + ":" + parser.barrierPort());
        System.out.println("Signal: " + parser.signalIp() + ":" + parser.signalPort());
        System.out.println("Output: " + parser.output());
        // if config is defined; always check before parser.config()
        if (parser.hasConfig()) {
            System.out.println("Config: " + parser.config());
        }

        Coordinator coordinator = new Coordinator(parser.myId(), parser.barrierIp(), parser.barrierPort(),
                parser.signalIp(), parser.signalPort());

        // scanning config
        long nb_msg = 0;
        ArrayList<String> config = new ArrayList<>();
        try {
            BufferedReader s = new BufferedReader(new FileReader(new File(parser.config())));
            nb_msg = Long.parseLong(s.readLine());
            String line;
            while ((line = s.readLine()) != null) {
                config.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] configArray = new String[config.size()];
        configArray = config.toArray(configArray);

        // Brodcast setup
        //broadcaster = new LCBroadcast(parser.hosts(), parser.myId(), nb_msg, configArray);
        broadcaster = new FIFOBroadcast(parser.hosts(), parser.myId(), nb_msg);
        // logfile setup
        path = Paths.get(parser.output());

        System.out.println("Waiting for all processes for finish initialization");
        coordinator.waitOnBarrier();

        System.out.println("Broadcasting messages...");
        broadcaster.start();

        System.out.println("Signaling end of broadcasting messages");
        coordinator.finishedBroadcasting();

        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
