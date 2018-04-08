import java.util.Stack;
import java.util.*;

//taken from https://github.com/abreun68/TSP-Project/blob/master/src/edu/rowan/algorithms/NearestNeighborSolver.java
public class TSPNearestNeighbor
{
    private int numberOfNodes;
    private Stack<Integer> stack;
    private Stack<Integer> tmpstack;

    public TSPNearestNeighbor() {
        stack = new Stack<Integer>();
        tmpstack = new Stack<Integer>();
    }

    public Stack<Integer> tsp(int adjacencyMatrix[][]) {
        numberOfNodes = adjacencyMatrix[1].length - 1;
        int[] visited = new int[numberOfNodes + 1];
        visited[0] = 1;
        stack.push(1);
        int element, dst = 0, i;
        int min = Integer.MAX_VALUE;
        boolean minFlag = false;
        //System.out.print(0 + "\t");

        while (!stack.isEmpty())
        {
            element = stack.peek();
            i = 1;
            min = Integer.MAX_VALUE;
            while (i <= numberOfNodes)
            {
                if (adjacencyMatrix[element][i] > 1 && visited[i] == 0)
                {
                    if (min > adjacencyMatrix[element][i])
                    {
                        min = adjacencyMatrix[element][i];
                        dst = i;
                        minFlag = true;
                    }
                }
                i++;
            }
            if (minFlag)
            {
                visited[dst] = 1;
                stack.push(dst);
                tmpstack.push(dst);
                //System.out.print(dst + "\t");
                minFlag = false;
                continue;
            }
            stack.pop();
        }
    return tmpstack;
    }
}
