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

    public static String IP_ADDR; //TODO: Change from Local


    public static int[] vector;
    public static int[][] matrix;

    public static DatagramSocket ds;



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
        System.out.println("******************************");
        System.out.println("*Ringo successfully initialized.");
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
        System.out.println("****************************");

        try {
            ds = new DatagramSocket(PORT_NUMBER);
        } catch (Exception e) {
            e.printStackTrace();
        }



        if (!isFirst) {
            getPingFromPOC();
        }

        initializeVector();
        initializeMatrix();


        Scanner scan = new Scanner(System.in);

        while (true) {
            //TODO: Put in thread, put ping requests in new thread.
            try {
                byte[] date = new byte[1024];
                DatagramPacket recieve = new DatagramPacket(date, date.length, InetAddress.getLocalHost(), PORT_NUMBER);
                ds.receive(recieve);
                System.out.println("Recieved Packet!");
                long timeStamp = 0;
                for (int i = 0; i < date.length; i++)
                {
                    timeStamp = (timeStamp << 8) + (date[i] & 0xff);
                }
                Date now = new Date();
                long ret = now.getTime();
                long ONEWAYTRIP = ret - timeStamp;
                byte[] RTT = ("" + (ONEWAYTRIP * 2)).getBytes();

                //TODO: Incorporate port number in byte array to forward.
                DatagramPacket send = new DatagramPacket(RTT, 2, InetAddress.getLocalHost(), 10292);
                ds.send(send);
                System.out.println("Sent Packet!");
            } catch (Exception e) {
                e.printStackTrace();
            }

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
            } else {
                System.out.println("Command Not Recognized. Valid commands include: \n");
                System.out.println("offline <seconds>");
                System.out.println("send <filename>");
                System.out.println("show-matrix");
                System.out.println("show-ring");
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
    }

    public static void getPingFromPOC() {
        System.out.println("GETTING PING FROM POC");

        Date now = new Date();
        long msSend = now.getTime();
        byte[] buf = new byte[1024];
        String ms = msSend + "";
        buf = ms.getBytes();
        try {

            InetAddress poc = InetAddress.getByName(POC_NAME);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, poc, POC_PORT);
            try {
                ds.send(packet);
                DatagramPacket recieve = new DatagramPacket(new byte[2], 2);
                ds.receive(recieve);


                System.out.println("Ping estimated at : " + new String(recieve.getData()) + " ms.");
            } catch (IOException e){
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void pointOfContact() {
        //TODO: Attach this Ringo to any other Ringos in the network.
        //TODO: Obtain list of all other Ringos in the network.
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

