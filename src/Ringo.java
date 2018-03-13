import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Created by Katie on 2/27/18.
 */
public class Ringo {


    public static int n;
    public static String flag;
    public static int PORT_NUMBER;
    public static String POC_NAME;

    public static int POC_PORT;

    public static int PING_PORT;

    public static final int PACKET_SIZE = 65535;
    public static boolean isFirst = false;
    public static int ringosOnline = 0;

    public static int RINGOID;

    public static String IP_ADDR; //TODO: Change from Local


    public static int[] vector;
    public static int[][] matrix;

    public static DatagramSocket ds;
    public static DatagramSocket ping;

    public static Map<Integer, Integer> knownRingos;

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
        PING_PORT = PORT_NUMBER + 1;
        printStats();

        knownRingos = new HashMap<Integer, Integer>();

        try {
            ds = new DatagramSocket(PORT_NUMBER);
            ping = new DatagramSocket(PING_PORT);//Opens UDP Socket at PORT_NUMBER;
        } catch (Exception e) {
            e.printStackTrace();
        }



        if (!isFirst) {
            getPingFromPOC(); //IF THERE EXISTS A POC, PING IT.
        } else {
            ringosOnline = 1;
            RINGOID = ringosOnline;
            System.out.println("There is now 1 Ringo Online.");
        }

        initializeVector();
        initializeMatrix();

        Thread checkForPings = new Thread() {
            public void run() {
                while (true) {
                    try {
                        byte[] date = new byte[1024];

                        DatagramPacket recieve =
                                new DatagramPacket(date, date.length, InetAddress.getLocalHost(), PING_PORT);
                        ping.receive(recieve);

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
                        ping.send(send);
                        System.out.println("Sent Packet! (" + sending + ")");
                        System.out.println("*****************");

                        knownRingos.put(ringosOnline, senderPort);
                        System.out.println("known ringos + " + knownRingos);

                        if (ringosOnline >= 1) {
                            System.out.println("SENDER PORT " + senderPort);
                            exchangeKnownRingos(InetAddress.getLocalHost(), senderPort);
                        }


                        System.out.print("Ringo Command: ");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        checkForPings.start();

        while (true) {

            Scanner scan = new Scanner(System.in);
            System.out.println("known ringos + "+  knownRingos);

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

    public static void initializeVector() {
        vector = new int[n];
        //dummy data;
        for (int i = 0; i < n; i++) {
            vector[i] = i;
        }
    }

    public static void initializeMatrix() {
        matrix = new int[n][n];
        matrix[0] = vector;
        matrix[1] = vector; //Replace with vector from Ringo 2...
    }

    public static void getPingFromPOC() {
        System.out.println("Obtaining Ping From the Point of Contact");
        Date now = new Date();
        long msSend = now.getTime();

        String ms = msSend + " " + PING_PORT;
        byte[] buf = ms.getBytes();

        try {
            InetAddress poc = InetAddress.getByName(POC_NAME);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, poc, PING_PORT);
            try {
                ping.send(packet);
                DatagramPacket recieve = new DatagramPacket(new byte[15], 15);
                ping.receive(recieve);

                String ping = new String(recieve.getData());
                StringTokenizer token = new StringTokenizer(ping);
                long pingTime = Long.parseLong(token.nextToken());
                System.out.println("Ping estimated at : " + pingTime + " ms.");
                ringosOnline = Integer.parseInt(token.nextToken());
                System.out.println("There are now " + ringosOnline + " Ringos online.");
                System.out.println("This Ringo has been assigned RINGOID: " + ringosOnline);
                RINGOID = ringosOnline;
                knownRingos.put(ringosOnline - 1, POC_PORT);
            } catch (IOException e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        System.out.println("*This Ringo has a ping port at " + PING_PORT);
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
    public static void exchangeKnownRingos(InetAddress ipAddr, int portNum){
        //TODO: After the POC Ringo sends the Ping Packet back to the new Ringo, send the RTT to the new Ringo.
        System.out.println("Exchanging Ringo @" + ipAddr.getHostName() + ": " + portNum);
        try {
            System.out.println("my known ringos " + knownRingos);
                int i = 0;
                for (Map.Entry<Integer, Integer> entry : knownRingos.entrySet()) {
                    System.out.println("ITERATION : " + i);
                    Integer key = entry.getKey();
                    Integer value = entry.getValue();
                    String keyValue = ("" + key + " " + value);
                    byte[] ringMap = keyValue.getBytes();
                    DatagramPacket mapPacket =
                            new DatagramPacket(ringMap, ringMap.length, ipAddr, portNum);
                    System.out.println("Sending to ip " + ipAddr + " and port " + portNum);
                    ds.send(mapPacket);
                    i++;
                    if (i==n) {
                        break;
                    }
                }

                DatagramPacket receive = new DatagramPacket(new byte[20], 20);
                ds.receive(receive);

                System.out.println("RECIEVED PACKET");
                System.out.println("PACKET CONTAINED: " + receive.getData().toString());

                String mapData = new String(receive.getData());
                StringTokenizer token = new StringTokenizer(mapData);
                int ringoID = Integer.valueOf(token.nextToken());
                System.out.println("ringoId is " + ringoID);
                int ringoNodePort = Integer.valueOf(token.nextToken().trim());
                System.out.println("ringo node port number is " + ringoNodePort);
                knownRingos.put(ringoID, ringoNodePort);
                System.out.println("known ringos after exchange " + knownRingos);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

