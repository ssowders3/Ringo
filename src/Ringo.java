import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.*;

public class Ringo {


    public static int n;
    public static String flag;
    public static int PORT_NUMBER;
    public static String POC_NAME;
    public static int POC_PORT;

    public static final int PACKET_SIZE = 65535;
    public static boolean isFirst = false;
    public static int ringosOnline;

    public static int RINGOID;

    public static String IP_ADDR; //TODO: Change from Local


    public static long[] vector;
    public static long[][] matrix;

    public static DatagramSocket ds;

    public static Map<String, Integer> knownRingos;

    public static String cmdError = "There was an error in your command line arguments. Try again.";


    public static void main(String[] args) {
        //TODO: Create command line parsing arguments
        //Example Input: ringo <flag> <localport> <POC-name> <PoC-port> <N>
        /*
        <flag>: S if this Ringo peer is the Sender, R if it is the Receiver, and F if it is a Forwarder
        <local-port>: the UDP port number that this Ringo should use (at least for peer discovery)
        <PoC-name>: the host-name of the PoC for this Ringo. Set to 0 if this Ringo does not have a PoC.
        <PoC-port>: the UDP port number of the PoC for this Ringo. Set to 0 if this Ringo does not have a PoC.
        <N>: the total number of Ringos (when they are all active).
         */
        parseCmd(args);
        printStats();

        knownRingos = new HashMap<String, Integer>();

        try {
            ds = new DatagramSocket(PORT_NUMBER); //Opens UDP Socket at PORT_NUMBER;
            ds.setSoTimeout(120000); //setting time out to two minutes
        } catch (Exception e) {
            e.printStackTrace();
        }
        long roundTrip = 0;
        if (!isFirst) {
            roundTrip = getPingFromPOC(); //IF THERE EXISTS A POC, PING IT.
        } else {
            ringosOnline = 1;
            System.out.println("There is now 1 Ringo Online.");
        }
        initializeVector(roundTrip);
        initializeMatrix();

        Thread checkForPings = new Thread() {
            public void run() {
                for (int i = 0; i < n; i++) {
                    try {
                        byte[] date = new byte[1024];

                        DatagramPacket recieve =
                                new DatagramPacket(date, date.length, InetAddress.getLocalHost(), PORT_NUMBER);
                        ds.receive(recieve);

                        System.out.println("\n****************");
                        System.out.println("New Ringo Online!");

                        String packetData = new String(date);

                        System.out.println("Ping Packet Contained Info: " + new String(date));

                        StringTokenizer token = new StringTokenizer(packetData);

                        long timeStamp = Long.valueOf(token.nextToken());
                        int senderPort = Integer.valueOf(token.nextToken().trim());

                        Date now = new Date();
                        long ret = now.getTime();
                        long ONEWAYTRIP = ret;

                        ringosOnline += 1;

                        String sending = ("" + (ONEWAYTRIP * 2) + " " + ringosOnline);

                        byte[] RTT = sending.getBytes();

                        DatagramPacket send = new DatagramPacket(RTT, 15, InetAddress.getLocalHost(), senderPort);
                        //why is the second arg 15
                        ds.send(send);
                        System.out.println("Sent Packet! (" + sending + ")");
                        System.out.println("*****************");

                        exchangeKnownRingos();

                        System.out.print("Ringo Command: ");

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Socket timed out, closing socket");
                        ds.close();
                        System.exit(1);
                    }
                }

            }
        };

        checkForPings.start();

        while (true) {

            Scanner scan = new Scanner(System.in);

            System.out.print("Ringo command: ");
            StringTokenizer token = new StringTokenizer(scan.nextLine());

            String command = token.nextToken();

            if (command.equals("offline")) {
                int time = Integer.parseInt(token.nextToken());
                System.out.println("Going offline for " + time + " seconds.");
            } else if (command.equals("send")) {
                String filename = token.nextToken();

                byte[] buf = new byte[PACKET_SIZE];

                System.out.println("Sending " + filename);

            } else if (command.equals("show-matrix")) {
                System.out.println("Matrix = \n" + Arrays.deepToString(matrix));
            } else if (command.equals("show-ring")) {
                System.out.println("Ringo's Ring = \n" + Arrays.toString(vector));
            } else if (command.equals("disconnect")) {
                System.out.println("Exiting...");
                System.exit(-1);
            } else if (command.equals("show-ringos")) {
                System.out.println(knownRingos.toString());
            } else {
                System.out.println("Command Not Recognized. Valid commands include: \n");
                System.out.println("offline <seconds>");
                System.out.println("send <filename>");
                System.out.println("show-matrix");
                System.out.println("show-ring");
                System.out.println("show-ringos");
                System.out.println("disconnect \n");
            }
        }

        //TODO: Initialize Ringo Timer
        //TODO: Initialize RTT Vector.
    }

