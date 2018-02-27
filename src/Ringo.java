import java.util.Vector;

/**
 * Created by Katie on 2/27/18.
 */
public class Ringo {

    public int num;
    public String ID;
    public final int PORT_NUMBER = 5600;
    public final String IP_ADDR = "127.0.0.1"; //TODO: Change from Local
    public Vector<Double> RTT;
    public Vector<Vector<Double>> RTTMatrix;


    public static void main(String[] args) {
        //TODO: Create command line parsing arguments

        //TODO: Initialize Ringo Timer
        //TODO: Initialize RTT Vector.
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
}
