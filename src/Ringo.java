import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

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
    public static String MY_IP;
    public static boolean myNeighborOnline = true;

    //MUST CHANGE TO CHANGE HOW BIG PACKETS ARE SENT
    final static int SEND_SIZE = 1000;

    public static int numPacketsInRecieve;

    public static int ping;
    public static int RINGOID;
    public static int sequenceNum;
    public static boolean calculatedPing = false;
    public static boolean calculatedRing = false;
    public static int num_iters = 0;
    public static int numRemoves;
    final static int INF = 99999;
    public static boolean backOnline = false;

    private static Timer timer;

    private static TimerTask myTask;

    public static int matrixReplies;

    public static Map<ringoAddr, Integer> knownRingos;
    public static Map<ringoAddr, Integer> offlineRingos;
    //public static Map<Integer, String[]> sendRingos;

    public static Thread checkForPackets;
    public static int myIdxInA;

    public static int offlinePacketCount = 0;

    public static String[] vector;
    public static String[][] matrix;
    public static String[][] tempMatrix;

    public static int[][] intMatrix;
    public static String[] dataRecieve;

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
            port = p;
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
            return o2.getPort() - o1.getPort();
        }

        @Override
        public boolean equals(Object o1) {
            ringoAddr ra = (ringoAddr) o1;
            return (IP_ADDR.equals(ra.getIP())) && (ra.getPort() == port);
        }

        @Override
        public int hashCode(){
            int hashcode = 0;
            hashcode = this.IP_ADDR.hashCode();
            hashcode += this.port;
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
        offlineRingos = new HashMap<ringoAddr, Integer>();
        calculatedPing = false;

        matrix = new String[n][n + 2];

        try {
            ds = new DatagramSocket(PORT_NUMBER); //Opens UDP Socket at PORT_NUMBER;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!args[2].equals("0")) {
            sendPingPacket(); //IF THERE EXISTS A POC, PING IT.
            try {
                InetAddress address = InetAddress.getLocalHost();
                System.out.println("My POC address " + address);
                MY_IP = address.getHostAddress();
            } catch (UnknownHostException E) {
                E.printStackTrace();
            }

        } else {
            ringosOnline = 1;
            RINGOID = ringosOnline - 1;
            try {
                InetAddress address = InetAddress.getLocalHost();
                System.out.println("My POC address " + address);
                MY_IP = address.getHostAddress();
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
                        //System.out.println("PACKET OF TYPE: " + FLAG);

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
                            //matrixRingos.put(new ringoAddr(ipOfSender, RINGOID, Integer.parseInt(port.trim()), type), pingerTime);
                            //System.out.println("Recieved new RTT");

                        } else if (FLAG.equals("M")) {
                            //System.out.println("ENTERING M");
                            //matrixReplies++;

                            String[] guest = new String[knownRingos.size() + 2];

                            for (int i = 0; i < (knownRingos.size()); i++) {
                                String curNumber = token.nextToken().trim();
                                //System.out.println(curNumber);
                                guest[i] = curNumber;
                            }
//                            if (added) {
//                                n++;
//                            }
                            guest[knownRingos.size()] = token.nextToken().trim();
                            //System.out.println("What am I receiving in M?" + guest[n]);
                            //System.out.println("Size of ringos map " + knownRingos.size());
                            //System.out.println("n " + n);
                            //System.out.print(n);
                            guest[knownRingos.size() + 1] = token.nextToken().trim();
                            //System.out.println("What am I receiving in M?" + guest[knownRingos.size()+1]);
                            //System.out.println("What is firstfield? " + firstField);

                            matrix[Integer.parseInt(firstField) - 1] = guest;

//                            if (matrixRingos.size() == n) {
//
//                            }

                            //System.out.println(Arrays.deepToString(matrix));
                            //System.out.println(firstField);
                            //System.out.println(secondField);
                        } else if (FLAG.equals("INFO")) { //INFORMATION PACKETS FOR DATA

                            fileExtension = firstField;
                            fileLength = Long.valueOf(token.nextToken().trim());
                            numRemoves = Integer.parseInt(token.nextToken().trim());

                            System.out.println("Incoming Packet of type " + fileExtension + ". There will be " + fileLength + " packets.");
                            String sending = "INFO " + fileExtension + " " + fileLength + " " + (numRemoves);

                            if (!(flag.equals("R"))) {
                                //System.out.println("FORWARDING INFO PACKET");
                                forwardData(sending.getBytes(), numRemoves);
                            }
                        } else if (FLAG.equals("C")) {

                            String senderIP = firstField;
                            int senderPort = Integer.parseInt(token.nextToken().trim());
                            //System.out.println(senderIP + " is checking to see if I'm alive");
                            String send = "CR " + MY_IP + " " + PORT_NUMBER;
                            byte[] sendData = send.getBytes();

                            try {
                                DatagramPacket infoPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(senderIP), (senderPort));
                                //System.out.println("Sending ack packet to " + senderIP + ":" + senderPort);
                                ds.send(infoPacket);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (FLAG.equals("CR")) {
                            myNeighborOnline = true;
                        } else if (FLAG.equals("OFF")) {

                            if (offlinePacketCount >= 3) {
                                //System.out.println("RINGO " + firstField + " IS OFFLINE.");
                                //System.out.println("Clearing knownRingos");
                                //knownRingos.clear();
                                String[][] b = new String[matrix.length][matrix.length];
                                for (int j = 0; j < matrix.length; j++) {
                                    b[j] = Arrays.copyOfRange(matrix[j], n, n + 2);
                                }
//                            System.out.println(" is equal to: ");
                                //System.out.println(Arrays.deepToString(b));
                                String[] cur = b[(Integer.parseInt(firstField))];
                                String removeIP = cur[0];
                                String removePort = cur[1];
                                //System.out.println("***********************");
                                //System.out.println(removeIP + " " + removePort);

                                ringoAddr temp = new ringoAddr(removeIP, Integer.valueOf(removePort.trim()));


                                //System.out.println("HASH OF REMOVED RINGO = " + temp.getIP().hashCode()  + " " + temp.getPort());

                                if (PORT_NUMBER != Integer.valueOf(removePort.trim())) {
                                    if (offlineRingos.isEmpty()) {
                                        offlineRingos.put(temp, knownRingos.remove(temp));
                                        //System.out.println("Known Ringos is now...");
                                        //printKnownRingos();
                                        //System.out.println("Offline Ringos is now...");
                                        printOfflineRingos();
                                    }
                                }
                                tempMatrix = matrix;
                                //System.out.println("temp matrix " + Arrays.deepToString(tempMatrix));
                                matrixMaker m = new matrixMaker(matrix, n);
                                //System.out.println(m);

                                //remove rows with the value "10" and then reprint the array
                                matrix = m.removeRowsWithValue(Integer.parseInt(firstField), matrix);

                                backOnline = true;
                                offlinePacketCount = 0;
                            } else {
                                offlinePacketCount++;
                            }
                        } else if (FLAG.equals("ON")) {
                            //System.out.println("IP NUMBER FROM RINGO ONLINE " + firstField);
                            //System.out.println("RINGO IS BACK ONLINE");
//                            System.out.println("TEMP MATRIX : " + Arrays.deepToString(tempMatrix));
//                            System.out.println("Temp matrix size = " + tempMatrix.length + " x " + tempMatrix[0].length);
//                            System.out.println("PUTTING INTO: ++++++++");
//                            System.out.println("MATRIX : " + Arrays.deepToString(matrix));
//                            System.out.println("Matrix size = " + matrix.length + " x " + matrix[0].length);

                            matrix = new String[n][n + 2];
                            matrix = tempMatrix;
                            //System.out.println(packetData);
                            int addPort = Integer.parseInt(token.nextToken().trim());
                            //System.out.println("PORT NUMBER FROM RINGO ONLINE " + addPort);
                            ringoAddr temp = new ringoAddr(firstField, addPort);
                            if (!offlineRingos.isEmpty()) {
                                System.out.println("Adding back from offline Ringos.");
                                knownRingos.put(new ringoAddr(firstField, 0, addPort, "F"), offlineRingos.remove(temp));
                                printKnownRingos();
                            }
                            //System.out.println("Clearing knownRingos");
                            //knownRingos.clear();
//                            String[][] c = new String[matrix.length][matrix.length];
//                            for (int j = 0; j < matrix.length; j++) {
//                                c[j] = Arrays.copyOfRange(matrix[j], n, n + 2);
//                            }
//
////                            System.out.println(" is equal to: ");
////                            System.out.println(Arrays.deepToString(b));
//                            String[] cur = c[(Integer.parseInt(firstField))];
//                            String addIP = cur[0];
//                            String addPort = cur[1];
                            for (Map.Entry<ringoAddr, Integer> entry : offlineRingos.entrySet()) {
                                if (entry.getKey().getIP().equals(firstField.trim()) && entry.getKey().getPort() == addPort) {
                                    int newPort = entry.getKey().getPort();
                                    String addIP = entry.getKey().getIP();
                                    int newID = entry.getKey().getID();
                                    String newType = entry.getKey().getType();
                                    int addRTT = entry.getValue();
                                    //try {
                                    knownRingos.put(new ringoAddr(addIP, newID, newPort, newType), addRTT);
                                    //System.out.println("KNOWN RINGOS AFTER ADD: " + knownRingos);
                                    //UNCOMMENT TO SEE PROBLEM
//                                    if(entry.getKey().getPort() != PORT_NUMBER) {
//                                        n++;
//                                        System.out.println("INCREMENTING N, N is now: " + n);
//                                    }
                                    //initializeVector();
                                }
                                try {
                                    //System.out.println("hety");
                                    matrix = tempMatrix;
                                    //System.out.println(Arrays.deepToString(matrix));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        } else {//RESERVED FOR DATA PACKETS BECAUSE WE CANNOT CONCATENATE STRINGS
                            if (!(flag.equals("R"))) {
                                //System.out.println("FORWARDING DATA");
                                forwardData(packetInfo, numRemoves + 1);
                            } else {
                                //System.out.println(packetData);
                                System.out.println("Packet has arrived to the reciever Ringo.");
                                //System.out.println("Packet contains: ");

                                //PACKET NUMBER
                                //System.out.println("FLAG: " + FLAG);
                                //System.out.println("FIRST FIELD = " + firstField.trim());

                                int curPacket = Integer.parseInt(FLAG.trim());
                                String data = token.nextToken();
                                while (token.hasMoreTokens()) {
                                    data += " " + token.nextToken();
                                }
                                //System.out.println(data);
                                if (curPacket == 0) {
                                    numPacketsInRecieve = (int) Double.parseDouble(firstField.trim());
                                    dataRecieve = new String[numPacketsInRecieve + 1];
                                    if (numPacketsInRecieve == 1) {
                                        System.out.println("FILE ARRIVED: " + data);

                                    }
                                }

                                //TOTAL NUMBER OF PACKETS
                                System.out.println("Data: " + data);

                                //DATA
                                dataRecieve[curPacket] = data;
                                //System.out.println("DATA RECIEVE [" + curPacket + "] = " + data);

                                //ALL PACKETS RECIEVED
                                if (Integer.parseInt(FLAG.trim()) == numPacketsInRecieve - 1) {
                                    System.out.println("DONE");
                                    for (int i = 0; i < numPacketsInRecieve; i++) {
                                        if (dataRecieve[i] == null || dataRecieve[i] == "") {
                                            //Dropped packet;
                                            System.out.println("There was a dropped packet.");
                                            break;
                                        }
                                    }

                                    String concatPackets = "";
                                    for (int i = 0; i < numPacketsInRecieve; i++) {
                                        concatPackets += dataRecieve[i];
                                    }
                                    System.out.println("FILE ARRIVED: " + concatPackets);

                                }
                                //System.out.println(new String(packetInfo));
                            }
                        }




                    } catch (Exception e) {
                        //e.printStackTrace();
                    }

                    //KATIE
                    //System.out.println("There are currently " + knownRingos.size() + " Ringos online.");
                    if (knownRingos.size() == n && !calculatedPing) {
                        //System.out.println("sending RTTs for matrix");
                        //System.out.println("my known ringos " + knownRingos);
                        for (Map.Entry<ringoAddr, Integer> entry : knownRingos.entrySet()) {
                            if (num_iters >= knownRingos.size()) {
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
                        //START TIMER TO CHECK ALL
                        if (knownRingos.size() == n) {
                            initializeVector();
                            timer = new Timer();

                            myTask = new TimerTask() {
                                @Override
                                public void run() {
                                    keepAlive();
                                    TimerTask task2 = new TimerTask() {
                                        @Override
                                        public void run() {

                                        }
                                    };
                                    timer.schedule(task2, 1000, 1000);



                                    if (myNeighborOnline) {
                                        //System.out.println("RINGO " + (myIdxInA) + " is online.");
                                    } else {
                                        //System.out.println("RINGO " + (myIdxInA) + " is offline.");
                                        //System.out.println("Broadcasting to remove index" + myIdxInA);
                                        String send = "OFF " + myIdxInA;
                                        for (Map.Entry<ringoAddr, Integer> otherRingos : knownRingos.entrySet()) {

                                            ringoAddr cur = otherRingos.getKey();
                                            try {
                                                if (offlineRingos.isEmpty()) {
                                                    DatagramPacket packet = new DatagramPacket(send.getBytes(), send.getBytes().length, InetAddress.getByName(cur.getIP()) , cur.getPort());
                                                    ds.send(packet);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    myNeighborOnline = false;
//                                    try {
//                                        wait(1000);
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                    }

                                }
                            };

                            timer.schedule(myTask, 1000, 2500);
                        }


                    }
                    boolean matrixNull = false;
                    for (int i = 0; i < knownRingos.size(); i++) {
                        for (int j = 0; j < knownRingos.size(); j++){
                            if (matrix[i][j] == null) {
                                matrixNull = true;
                            }
                        }
                    }
                    if (!(matrixNull) && !calculatedRing) {
                        makeIdealMatrix(matrix, PORT_NUMBER, false);
                        //calculateRing();
                    }
                }
            }
        };

        checkForPackets.start();

        while (true) {
            Timer offlineTimer = new Timer();
            Scanner scan = new Scanner(System.in);
            System.out.println("Ringo command: ");
            StringTokenizer token = new StringTokenizer(scan.nextLine());

            String command = token.nextToken();

            if (command.equals("offline")) {
                int time = Integer.parseInt(token.nextToken());
                System.out.println("Going offline for " + time + " seconds.");
                ds.close();
                tempMatrix = matrix;

                try {

                    timer.cancel();
                    myTask.cancel();
                    Thread.sleep(time * 1000);
                    checkForPackets.sleep(time * 1000);

                } catch (Exception e) {
                    //e.printStackTrace();
                }
                System.out.println("Coming out of sleep");

                timer = new Timer();
                myTask = new TimerTask() {
                    @Override
                    public void run() {
                        keepAlive();
                        TimerTask task2 = new TimerTask() {
                            @Override
                            public void run() {

                            }
                        };
                        timer.schedule(task2, 1000, 1000);
                        //boolean sendOnce = false;
                        if (myNeighborOnline) {

                            //System.out.printlknownRingos.size()("RINGO " + (myIdxInA) + " is online during myNeighborOnline.");
                            //System.out.println("Broadcasting to add index" + myIdxInA);

                            String send = "ON " + ip + " " + PORT_NUMBER;
                            for (Map.Entry<ringoAddr, Integer> otherRingos : knownRingos.entrySet()) {

                                ringoAddr cur = otherRingos.getKey();
                                try {
                                    if (!(cur.getPort() == PORT_NUMBER && MY_IP == cur.getIP())) {
                                        DatagramPacket packet = new DatagramPacket(send.getBytes(), send.getBytes().length, InetAddress.getByName(cur.getIP()) , cur.getPort());
                                        ds.send(packet);
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        } else {
                            //System.out.println("Broadcasting to remove index" + myIdxInA);
                            String send = "OFF " + myIdxInA;
                            for (Map.Entry<ringoAddr, Integer> otherRingos : knownRingos.entrySet()) {

                                ringoAddr cur = otherRingos.getKey();
                                try {
                                    DatagramPacket packet = new DatagramPacket(send.getBytes(), send.getBytes().length, InetAddress.getByName(cur.getIP()) , cur.getPort());
                                    ds.send(packet);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }
                        myNeighborOnline = false;

                    }
                };
                try {
                    ds = new DatagramSocket(PORT_NUMBER);
                    timer.schedule(myTask, 0, 2500);
                    //System.out.println("RINGO ONLINE");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (command.equals("send")) {
                String filename = token.nextToken();
                sendData(filename, 0); //SELECT WHICH PORTION OF "A" TO SEND TO.

            } else if (command.equals("show-matrix")) {
                System.out.print("     ");
                for (int l = 0; l < knownRingos.size(); l++) {
                    System.out.print(l + ")     ");
                }
                System.out.println("");
                for (int i = 0; i < knownRingos.size(); i++) {
                    System.out.print(i + ") [");
                    for (int j = 0; j < knownRingos.size(); j++) {
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
                    a[i] = Arrays.copyOfRange(matrix[i], knownRingos.size(), knownRingos.size() + 2);
                }

                for (int i = 0; i < knownRingos.size(); i++) {
                    System.out.println("Row/Column " + i + " refers to Ringo " + Arrays.deepToString(a[i]));
                }

            } else if (command.equals("show-ring")) {
                makeIdealMatrix(matrix, PORT_NUMBER, true);
                System.out.println("\nthe nodes visited are as follows");
                TSPNearestNeighbor tspNearestNeighbour = new TSPNearestNeighbor();
                Stack<Integer> tmpStack = new Stack<>();
                //System.out.println(intMatrix);
                tmpStack = tspNearestNeighbour.tsp(intMatrix);
                System.out.print(0 + "\t");
                while (!tmpStack.isEmpty()) {
                    int dst = tmpStack.pop();
                    System.out.print(dst + "\t");
                }
                System.out.println("\n******************************");
            } else if (command.equals("disconnect")) {
                System.out.println("Exiting...");
                ds.close();
                System.exit(-1);
            } else if (command.equals("show-ringos")) {
                System.out.println("This Ringo is connected to...");
                if (true) {
                    printKnownRingos();
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
        for (int j = 0; j < knownRingos.size(); j++) {
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
        backOnline = false;


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

    public static void sendData(String filename, int curRemoves) {

        //ASSUMING THIS METHOD IS ONLY BEING RUN BY THE SENDER
        byte[] fileAsArray = null;
        String extension = "";
        Stack<Integer> stack;
        makeIdealMatrix(matrix, PORT_NUMBER, false);
        TSPNearestNeighbor tspNearestNeighbour = new TSPNearestNeighbor();
        stack = tspNearestNeighbour.tsp(intMatrix);

        //System.out.println("THE CURRENT STACK IS");
        //System.out.println(stack.toString().replaceAll("\\[", "").replaceAll("]", ""));

        Queue<Integer> q = new LinkedList<>();
        while (!stack.empty()) {
            q.add(stack.pop());
        }

//        System.out.println("THE NEW QUEUE IS: ");
  //      System.out.println(q.toString().replaceAll("\\[", "").replaceAll("]", ""));

        //linkedList myLinkedList = new linkedList();
        stack.push(0);


        //---------------GETS FILE TYPE
        int i = filename.lastIndexOf('.');
        if (i > 0) {
            extension = filename.substring(i + 1);
        }
        //---------------

        //---------------GETS BYTE ARRAY FROM FILE
        try {
            fileAsArray = Files.readAllBytes(new File(filename).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //----------------------

        String destinationIP = "";
        int destinationPort = 0;
//        ArrayList<Integer> sendList = new ArrayList<>();
//        while (!stack.isEmpty()) {
//            int myID = stack.pop();
//            myLinkedList.insertAtEnd(myID);
//        }
//        myLinkedList.display();

        String[][] a = new String[matrix.length][matrix.length];
        for (int j = 0; j < matrix.length; j++) {
            a[j] = Arrays.copyOfRange(matrix[j], knownRingos.size(), knownRingos.size() + 2);
        }

        try {

            //System.out.println("A is equal to: ");
            //System.out.println(Arrays.deepToString(a));

            for (int count = 0; count < curRemoves - 1; count++) {
                q.remove();
            }

            String[] cur = a[(knownRingos.size()) - q.remove()];

            destinationIP = cur[0];
            destinationPort = Integer.parseInt(cur[1].trim());
            System.out.println("SENDING PACKET TO: " + destinationIP + " : " + destinationPort);

            if (destinationPort == PORT_NUMBER && destinationIP.equals(MY_IP)) {
                cur = a[(knownRingos.size()) - q.remove()];
                curRemoves += 1;
                destinationIP = cur[0];
                destinationPort = Integer.parseInt(cur[1].trim());
                System.out.println("JK, SENDING PACKET TO: " + destinationIP + " : " + destinationPort);

            }
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("Receiver is offline");

        }


        if (destinationIP == "") {
            System.out.println("No ringo could be detected as a reciever.");
            return;
        } else {
            try {
                double numPacketsToSend = Math.ceil((double)(fileAsArray.length) / (double)(SEND_SIZE));
                //System.out.println("THERE ARE " + numPacketsToSend + " PACKETS TO SEND");

                String info = "INFO " + extension + " " + numPacketsToSend + " " + curRemoves;
                byte[] infoToBytes = info.getBytes();
                DatagramPacket infoPacket = new DatagramPacket(infoToBytes, infoToBytes.length, InetAddress.getByName(destinationIP), destinationPort);
                ds.send(infoPacket);
                //System.out.println("Sent Info packet");

                int startingIdx = 0;

                for (int byteIdx = 0; byteIdx < numPacketsToSend; byteIdx++) {
                    //System.out.println("SENDING PACKET " + byteIdx);
                    int endingIdx = startingIdx + SEND_SIZE;
                    String sizeInfo = "" +  + byteIdx + " " + numPacketsToSend + " ";
                    byte[] sizeBytes = sizeInfo.getBytes();

                    if (endingIdx > fileAsArray.length) {
                        endingIdx = fileAsArray.length;
                    }

                    byte[] sending = Arrays.copyOfRange(fileAsArray, startingIdx, endingIdx);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    outputStream.write(sizeBytes);
                    outputStream.write(sending);

                    byte sendData[] = outputStream.toByteArray();

                    try {
                        DatagramPacket dataPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(destinationIP), destinationPort);
                        ds.send(dataPacket);
                    } catch (Exception e) {
                        DatagramPacket dataPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(destinationIP), destinationPort);
                        ds.send(dataPacket);
                    }

                    startingIdx += SEND_SIZE;
                }

            } catch (Exception e) {
                System.out.println("Cannot send either trying to send from a forwarder or reciever ringo is offline.");

            }
        }
    }

    public static void forwardData(byte[] info, int curRemoves) {

        Stack<Integer> stack;
        makeIdealMatrix(matrix, PORT_NUMBER, false);
        TSPNearestNeighbor tspNearestNeighbour = new TSPNearestNeighbor();
        stack = tspNearestNeighbour.tsp(intMatrix);

        Queue<Integer> q = new PriorityQueue<>();
        while (!stack.empty()) {
            q.add(stack.pop());
        }

        String[][] a = new String[matrix.length][matrix.length];
        for (int j = 0; j < matrix.length; j++) {
            a[j] = Arrays.copyOfRange(matrix[j], knownRingos.size(), knownRingos.size() + 2);
        }

        for (int count = 0; count < curRemoves; count++) {
            int removed = q.remove();
            System.out.println(removed);
        }

        System.out.println(Arrays.deepToString(a));
        System.out.println("MY INDEX " + (myIdxInA - 1));
        int sendTo = 0;
        if (myIdxInA == knownRingos.size()) {
            sendTo = 0;
        } else {
            sendTo = myIdxInA;
            System.out.println("Sending to Index " + sendTo);
        }
        String[] neighbor = a[sendTo];
        String destinationIP = neighbor[0];
        int destinationPort = Integer.parseInt(neighbor[1]);

        System.out.println("Sending to : " + destinationIP + " :" + destinationPort);


        if (!(destinationIP.trim().equals(MY_IP) && destinationPort == PORT_NUMBER)) {
            //System.out.println("SENDING TO ITSELF");
            int newQ = q.remove();
            //System.out.println(newQ);
            //cur = a[newQ];
            //destinationIP = cur[0];
            //destinationPort = Integer.parseInt(cur[1]);
            //System.out.println("FORWARDING PACKET TO: " + destinationIP + " : " + destinationPort);


        }

        if (destinationIP == "") {
            System.out.println("No ringo could be detected as a reciever.");
            return;
        } else {
            try {
                DatagramPacket infoPacket = new DatagramPacket(info, info.length, InetAddress.getByName(destinationIP), destinationPort);
                ds.send(infoPacket);
            } catch (Exception e) {
                if (myIdxInA == knownRingos.size()) {
                    sendTo = 1;
                } else {
                    sendTo = myIdxInA + 1;
                    System.out.println("Sending to Index " + sendTo);
                }
                try {
                    neighbor = a[sendTo];
                    destinationIP = neighbor[0];
                    destinationPort = Integer.parseInt(neighbor[1]);
                    DatagramPacket infoPacket = new DatagramPacket(info, info.length, InetAddress.getByName(destinationIP), destinationPort);
                    ds.send(infoPacket);
                } catch (Exception f) {

                }
            }
        }


    }

    public static void keepAlive() {
        String[][] a = new String[matrix.length][matrix.length];
        for (int j = 0; j < matrix.length; j++) {
            a[j] = Arrays.copyOfRange(matrix[j], knownRingos.size(), knownRingos.size() + 2);
        }

        //System.out.println(Arrays.deepToString(a));

//        System.out.println(MY_IP.trim());
//        System.out.println(PORT_NUMBER);

        int idx = 0;
        for (int i = 0; i < knownRingos.size(); i++) {
//            System.out.println(MY_IP.trim() + " =? " + a[i][0].trim());
//            System.out.println(MY_IP.trim() + " =? " + a[i][0].trim());
//
//            System.out.println(PORT_NUMBER == Integer.parseInt(a[i][1].trim()));
//            System.out.println(MY_IP.trim() + " =? " + a[i][0].trim());

            if (a[i][0].trim().equals(MY_IP) && Integer.parseInt(a[i][1].trim()) == (PORT_NUMBER)) {
                idx = i;
                break;
            }
        }

        //System.out.println("I AM LOCATED IN INDEX " + idx);
        myIdxInA = idx + 1;
        if (myIdxInA == knownRingos.size()) {
            myIdxInA = 0;
        }

        String send = "C " + MY_IP.trim() + " " + PORT_NUMBER;
        byte[] sendData = send.getBytes();

        String sendingToIP = a[myIdxInA][0].trim();
        //System.out.println("THIS EVENTUALLY GETS FUCKED UP " + (a[myIdxInA][0]));
        //System.out.println("THIS EVENTUALLY GETS FUCKED UP " + (a[myIdxInA][1]));
        int sendingToPort = Integer.parseInt(a[myIdxInA][1]);

        //System.out.println("Checking to see if " + sendingToIP + ":" + sendingToPort);

        try {
            DatagramPacket infoPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(sendingToIP), sendingToPort);
            ds.send(infoPacket);
        } catch (Exception e) {
            //e.printStackTrace();
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

        String ret = "MR " + RTT + " " + ip + " " + PORT_NUMBER;
        byte[] send = ret.getBytes();

        try {
            DatagramPacket dp = new DatagramPacket(send, send.length, InetAddress.getByName(ip), Integer.parseInt(port.trim()));
            ds.send(dp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printKnownRingos() {
        for (Map.Entry<ringoAddr, Integer> otherRingos : knownRingos.entrySet()) {
            ringoAddr ra = otherRingos.getKey();
            String ip = ra.getIP();
            int port = ra.getPort();
            int id = ra.getID();
            //System.out.println("my ip " + ip + " my port " + port + " id " + id);

            Integer ping = otherRingos.getValue();
            System.out.println("Ringo @ " + ip + ":" + port + " with ping ~" + ping + " ms. with of type " + ra.getType());
        }
    }

    public static void printOfflineRingos() {
        for (Map.Entry<ringoAddr, Integer> otherRingos : offlineRingos.entrySet()) {
            ringoAddr ra = otherRingos.getKey();
            String ip = ra.getIP();
            int port = ra.getPort();
            int id = ra.getID();
            //System.out.println("my ip " + ip + " my port " + port + " id " + id);

            Integer ping = otherRingos.getValue();
            System.out.println("Ringo @ " + ip + ":" + port + " with ping ~" + ping + " ms.");
        }
    }




    //    final static int NO_PARENT = -1;
    public static void makeIdealMatrix(String[][] graph, int src, boolean showOutput) {
        //TODO: Construct shortest path among all Ringos in the network.
        //System.out.println(matrix.toString());
        int dist[] = new int[knownRingos.size()]; // The output array. dist[i] will hold
        // the shortest distance from src to i
        int[][] newMatrix = new int[knownRingos.size()][knownRingos.size()];
        intMatrix = new int[knownRingos.size()][knownRingos.size()];

        for (int i = 0; i < knownRingos.size(); i++) {
            //System.out.print(i + ") [");
            for (int j = 0; j < knownRingos.size(); j++) {
                //System.out.print(graph[i][j] + "     ");
                intMatrix[i][j] = Integer.parseInt(graph[i][j]);
                newMatrix[i][j] = intMatrix[i][j];
            }
        }
        for (int i = 0; i < knownRingos.size(); i++) {
            //System.out.print(i + ") [");
            for (int j = 0; j < knownRingos.size(); j++) {
                intMatrix[i][j] = (newMatrix[i][j] + newMatrix[j][i]) / 2;
            }
        }
    }


    static void printMatrix(int dist[][]) {
        System.out.println("\n");
        for (int i=0; i<knownRingos.size(); ++i) {
            for (int j=0; j<knownRingos.size(); ++j) {
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

