import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * akimpel3/903047185
 * ssowders3/903051295
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
    public static String ip;

    public static int ping;
    public static int RINGOID;
    public static boolean calculatedPing = false;
    public static boolean calculatedRing = false;
    public static int num_iters = 0;
    final static int INF = 99999;

    public static int matrixReplies;

    public static Map<ringoAddr, Integer> knownRingos;
    public static Map<ringoAddr, Integer> matrixRingos;

    public static Thread checkForPackets;

    public static String[] vector;
    public static String[][] matrix;

    public static int[][] intMatrix;

    public static DatagramSocket ds;

    public static String cmdError = "There was an error in your command line arguments. Try again.";

    //CHANGE BASED ON CURRENT FILE BEING ACCEPTED
    public static String fileExtension;
    public static long fileLength;

    static class ringoAddr implements Comparable<ringoAddr>, Comparator<ringoAddr> {
        public String IP_ADDR;
        public int ID;
        public int port;
        public String type;

        public ringoAddr(String ip, int p) {
            IP_ADDR = ip;
            ID = p;
        }

        public ringoAddr(String ip, int id, int p, String type) {
            IP_ADDR = ip;
            this.ID = id;
            this.port = p;
            this.type = type;
        }

        public int getID() {
            return ID;
        }

        public String getIP() {
            return IP_ADDR;
        }

        public int getPort() {
            return port;
        }

        public String getType() { return type; }

        public String toString() {
            return "RINGO: " + IP_ADDR + " ,ID: " + ID + " PORT: " + port + " , RTT";
        }

        @Override
        public int compareTo(ringoAddr o1) {
            return this.getPort() - o1.getPort();
        }

        @Override
        public int compare(ringoAddr o1, ringoAddr o2) {
            return o1.getPort() - o2.getPort();
        }

        @Override
        public boolean equals(Object o1) {
            ringoAddr ra = (ringoAddr) o1;
            return (IP_ADDR.equals(ra.getIP())) && (ra.getPort() == port);
        }
        
        @Override
        public int hashCode(){
            int hashcode = 0;
            hashcode = IP_ADDR.hashCode()*20;
            hashcode += port;
            return hashcode;
        }
        
        
    }


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
        matrixRingos = new TreeMap<ringoAddr, Integer>();
        calculatedPing = false;

        matrix = new String[n][n + 2];

        try {
            ds = new DatagramSocket(PORT_NUMBER); //Opens UDP Socket at PORT_NUMBER;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!args[2].equals("0")) {
            sendPingPacket(); //IF THERE EXISTS A POC, PING IT.

        } else {
            ringosOnline = 1;
            RINGOID = ringosOnline - 1;
            try {
                InetAddress address = InetAddress.getLocalHost();
                System.out.println("My POC address " + address);
                String hostIP = address.getHostAddress();

                //add yourself to known ringos
                //knownRingos.put(new ringoAddr(hostIP, 0, PORT_NUMBER), 99999);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            InetAddress address = InetAddress.getByName(POC_NAME);
            String hostIP = address.getHostAddress();
            //not sure if we need this
            //System.out.println("ADDING A " + flag + " TO KNOWNRINGOS");
            knownRingos.put(new ringoAddr(ip, RINGOID, PORT_NUMBER, flag), 99999);
        } catch (Exception e) {
            e.printStackTrace();
        }

        checkForPackets = new Thread() {
            public void run() {
                while (true) {
                    try {
                        byte[] packetInfo = new byte[1024];
                        InetAddress address = InetAddress.getLocalHost();
                        String hostIP = address.getHostAddress();
                        //System.out.println( "Receiving on IP: " + hostIP + "\n" + "Name: " + hostIP);
                        DatagramPacket recieve = //Recieves Ping request at this Ringo's Port number.
                                new DatagramPacket(packetInfo, packetInfo.length, InetAddress.getByName(hostIP), PORT_NUMBER);
                        ds.receive(recieve);

                        String packetData = new String(packetInfo);
                        //System.out.println("RECIEVED PACKET CONTAINING " + packetData);
                        StringTokenizer token = new StringTokenizer(packetData);

                        String FLAG = token.nextToken().trim();
                        //System.out.println("my flag " + FLAG);
                        String firstField = token.nextToken().trim();
                        //System.out.println("firstField " + firstField);

                        if (FLAG.equals("P")) {
                            //System.out.println("ENTERING P");
                            String secondField = token.nextToken().trim();
                            String thirdField = token.nextToken().trim();
                            String type = token.nextToken().trim();
                            //System.out.println("secondField in Ping " + secondField);
                            //System.out.println("thirdField in Ping " + thirdField);
                            //System.out.println("\n\nA new Ringo has requested to ping you.");
                            if (calculatedPing == true) {
                                //System.out.println("PING RECIEVED FOR MATRIX");
                            }

                            recievePing(firstField, thirdField, secondField, type);
                        } else if (FLAG.equals("E")) {
                            //System.out.println("ENTERING E");
                            String secondField = token.nextToken().trim();
                            //System.out.println("secondField in Exchange " + secondField);
                            String thirdField = token.nextToken();
                            String type = token.nextToken();
                           // System.out.println("thirdField in Exchange " + thirdField);
                            acceptRingos(firstField, secondField, thirdField, ping, type);
                            //System.out.println("A Ringo is exchanging information with you...");

                        } else if (FLAG.equals("PR")) {
                            //System.out.println("ENTERING PR");
                            String secondField = token.nextToken().trim();
                            ping = Integer.parseInt(firstField);
                            //System.out.println(ping);
                            ringosOnline = Integer.parseInt(secondField);

                        } else if (FLAG.equals("PM")) {//PING MATRIX{
                            //System.out.println("ENTERING PM");
                            String timeStamp = firstField;
                            String secondField = token.nextToken().trim();
                            String ipOfSender = secondField;
                            String port = token.nextToken();
                            //System.out.println("PING MATRIX timestamp : " + timeStamp + " ipOfSender : " + ipOfSender + " port " + port);
                            replyToMatrix(timeStamp, ipOfSender, port);
                        } else if (FLAG.equals("MR")) {
                            //System.out.println("ENTERING MR");
                            //populate matrix

                            String timeStamp = firstField;
                            String secondField = token.nextToken().trim();
                            String ipOfSender = secondField;
                            String port = token.nextToken();
                            String type = token.nextToken();
                            int pingerTime = Math.abs(Integer.parseInt(timeStamp));
                            matrixRingos.put(new ringoAddr(ipOfSender, RINGOID, Integer.parseInt(port.trim()), type), pingerTime);
                            //System.out.println("Recieved new RTT");

                        } else if (FLAG.equals("M")) {
                            //System.out.println("ENTERING M");
                            matrixReplies++;

                            String[] guest = new String[n + 2];

                            for (int i = 0; i < (n); i++) {
                                String curNumber = token.nextToken().trim();
                                guest[i] = curNumber;
                            }
                            guest[n] = token.nextToken().trim();
                            guest[n + 1] = token.nextToken().trim();

                            matrix[Integer.parseInt(firstField) - 1] = guest;

                            if (matrixRingos.size() == n) {

                            }

                            //System.out.println(Arrays.deepToString(matrix));
                            //System.out.println(firstField);
                            //System.out.println(secondField);
                        } else if (FLAG.equals("I")) { //INFORMATION PACKETS FOR DATA
                            fileExtension = firstField;
                            fileLength = Long.valueOf(token.nextToken().trim());
                            System.out.println("INCOMING PACKET OF TYPE " + fileExtension + " OF LENGTH " + fileLength);
                        } else {//RESERVED FOR DATA PACKETS BECAUSE WE CANNOT CONCATENATE STRINGS
                            System.out.println("Recieved Data Packet!");
                            System.out.println("Packet contained: " );
                            System.out.println(new String(packetInfo));
                        }


                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    //KATIE
                    //System.out.println("There are currently " + knownRingos.size() + " Ringos online.");
                    if (knownRingos.size() == n && !calculatedPing) {
                        //System.out.println("sending RTTs for matrix");
                        for (Map.Entry<ringoAddr, Integer> entry : knownRingos.entrySet()) {
                            if (num_iters >= n) {
                                //System.out.println("BREAKING LOOP");
                                break;
                            }
                            ringoAddr ra = entry.getKey();
                            String ip = ra.getIP(); //IP ADDR OF RINGO
                            Integer port = ra.getPort(); //PORT OF RINGO
                            //don't send to yourself
                            if (port == PORT_NUMBER) {
                                num_iters++;
                                continue;
                            }
                            //System.out.println("SENDING RINGOS FOR MATRIX");
                            //System.out.println(entry.getKey() + entry.getKey().getIP() + " " + entry.getValue());
                            String ping = ip + " " + port;
                            byte[] send = ping.getBytes();

                            Date now = new Date();
                            long msSend = now.getTime();

                            String ms = "PM " + msSend + " " + ip + " " + PORT_NUMBER;
                            byte[] buf = ms.getBytes();
                            try {
                                //System.out.println("************************************");
                                //System.out.println("sendport " + port + " myip " + ip);
                                //System.out.println("************************************");
                                DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), port);
                                try {
                                    num_iters++;
                                    //System.out.println("my iteration number " + num_iters);
                                    ds.send(packet);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        //System.out.println("Current Ring = " + Arrays.toString(vector));
                        if (knownRingos.size() == n) {
                            initializeVector();
                        }


                    }
                    boolean matrixNull = false;
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++){
                            if (matrix[i][j] == null) {
                                matrixNull = true;
                            }
                        }
                    }
                    if (!(matrixNull) && !calculatedRing) {
                        dijkstra(matrix, PORT_NUMBER, false);
                        //calculateRing();
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
                sendData(filename);

            } else if (command.equals("show-matrix")) {
                System.out.print("     ");
                for (int l = 0; l < n; l++) {
                    System.out.print(l + ")     ");
                }
                System.out.println("");
                for (int i = 0; i < n; i++) {
                    System.out.print(i + ") [");
                    for (int j = 0; j < n; j++) {
                        System.out.print(matrix[i][j] + "     ");
                    }
                    System.out.println("]");
                }

                System.out.println("\n**********************");
                System.out.println("IDEAL MATRIX");
                printMatrix(intMatrix);
                System.out.println();
                System.out.println("**********************");
                String[][] a = new String[matrix.length][];
                for (int i = 0; i < matrix.length; i++) {
                    a[i] = Arrays.copyOfRange(matrix[i], n, n + 2);
                }

                for (int i = 0; i < n; i++) {
                    System.out.println("Row/Column " + i + " refers to Ringo " + Arrays.deepToString(a[i]));
                }

            } else if (command.equals("show-ring")) {
                dijkstra(matrix, PORT_NUMBER, true);
                System.out.println("\nthe nodes visited are as follows");
                TSPNearestNeighbor tspNearestNeighbour = new TSPNearestNeighbor();
                tspNearestNeighbour.tsp(intMatrix);
                System.out.println("\n******************************");
            } else if (command.equals("disconnect")) {
                System.out.println("Exiting...");
                ds.close();
                System.exit(-1);
            } else if (command.equals("show-ringos")) {
                System.out.println("This Ringo is connected to...");
                if (true) {
                    for (Map.Entry<ringoAddr, Integer> otherRingos : knownRingos.entrySet()) {
                        ringoAddr ra = otherRingos.getKey();
                        String ip = ra.getIP();
                        int port = ra.getPort();
                        int id = ra.getID();
                        //System.out.println("my ip " + ip + " my port " + port + " id " + id);

                        Integer ping = otherRingos.getValue();
                        System.out.println("Ringo @ " + ip + ":" + port + " with ping ~" + ping + " ms. of type " + ra.getType());
                    }
                } else {
                    System.out.println("Connect the number of Ringos equal to N first.");
                }
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
        

        vector = new String[n];
        int i = 0;
        int count = 1;

        for (Map.Entry<ringoAddr, Integer> otherRingos : knownRingos.entrySet()) {
            ringoAddr ra = otherRingos.getKey();
            String ip = ra.getIP();
            int port = ra.getPort();
            int id = ra.getID();

            Integer ping = otherRingos.getValue();
            vector[i] = "" + ping;
            if ((ping == 99999)) {
                count = i + 1;
            }
            //System.out.println("Ringo @ " + ip + ":" + port + " with ping ~" + ping + " ms.");
            i++;
        }

        String s = "";
        try {
            s = "M " + count + " ";
        } catch (Exception e) {
            //
        }
        for (int j = 0; j < n; j++) {
            s = s + " " + vector[j];
        }

        try {
            s = s + " " + InetAddress.getLocalHost().getHostAddress() + " " + PORT_NUMBER + " " + flag;
        } catch (Exception e) {
            //
        }
        byte[] info = s.getBytes();

        for (Map.Entry<ringoAddr, Integer> known : knownRingos.entrySet()) {
            try {

                ringoAddr ra = known.getKey();
                if (!(ra.getIP().equals(InetAddress.getLocalHost()) && (ra.getPort() != PORT_NUMBER))) {
                    //System.out.println("SENDING VECTOR TO " + ra.getIP() + ":" + ra.getPort());
                    InetAddress target = InetAddress.getByName(ra.getIP());
                    DatagramPacket packet = new DatagramPacket(info, info.length, InetAddress.getByName(ra.getIP()), ra.getPort());
                    try {
                        ds.send(packet);
                        //System.out.println("Sent vector!");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        calculatedPing = true;


    }

    public static void sendPingPacket() {
        //System.out.println("\nSending a Ping Request.");
        Date now = new Date();
        long msSend = now.getTime();
        RINGOID = ringosOnline;

        String ms = "P " + msSend + " " + PORT_NUMBER + " " + ip + " " + flag;
        byte[] buf = ms.getBytes();
        try {
            InetAddress poc = InetAddress.getByName(POC_NAME);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, poc, POC_PORT);
            //System.out.println("My packet contains ipaddr " + poc + " and port " + POC_PORT);
            try {
                ds.send(packet);
            } catch (Exception e) {
                //
            }
        } catch (Exception e) {

        }
    }

    public static void recievePing(String pingTime, String senderIP, String senderPort, String type) {

        Date now = new Date();
        long cur = now.getTime();

        long RTT = Long.valueOf(pingTime) - cur;
        RTT *= 2;
        //System.out.println("my round trip time " + RTT);

        //commenting this out for now to get basic functionality back
        ringoAddr testRA = new ringoAddr(senderIP, ringosOnline, Integer.parseInt(senderPort), type);
        if (!(knownRingos.containsKey(testRA))) {
            try {
                //System.out.println("ADDING TO KNOWN RINGOS : senderIP " + senderIP + " ringosOnline " + ringosOnline + " senderPort " + senderPort);
                knownRingos.put(new ringoAddr(senderIP, ringosOnline, Integer.parseInt(senderPort), type), Math.abs((int) RTT));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ringosOnline += 1;

        String ret = "PR " + RTT + " " + ringosOnline;
        byte[] send = ret.getBytes();


        try {
            //System.out.println("sending at ip address " + senderIP + "port number " + senderPort);
            DatagramPacket dp = new DatagramPacket(send, send.length, InetAddress.getByName(senderIP), Integer.parseInt(senderPort.trim()));
            ds.send(dp);
            //System.out.println("You have sent the requesting Ringo a reply.\n");
            broadcastRingos(InetAddress.getByName(senderIP), Integer.parseInt(senderPort));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcastRingos(InetAddress ipAddr, int portNum) {
        try {
            //System.out.println("Broadcasting my known Ringos " + knownRingos);
            int i = 0;
            for (Map.Entry<ringoAddr, Integer> entry : knownRingos.entrySet()) {
                ringoAddr ra = entry.getKey();

                String ip = ra.getIP();
                int id = ra.getID();
                int myport = ra.getPort();

                //System.out.println("PRINTING VALUES IN BROADCAST RINGOS PACKET " + " ip " + ip + " id " + id + " port " + myport);

                String keyValue = ("E " + ip + " " + id + " " + myport + " " + ra.getType());
                byte[] ringMap = keyValue.getBytes();

                for (Map.Entry<ringoAddr, Integer> key: knownRingos.entrySet()) {
                    //Don't send to itself.
                    
                    if (!(key.getKey().getIP().equals(ip) && !(key.getKey().getPort() != myport))) {
                        DatagramPacket mapPacket =
                            new DatagramPacket(ringMap, ringMap.length, InetAddress.getByName(key.getKey().getIP()), key.getKey().getPort());
                  
                        //System.out.println("SENDING EXCHANGES to ipAddr " + key.getKey().getIP() + "and port " + key.getKey().getPort());
                        ds.send(mapPacket);
                    }
                    
                }
                //System.out.println("\n There are currently " + knownRingos.size() + " Ringos online.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void acceptRingos(String ringoIP, String ringoID, String port, int ping, String type) {
        try {
            ringoAddr testRA = new ringoAddr(ringoIP.trim(), Integer.parseInt(ringoID.trim()), Integer.parseInt(port.trim()), type);
            //System.out.println("KNOWN RINGOS AT THIS POINT IN TIME: " + knownRingos);
            if (!(knownRingos.containsKey(testRA))) {
                //System.out.println("ADDING NEW RINGO " + testRA.getIP() + ":" + testRA.getPort());
                knownRingos.put(new ringoAddr(ringoIP.trim(), Integer.parseInt(ringoID.trim()), Integer.parseInt(port.trim()), type), Math.abs(ping));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendData(String filename) {

        //ASSUMING THIS METHOD IS ONLY BEING RUN BY THE SENDER
        byte[] fileAsArray = null;
        String extension = "";

        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i+1);
        }

        try {
            fileAsArray = Files.readAllBytes(new File(filename).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String destinationIP = "";
        int destinationPort = 0;

        for (Map.Entry<ringoAddr, Integer> key: knownRingos.entrySet()) {
            //Don't send to itself.
            ringoAddr ra = key.getKey();

            //FOUND THE DESTINATION
            if (ra.getType().equals("R")) {
                destinationIP = ra.getIP();
                destinationPort = ra.getPort();
                break;
            }
        }



        if (destinationIP == "") {
            System.out.println("No ringo could be detected as a reciever.");
            return;
        } else {
            try {
                String info = "I " + extension + " " + fileAsArray.length;
                byte[] infoToBytes = info.getBytes();
                DatagramPacket infoPacket = new DatagramPacket(infoToBytes, infoToBytes.length, InetAddress.getByName(destinationIP), destinationPort);
                ds.send(infoPacket);
                DatagramPacket dataPacket = new DatagramPacket(fileAsArray, fileAsArray.length, InetAddress.getByName(destinationIP), destinationPort);
                ds.send(dataPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendDummy() {
        //TODO: Send a dummy packet to other Ringos in the network to see if they are alive.
        //Might not be in Milestone 2.
    }

    public void recieveDummy() {
        //TODO: Send a dummy packet to other Ringos in the network to see if they are alive.
        //Might not be in Milestone 2.
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

        String ret = "MR " + RTT + " " + ip + " " + PORT_NUMBER;
        byte[] send = ret.getBytes();

        try {
            DatagramPacket dp = new DatagramPacket(send, send.length, InetAddress.getByName(ip), Integer.parseInt(port.trim()));
            ds.send(dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    //    final static int NO_PARENT = -1;
    public static void dijkstra(String[][] graph, int src, boolean showOutput) {
        //TODO: Construct shortest path among all Ringos in the network.
        //System.out.println(matrix.toString());
        int dist[] = new int[n]; // The output array. dist[i] will hold
        // the shortest distance from src to i
        int[][] newMatrix = new int[n][n];
        intMatrix = new int[n][n];

        for (int i = 0; i < n; i++) {
            //System.out.print(i + ") [");
            for (int j = 0; j < n; j++) {
                //System.out.print(graph[i][j] + "     ");
                intMatrix[i][j] = Integer.parseInt(graph[i][j]);
                newMatrix[i][j] = intMatrix[i][j];
            }
        }
        for (int i = 0; i < n; i++) {
            //System.out.print(i + ") [");
            for (int j = 0; j < n; j++) {
                intMatrix[i][j] = (newMatrix[i][j] + newMatrix[j][i]) / 2;
            }
        }
    }


    static void printMatrix(int dist[][]) {
        System.out.println("\n");
        for (int i=0; i<n; ++i) {
            for (int j=0; j<n; ++j) {
                if (dist[i][j]==INF)
                    System.out.print("INF ");
                else
                    System.out.print(dist[i][j]+"   ");
            }
            System.out.println();
        }
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
                InetAddress address = InetAddress.getLocalHost();
                String hostIP = address.getHostAddress() ;
                String hostName = address.getHostName();
                System.out.println( "IP: " + hostIP + "\n" + "Name: " + hostName);
                InetAddress i = InetAddress.getByName(hostIP);
                ip = i.getHostAddress();
                System.out.println("This Ringo is located at IP: " + ip);
                POC_PORT = Integer.parseInt(args[3]);
                n = Integer.parseInt(args[4]);
            } catch (Exception e) {
                System.out.println(cmdError);
                System.exit(-1);
            }
        }
    }
}

