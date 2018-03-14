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

    public static final int PACKET_SIZE = 65535;
    public static boolean isFirst = false;
    public static int ringosOnline = 0;
    public static int initialRingoCount = 0;

    public static int RINGOID;
    public static boolean calculatedPing;
    public static int num_iters = 0;

    public static String IP_ADDR; //TODO: Change from Local

    public static Thread checkForPackets;



    public static int[] vector;
    public static int[][] matrix;

    public static DatagramSocket ds;



    public static String cmdError = "There was an error in your command line arguments. Try again.";

    static class ringoAddr {
        public String IP_ADDR;
        public int ID;

        public ringoAddr(String ip, int p) {
            IP_ADDR = ip;
            ID = p;
        }

        public int getID() {
            return ID;
        }

        public String getIP() {
            return IP_ADDR;
        }

        public String toString() {
            return "RINGO: "  + IP_ADDR +  " ,ID: " + ID + " , PORT NUMBER";
        }
    }

    public static Map<ringoAddr, Integer> knownRingos;

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

        knownRingos = new HashMap<ringoAddr, Integer>();
        calculatedPing = false;

        try {
            ds = new DatagramSocket(PORT_NUMBER); //Opens UDP Socket at PORT_NUMBER;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!args[2].equals("0")) {
            sendPingPacket(); //IF THERE EXISTS A POC, PING IT.
        } else {
            ringosOnline = 1;
            initialRingoCount = 2;
            RINGOID = ringosOnline;
            knownRingos.put(new ringoAddr("127.0.0.1", 0), PORT_NUMBER);
        }

        initializeVector();
        initializeMatrix();

        checkForPackets = new Thread() {
            public void run() {
                while (true) {
                    try {
                        byte[] packetInfo = new byte[1024];
                        DatagramPacket recieve = //Recieves Ping request at this Ringo's Port number.
                                new DatagramPacket(packetInfo, packetInfo.length, InetAddress.getLocalHost(), PORT_NUMBER);
                        ds.receive(recieve);

                        String packetData = new String(packetInfo);
                        StringTokenizer token = new StringTokenizer(packetData);

                        String FLAG = token.nextToken().trim();
                        String firstField = token.nextToken().trim();
                        String secondField = token.nextToken().trim();
                        if (FLAG.equals("P")) {
                            System.out.println("\n\nA new Ringo has requested to ping you.");
                            recievePing(firstField, secondField);
                        } else if (FLAG.equals("E")) {
                            String thirdField = token.nextToken();
                            acceptRingos(firstField, thirdField, secondField);
                            System.out.println("A Ringo is exchanging information with you...");

                        } else if (FLAG.equals("PR")) {
                            int ping = Integer.parseInt(firstField);

                            ringosOnline = Integer.parseInt(secondField);
                            System.out.println("There are " + ringosOnline + " Ringos online.");
                            System.out.println("\n\nYou have recieved a ping reply.");
                            System.out.println("Ping was estimated at " + Math.abs(ping) + "ms.");
                            /*

                              0  1  2
                            0 0
                            1    0
                            2       0
                             */
                            //System.out.println("There are now " + secondField + " Ringos online.");
                        } else if (FLAG.equals("PM")) {//PING MATRIX{
                            String timeStamp = firstField;
                            String ipOfSender = secondField;
                            String port = token.nextToken();
                            System.out.println("timestamp : " + timeStamp + " ipOfSender : " + ipOfSender + " port " + port);
                            replyToMatrix(timeStamp, ipOfSender, port);
                        } else if (FLAG.equals("MR")) {
                            //populate matrix
                            String timeStamp = firstField;
                            String ipOfSender = secondField;
                            String port = token.nextToken();
                            System.out.println("timestamp : " + timeStamp + " ipOfSender : " + ipOfSender + " port " + port);
                            System.out.println("REPLY timestamp : " + timeStamp);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //KATIE
//                    System.out.println("My ringos online " + ringosOnline);
                    if ((!args[2].equals("0") && ringosOnline == n) || args[2].equals("0")) {
                        //System.out.println("sending RTTs for matrix");
                        for (Map.Entry<ringoAddr, Integer> entry : knownRingos.entrySet()) {
                            if (num_iters >= n) {
                                //System.out.println("BREAKING LOOP");
                                break;
                            }
                            ringoAddr ra = entry.getKey();
                            String ip = ra.getIP(); //IP ADDR OF RINGO
                            Integer port = entry.getValue(); //PORT OF RINGO
                            //don't send to yourself
                            if (port == PORT_NUMBER || port == POC_PORT) {
                                num_iters++;
                                continue;
                            }
                            System.out.println("SENDING RINGOS FOR MATRIX");
                            System.out.println(entry.getKey() + entry.getKey().getIP() + " " + entry.getValue());
                            String ping = ip + " " + port;
                            byte[] send = ping.getBytes();

                            Date now = new Date();
                            long msSend = now.getTime();

                            String ms = "PM " + msSend + " " + ip + " " + port;
                            byte[] buf = ms.getBytes();
                            try {
                                InetAddress curRingo = InetAddress.getByName(POC_NAME);
                                DatagramPacket packet = new DatagramPacket(buf, buf.length, curRingo, port);
                                try {
                                    num_iters++;
                                    System.out.println("my iteration number " + num_iters);
                                    ds.send(packet);
                                } catch (Exception e) {
                                    //
                                }
                            } catch (Exception e) {

                            }
                        }
                    }
                }
            }
        };

        checkForPackets.start();

        while (true) {
            Scanner scan = new Scanner(System.in);


            System.out.println("Ringo command: ");
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
    }

    public static void initializeVector() {
        vector = new int[n];
        //dummy data;
        //for (int i = 0; i < n; i++) {
          //  vector[i] = i;
        //}
    }

    public static void initializeMatrix() {
        matrix = new int[n][n];
        matrix[0] = vector;
        matrix[1] = vector; //Replace with vector from Ringo 2...
    }

    public static void sendPingPacket() {
        //System.out.println("\nSending a Ping Request.");
        Date now = new Date();
        long msSend = now.getTime();

        String ms = "P " + msSend + " " + PORT_NUMBER;
        byte[] buf = ms.getBytes();
        try {
            InetAddress poc = InetAddress.getByName(POC_NAME);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, poc, POC_PORT);
            try {
                ds.send(packet);
            } catch (Exception e) {
                //
            }
        } catch (Exception e) {

        }
    }

    public static void recievePing(String pingTime, String senderPort) {

        Date now = new Date();
        long cur = now.getTime();

        long RTT = Long.valueOf(pingTime) - cur;
        RTT *= 2;

        if (!(knownRingos.containsValue(Integer.parseInt(senderPort.trim())))) {
            knownRingos.put(new ringoAddr("127.0.0.1", ringosOnline), Integer.parseInt(senderPort));
        }
        ringosOnline += 1;
        String ret = "PR " + RTT + " " + ringosOnline;
        byte[] send = ret.getBytes();

        try {
            DatagramPacket dp = new DatagramPacket(send, send.length, InetAddress.getLocalHost(), Integer.parseInt(senderPort.trim()));
            ds.send(dp);
            System.out.println("You have sent the requesting Ringo a reply.\n");
            broadcastRingos(InetAddress.getLocalHost(), Integer.parseInt(senderPort));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcastRingos(InetAddress ipAddr, int portNum) {
        try {
            //System.out.println("Broadcasting my known Ringos " + knownRingos + " to " + portNum);
            int i = 0;
            for (Map.Entry<ringoAddr, Integer> entry : knownRingos.entrySet()) {
                ringoAddr ra = entry.getKey();

                String ip = ra.getIP();
                int id = ra.getID();

                Integer value = entry.getValue();
                String keyValue = ("E " + ip + " " + id + " " + value);
                byte[] ringMap = keyValue.getBytes();

                for (Integer port: knownRingos.values()) {
                   //Don't send to itself.
                        DatagramPacket mapPacket =
                                new DatagramPacket(ringMap, ringMap.length, ipAddr, port);
                        ds.send(mapPacket);
                }
                //System.out.println("There are currently " + knownRingos.size() + " Ringos online.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void acceptRingos(String ringoIP, String ringoID, String port){
        try {
            if (!(knownRingos.containsValue(Integer.parseInt(ringoID.trim())))) {
                knownRingos.put(new ringoAddr(ringoIP.trim(), Integer.parseInt(port.trim())), Integer.parseInt(ringoID.trim()));
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

    public static void replyToMatrix(String timeStamp, String ip, String port) {
        Date now = new Date();
        long cur = now.getTime();

        long RTT = Long.valueOf(timeStamp) - cur;
        RTT *= 2;

        String ret = "MR " + RTT + " " + ip + " " + port;
        byte[] send = ret.getBytes();

        try {
            DatagramPacket dp = new DatagramPacket(send, send.length, InetAddress.getLocalHost(), Integer.parseInt(port.trim()));
            ds.send(dp);
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

