import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
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
    public static boolean calculatedPing = false;
    public static boolean calculatedRing = false;
    public static int num_iters = 0;

    public static int row;
    public static int matrixReplies;


    public static Map<ringoAddr, Integer> knownRingos;
    public static Map<ringoAddr, Integer> matrixRingos;
    public static Map<Integer, Integer> positions = new TreeMap<Integer, Integer>();


    public static Thread checkForPackets;

    public static String[] vector;
    public static String[][] matrix;

    public static int[][] intMatrix;

    public static DatagramSocket ds;

    public static String cmdError = "There was an error in your command line arguments. Try again.";

    static class ringoAddr implements Comparable<ringoAddr>, Comparator<ringoAddr> {
        public String IP_ADDR;
        public int ID;
        public int port;

        public ringoAddr(String ip, int p) {
            IP_ADDR = ip;
            ID = p;
        }

        public ringoAddr(String ip, int id, int p) {
            IP_ADDR = ip;
            this.ID = id;
            this.port = p;
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

        public String toString() {
            return "RINGO: " + IP_ADDR + " ,ID: " + ID + " , PORT NUMBER";
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
            return this.getPort() == ra.getPort();
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
            initialRingoCount = 2;
            RINGOID = ringosOnline - 1;
            knownRingos.put(new ringoAddr("127.0.0.1", 0), PORT_NUMBER);

        }

        matrixRingos.put(new ringoAddr("127.0.0.1", RINGOID, PORT_NUMBER), 99999);


        checkForPackets = new Thread() {
            public void run() {
                while (true) {
                    try {
                        byte[] packetInfo = new byte[1024];
                        DatagramPacket recieve = //Recieves Ping request at this Ringo's Port number.
                                new DatagramPacket(packetInfo, packetInfo.length, InetAddress.getLocalHost(), PORT_NUMBER);
                        ds.receive(recieve);

                        String packetData = new String(packetInfo);
                        //System.out.println("RECIEVED PACKET CONTAINING " + packetData);
                        StringTokenizer token = new StringTokenizer(packetData);

                        String FLAG = token.nextToken().trim();
                        String firstField = token.nextToken().trim();

                        if (FLAG.equals("P")) {
                            String secondField = token.nextToken().trim();
                            //System.out.println("\n\nA new Ringo has requested to ping you.");
                            if (calculatedPing == true) {
                                //System.out.println("PING RECIEVED FOR MATRIX");
                            }

                            recievePing(firstField, secondField);
                        } else if (FLAG.equals("E")) {
                            String secondField = token.nextToken().trim();
                            String thirdField = token.nextToken();
                            acceptRingos(firstField, thirdField, secondField);
                            //System.out.println("A Ringo is exchanging information with you...");

                        } else if (FLAG.equals("PR")) {
                            String secondField = token.nextToken().trim();
                            int ping = Integer.parseInt(firstField);
                            ringosOnline = Integer.parseInt(secondField);

                        } else if (FLAG.equals("PM")) {//PING MATRIX{
                            String timeStamp = firstField;
                            String secondField = token.nextToken().trim();
                            String ipOfSender = secondField;
                            String port = token.nextToken();
                            //System.out.println("PING MATRIX timestamp : " + timeStamp + " ipOfSender : " + ipOfSender + " port " + port);
                            replyToMatrix(timeStamp, ipOfSender, port);
                        } else if (FLAG.equals("MR")) {
                            //populate matrix

                            String timeStamp = firstField;
                            String secondField = token.nextToken().trim();
                            String ipOfSender = secondField;
                            String port = token.nextToken();
                            int pingerTime = Math.abs(Integer.parseInt(timeStamp));
                            matrixRingos.put(new ringoAddr(ipOfSender, RINGOID, Integer.parseInt(port.trim())), pingerTime);
                            //System.out.println("Recieved new RTT");

                        } else if (FLAG.equals("M")) {
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
                            //System.out.println(firstField);
                            //System.out.println(secondField);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //KATIE
                    //System.out.println("My ringos online " + knownRingos.size());
                    if (knownRingos.size() == n && !calculatedPing) {
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
                                String curRingo = "127.0.0.1";
                                //System.out.println("sender ip address " + curRingo);
                                DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), port);
                                try {
                                    num_iters++;
                                    //System.out.println("my iteration number " + num_iters);
                                    ds.send(packet);
                                } catch (Exception e) {
                                    //
                                }
                            } catch (Exception e) {
                            }
                        }
                        //System.out.println("Current Ring = " + Arrays.toString(vector));
                        if (matrixRingos.size() == n) {
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

                byte[] buf = new byte[PACKET_SIZE];

                System.out.println("Sending " + filename);

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

                String[][] a = new String[matrix.length][];
                for (int i = 0; i < matrix.length; i++) {
                    a[i] = Arrays.copyOfRange(matrix[i], n, n + 2);
                }

                for (int i = 0; i < n; i++) {
                    System.out.println("Row/Column " + i + " refers to Ringo " + Arrays.deepToString(a[i]));
                }

            } else if (command.equals("show-ring")) {

                dijkstra(matrix, PORT_NUMBER, true);
                System.out.println();
                int[] lowestInRows = new int[n];
                int counter = 0;
                int j = n - 1;

                for (int i = 0; i < n; i++) {
                    j = j - (n - 2);
                    for (; j < n; j++) {
                        int currentI = i;
                        int currentJ = j;

                        int curValue = intMatrix[i][j];
                        if (curValue == 99999) {
                            break;
                        }

                        System.out.println("The path from ringo " + i + " to Ringo  " + j + " is averaged at " + curValue +" ms.");

                        counter++;
                    }
                }
                System.out.println("For optimal ring formation, start at the sender Ringo, and forward to Reciever according to this data/");
                calculatedRing = true;
            } else if (command.equals("disconnect")) {
                System.out.println("Exiting...");
                System.exit(-1);
            } else if (command.equals("show-ringos")) {
                System.out.println("This Ringo is connected to...");
                if (true) {
                    for (Map.Entry<ringoAddr, Integer> otherRingos : matrixRingos.entrySet()) {
                        ringoAddr ra = otherRingos.getKey();
                        String ip = ra.getIP();
                        int port = ra.getPort();
                        int id = ra.getID();

                        Integer ping = otherRingos.getValue();
                        System.out.println("Ringo @ " + ip + ":" + port + " with ping ~" + ping + " ms.");
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

        for (Map.Entry<ringoAddr, Integer> otherRingos : matrixRingos.entrySet()) {
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
            s = s + " " + InetAddress.getLocalHost().getHostAddress() + " " + PORT_NUMBER;
        } catch (Exception e) {
            //
        }
        byte[] info = s.getBytes();

        for (Map.Entry<ringoAddr, Integer> known : knownRingos.entrySet()) {
            try {

                ringoAddr ra = known.getKey();
                //System.out.println("SENDING VECTOR TO " + ra.getIP() + ":" + known.getValue());
                InetAddress target = InetAddress.getByName(ra.getIP());
                DatagramPacket packet = new DatagramPacket(info, info.length, target, known.getValue());
                try {
                    ds.send(packet);
                    //System.out.println("Sent vector!");
                } catch (Exception e) {
                    e.printStackTrace();
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
            //System.out.println("You have sent the requesting Ringo a reply.\n");
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

                for (Integer port : knownRingos.values()) {
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

    public static void acceptRingos(String ringoIP, String ringoID, String port) {
        try {
            if (!(knownRingos.containsValue(Integer.parseInt(ringoID.trim())))) {
                knownRingos.put(new ringoAddr(ringoIP.trim(), Integer.parseInt(port.trim())), Integer.parseInt(ringoID.trim()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void calculateRing() {
        System.out.println(Arrays.toString(intMatrix));

        int[] lowestInRows = new int[n];
        int counter = 0;
        int j = 2;

        for (int i = 0; i < n; i++) {
            j = j - 1;
            for (; j < n; j++) {

                int curValue = intMatrix[i][j];
                if (curValue == 99999) {
                    break;
                }
                System.out.println("The shortest path from ringo " + i + " to Ringo  " + j + " is " + curValue +" ms.");
                counter++;
            }
        }

        calculatedRing = true;
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

    public static int[][] makeIdealMatrix(String[][] matrix) {
        int[][] newMatrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            //System.out.print(i + ") [");
            for (int j = 0; j < n; j++) {
                System.out.print(matrix[i][j] + "     ");
                newMatrix[i][j] = Integer.parseInt(matrix[i][j]);
//                if (newMatrix[i][j] == 99999){
//                    newMatrix[i][j] = 0;
//                }
                newMatrix[i][j] = (newMatrix[i][j] + newMatrix[j][i]) / 2;
                newMatrix[j][i] = (newMatrix[i][j] + newMatrix[j][i]) / 2;
            }
        }
        printMatrix(newMatrix);
        return newMatrix;
    }
    final static int NO_PARENT = -1;
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
        //printMatrix(intMatrix);

        // shortestDistances[i] will hold the
        // shortest distance from src to i
        int[] shortestDistances = new int[n];

        // added[i] will true if vertex i is
        // included / in shortest path tree
        // or shortest distance from src to 
        // i is finalized
        boolean[] added = new boolean[n];

        // Initialize all distances as 
        // INFINITE and added[] as false
        for (int vertexIndex = 0; vertexIndex < n; vertexIndex++) {
            shortestDistances[vertexIndex] = Integer.MAX_VALUE;
            added[vertexIndex] = false;
        }

        // Distance of source vertex from
        // itself is always 0
        src = 0;
        shortestDistances[src] = 0;

        // Parent array to store shortest
        // path tree
        int[] parents = new int[n];

        // The starting vertex does not 
        // have a parent
        parents[src] = NO_PARENT;

        // Find shortest path for all 
        // vertices
        for (int i = 1; i < n; i++) {

            // Pick the minimum distance vertex
            // from the set of vertices not yet
            // processed. nearestVertex is 
            // always equal to startNode in 
            // first iteration.
            int nearestVertex = -1;
            int shortestDistance = Integer.MAX_VALUE;
            for (int vertexIndex = 0; vertexIndex < n; vertexIndex++) {
                if (!added[vertexIndex] &&
                        shortestDistances[vertexIndex] < shortestDistance) {
                    nearestVertex = vertexIndex;
                    shortestDistance = shortestDistances[vertexIndex];
                }
            }

            // Mark the picked vertex as
            // processed
            added[nearestVertex] = true;

            // Update dist value of the
            // adjacent vertices of the
            // picked vertex.
            for (int vertexIndex = 0; vertexIndex < n; vertexIndex++) {
                int edgeDistance = intMatrix[nearestVertex][vertexIndex];

                if (edgeDistance > 0
                        && ((shortestDistance + edgeDistance) <
                        shortestDistances[vertexIndex])) {
                    parents[vertexIndex] = nearestVertex;
                    shortestDistances[vertexIndex] = shortestDistance + edgeDistance;
                }
            }
        }

        if (showOutput) {
            printSolution(src, shortestDistances, parents);

        }
    }


        // A utility function to print
        // the constructed distances
        // array and shortest paths
        static void printSolution(int startVertex,
                                          int[] distances,
                                          int[] parents)
        {
            int nVertices = distances.length;
            System.out.print("Vertex\t Distance\tPath");

            for (int vertexIndex = 0; vertexIndex < nVertices; vertexIndex++)
            {
                if (vertexIndex != startVertex)
                {
                    System.out.print("\n" + startVertex + " -> ");
                    System.out.print(vertexIndex + " \t\t ");
                    System.out.print(distances[vertexIndex] + "\t\t");
                    printPath(vertexIndex, parents);
                }
            }
        }

        // Function to print shortest path
        // from source to currentVertex
        // using parents array
        private static void printPath(int currentVertex,
                                      int[] parents)
        {
            // Base case : Source node has
            // been processed
            if (currentVertex == NO_PARENT)
            {
                return;
            }
            printPath(parents[currentVertex], parents);
            System.out.print(currentVertex + " ");
        }

    final static int INF = 99999;

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

    //TAKEN FROM JAVA2S
    public static byte[] convertIntegersToBytes (int[] integers) {
        if (integers != null) {
            byte[] outputBytes = new byte[integers.length * 4];

            for(int i = 0, k = 0; i < integers.length; i++) {
                int integerTemp = integers[i];
                for(int j = 0; j < 4; j++, k++) {
                    outputBytes[k] = (byte)((integerTemp >> (8 * j)) & 0xFF);
                }
            }
            return outputBytes;
        } else {
            return null;
        }
    }
}

