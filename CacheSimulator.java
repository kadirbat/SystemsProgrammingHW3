import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

public class CacheSimulator {

	static int s = 0;
	static int E = 0;
	static int b = 0;
	static int S = 0;
	static int B = 0;
	static int indexBits = 0;
	static int blockBits = 0;
	static int tagBits = 0;
	
	static int hits = 0;
	static int misses = 0;
	static int evictions = 0;
	static byte[] ram;
	
	public static void main(String[] args) throws IOException {
		// command line arguments: ./your_simulator -s 1 -E 2 -b 3 -t test.trace
		// 8 arguments
		if (args.length != 8) {
			System.out.println("Number of arguments must be 8!");
			System.out.println("Example: ./your_simulator -s 1 -E 2 -b 3 -t test1.trace");
			System.exit(1);
		}
		
		int s = 0;
		int E = 0;
		int b = 0;
		String trace = "";
		
		for (int i = 0; i < 8; i = i + 2) {
			String option = args[i];
			String optionValue = args[i+1];

			switch (option) {
			// intialize s, E, b and trace file respectively
			case "-s": s = Integer.parseInt(optionValue); break;
			case "-E": E = Integer.parseInt(optionValue); break;
			case "-b": b = Integer.parseInt(optionValue); break;
			case "-t": trace = optionValue; break;
			}
		}
		// 2^b = B etc.
		S = (int) Math.pow(2, s);
		B = (int) Math.pow(2, b);
		indexBits = (int) (Math.log(S) / Math.log(2));
		blockBits = (int) (Math.log(B) / Math.log(2));
		// 32 since address field specifies 32-bit hexadecimal memory address
		tagBits = 32 - indexBits - blockBits;
		Line[][] cache = new Line[S][E];
		
		// i will be for set indices, and j for line indices
		for (int i = 0; i < S; i++) {
			for (int j = 0; j < E; j++) {
				cache[i][j] = new Line();
			}
		}
		
		// buffered used to get the whole line
		FileReader readTraceFile = new FileReader(trace);
		BufferedReader readTrace = new BufferedReader(readTraceFile);
		
		// read RAM.dat
		readRam();

		// read each trace line and apply the operation
		String lineTrace = readTrace.readLine();
		while (lineTrace != null) {
			// split when comma or whitespace shows up
			// in the form of regular expression
			String[] traceTokens = lineTrace.split("[\\s,]+");
			String operation = traceTokens[0];
			String address = traceTokens[1];
			String data = "";
			int size = Integer.parseInt(traceTokens[2]);

			//System.out.println(traceTokens.length);
			if (traceTokens.length == 4) {
				data = traceTokens[3];
			}

			// print steps
			// print miss, hit, evictions
			switch (operation) {
			// Load
			case "L":
				System.out.println(operation + " " + address + ", " + size);
				if (size > B) {
					System.out.println("size requested is larger than cache block size!");
					System.exit(1);
				}
				dataLoad(cache, address, size);

				break;
			// store
			case "S":
				System.out.println(operation + " " + address + ", " + size + ", " + data);
				if (size > B) {
					System.out.println("size requested is larger than cache block size!");
					System.exit(1);
				}
				dataStore(cache, address, size, data);

				break;
			// modify -> load then store
			case "M": 
				System.out.println(operation + " " + address + ", " + size + ", " + data);
				if (size > B) {
					System.out.println("size requested is larger than cache block size!");
					System.exit(1);
				}
				dataLoad(cache, address, size);
				dataStore(cache, address, size, data);

				break;
			default:
				System.out.println("Invalid Operation!");
				System.exit(1);
			}
			lineTrace = readTrace.readLine();
		}
	}
	
	public static void dataLoad(Line[][] cache, String address, int size) {
		// hexadecimal address to a long value (16 indicates base 16)
        long decimalAddress = Long.parseLong(address, 16);

        // getting the tag, index, and block offset (<<< does not maintain sign bit)
        int tag = (int) (decimalAddress >>> (indexBits + blockBits));
        int index = (int) ((decimalAddress >>> blockBits) & ((1 << indexBits) - 1));
        int blockOffset = (int) (decimalAddress & ((1 << blockBits) - 1));
        boolean checkHit = false;
        
        // found in cache
        for (Line[] set : cache) {
        	for (Line line : set) {
        		if (line.getTag() == tag && line.getValid() == 1) {
        			System.out.println("  Hit");
                    hits++;
                    checkHit = true;
                    line.setTime(System.currentTimeMillis());
                    break;
                }
        	}
        }
        if (checkHit == false) {
        	System.out.println("  Miss");
        	System.out.println("  Place in cache");
        	misses++;
        	
        	// find empty line in cache or a line that has least time to replace from RAM
        	Line victimLine = cache[0][0];
        	boolean foundLine = false;
        	for (int i = 0; i < cache.length; i++) {
        		for (int j = 0; j < cache[i].length; j++) {
        			// found empty
        			if (cache[i][j].getValid() == 0 && cache[i][j].getTag() == 0) {
        				victimLine = cache[i][j];
        				foundLine = true;
        				break;
        			}
        			// found less time (FIFO)
        			if (cache[i][j].getTime() < victimLine.getTime()) {
        				victimLine = cache[i][j];
        			}
        		}
        		if (foundLine == true) {
        			break;
        		}
        	}
        	
        	if (victimLine.getValid() == 1) {
        		evictions++;
        		
        	}
        	
        	// update line evicted
        }

	}

	public static void dataStore(Line[][] cache, String address, int size, String data) {

	}

	public static void readRam() {
		try {
			// to read large array of bytes
            RandomAccessFile accessBinFile = new RandomAccessFile("RAM.dat", "r");
            long lenRam = 2000000000;
            // to beginning of file
            accessBinFile.seek(0); 
            ram = new byte[(int) lenRam];
            // reads bytes from file into ram bytes array
            accessBinFile.readFully(ram);
            accessBinFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}

class Line {
	// instance variables
	private int tag;
	private short valid;
	private long time;
	private byte[] data;
	
	public Line() {
		this.tag = 0;
		this.valid = 0;
		this.time = 0;
		this.data = new byte[CacheSimulator.B];
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

	public short getValid() {
		return valid;
	}

	public void setValid(short valid) {
		this.valid = valid;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] copyToData, int startPos, int length) {
        System.arraycopy(copyToData, startPos, data, 0, length);
    }
	
}