    public static void initializeVector(long roundTrip) {
        vector = new long[n];
        //dummy data;
        for (int i = 0; i < n; i++) {
            vector[i] = i;
        }
    }

    public static void initializeMatrix() {
        matrix = new long[n][n];
        matrix[0] = vector;
        matrix[1] = vector; //Replace with vector from Ringo 2...
    }

    public static long getPingFromPOC() {
        System.out.println("Obtaining Ping From the Point of Contact");
        Date now = new Date();
        long msSend = now.getTime();

        String ms = msSend + " " + PORT_NUMBER;
        byte[] buf = ms.getBytes();

        try {
            InetAddress poc = InetAddress.getByName(POC_NAME);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, poc, POC_PORT);
            int try_count = 0;
            while (true) {
                try {
                    if (try_count < 3) {
                        try_count = try_count + 1;
                        ds.send(packet);
                        DatagramPacket recieve = new DatagramPacket(new byte[15], 15);
                        //If POC is not initialized ds.receive will freeze until socket times out
                        //need to determine whether this is a big issue
                        ds.receive(recieve);
                        System.out.println("Packet recieved");
                        String ping = new String(recieve.getData());
                        StringTokenizer token = new StringTokenizer(ping);
                        long pingTime = Long.parseLong(token.nextToken());
                        System.out.println("Ping estimated at : " + pingTime + " ms.");
                        System.out.println("There are now " + token.nextToken() + " Ringos online.");
                        System.out.println("This Ringo has been assigned RINGOID: " + ringosOnline);
                        RINGOID = ringosOnline;
                        return pingTime;
                    }
                } catch (IOException e){
                    System.out.println("Sending failed trying again");
                    if (try_count == 3) {
                        System.out.println("Failed to send closing socket");
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void printStats() {
        System.out.println("******************************");
        if (flag.equals("S")) {
            System.out.println("*This Ringo was designated as a Sender.");
        } else if (flag.equals("R")) {
            System.out.println("*This Ringo was designated as a Reciever");
        } else if (flag.equals("F")) {
            System.out.println("*This Ringo was designated as a Forwarder.");
        } else {
            System.out.println(cmdError);
            System.exit(-1);
        }
        System.out.println("*This Ringo has a socket at port " + PORT_NUMBER + ".");
        System.out.println("*The total number of Ringos is " + n + ".");

        if (POC_NAME.equals("0")) {
            System.out.println("This is the first Ringo online!");
            isFirst = true;
        } else {
            System.out.println("*The host name of the Point of Contact is " + POC_NAME + " @ " + POC_PORT);

        }

        System.out.println("*Ringo successfully initialized.");
        System.out.println("****************************");
    }
    public static void exchangeKnownRingos(){
        //TODO: After the POC Ringo sends the Ping Packet back to the new Ringo, send the RTT to the new Ringo.
    }

    public void sendDummy() {
        //TODO: Send a dummy packet to other Ringos in the network to see if they are alive.
        //Might not be in Milestone 2.
    }

    public void recieveDummy() {
        //TODO: Ping other ringos in the network. Calculate RTT through timestamp.
    }

    public void floydWarshall() {
        //TODO: Construct shortest path among all Ringos in the network.
    }

    public void sendPacket(Ringo r) {
        //TODO: Send a packet to another Ringo in the network.
    }

    public static void parseCmd(String[] args) {
        if (args.length != 5) {
            System.out.println(cmdError);
            System.exit(-1);
        } else {
            try {
                flag = args[0];
                PORT_NUMBER = Integer.parseInt(args[1]);
                POC_NAME = args[2];
                POC_PORT = Integer.parseInt(args[3]);
                n = Integer.parseInt(args[4]);
            } catch (Exception e) {
                System.out.println(cmdError);
                System.exit(-1);
            }
        }
    }
}

