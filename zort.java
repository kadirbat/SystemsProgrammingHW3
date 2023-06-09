import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
class LineOfData {
    // Define the structure of the lineOfData
    // You can add any necessary fields here
}

class CacheType {
    int b;
    int E;
    int s;
    int nH;
    int nM;
    int nE;
    LineOfData[][] way;

    public CacheType(int b, int E, int s, int nH, int nM, int nE) {
        this.b = b;
        this.E = E;
        this.s = s;
        this.nH = nH;
        this.nM = nM;
        this.nE = nE;
        this.way = new LineOfData[s][E]; // Create a 2D array of LineOfData objects
    }
}

public class zort {
 
     CacheType cacheL1D, cacheL1I, cacheL2; // Declare the variables

        // Create instances of the CacheType class and initialize the variables
        cacheL1D = new CacheType( /* Initialize the values for b, E, s, nH, nM, nE */ );
        cacheL1I = new CacheType( /* Initialize the values for b, E, s, nH, nM, nE */ );
        cacheL2 = new CacheType( /* Initialize the values for b, E, s, nH, nM, nE */ );





}
