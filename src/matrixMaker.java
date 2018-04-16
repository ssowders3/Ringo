import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class matrixMaker {
    private String[][] data;
    private String[][] saveData;

    public matrixMaker(String[][] data, int size) {
        int r = data.length;
        int c = data[0].length;
        this.data = new String[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                this.data[i][j] = data[i][j];
            }
        }
    }

    /* convenience method for getting a
       string representation of matrix */
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        for (String[] row : this.data) {
            for (String val : row) {
                sb.append(val);
                sb.append(" ");
            }
            sb.append("\n");
        }

        return (sb.toString());
    }

    public String[][] removeRowsWithValue(final int index, String[][] sourcearr) {

        String destinationarr[][] = new String[this.data.length][this.data[0].length];
        String savearr[][] = new String[this.data.length][this.data[0].length];
        int p = 0;
        //System.out.println(this.data.length);
        for( int i = 0; i < this.data.length; i++) {
            int q = 0;
            if (i == index) {
                continue;
            }
            for( int j = 0; j < this.data[0].length; j++) {
                if (j == index) {
                    continue;
                }
                try {
                    destinationarr[p][q] = sourcearr[i][j];
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //System.out.println(destinationarr[p][q]);
                ++q;
            }
            ++p;
        }

        return destinationarr;
    }
    public String[][] addRowsBack(final int index, String[][] sourcearr) {
        ArrayList<ArrayList<String>> a = new ArrayList<>(sourcearr.length);
        ArrayList<String> blankLine = new ArrayList<>(sourcearr.length * 2 - 1);
        for (int i = 0; i < sourcearr.length * 2 - 1; i++)
        {
            blankLine.add(null);
        }

        for (int i = 0; i < sourcearr.length; i++)
        {
            ArrayList<String> line = new ArrayList<>();
            for (int j = 0; j < sourcearr[i].length; j++)
            {
                line.add(sourcearr[i][j]);
                if (j != sourcearr[i].length - 1)
                    line.add(null);
            }
            a.add(line);
            if (i != sourcearr.length - 1)
                a.add(blankLine);
        }

        for (ArrayList<String> b : a)
        {
            System.out.println(b);
        }
        return sourcearr;
    }

}
