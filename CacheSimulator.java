import java.io.BufferedReader;
import java.io.EOFException;
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
			System.out.println("Example on to write on terminal after compiling:");
			System.out.println("java CacheSimulator -s 1 -E 2 -b 3 -t test.trace");
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
		System.out.println("hits:" + hits + " misses:" + misses + " evictions:" + evictions);
	}
	
	public static void dataLoad(Line[][] cache, String address, int size) {
		// hexadecimal address to a long value (16 indicates base 16)
        long addressDec = Long.parseLong(address, 16);

        // getting the tag, index, and block offset (<<< does not maintain sign bit)
        int tag = (int) (addressDec >>> (indexBits + blockBits));
        int index = (int) ((addressDec >>> blockBits) & ((1 << indexBits) - 1));
        int blockOffset = (int) (addressDec & ((1 << blockBits) - 1));
        boolean checkHit = false;
        
        // found in cache
        for (Line line : cache[index]) {
            if (line.getTag() == tag && line.getValid() == 1) {
                System.out.println("  Hit");
                hits++;
                checkHit = true;
                line.setTime(System.currentTimeMillis());
                break;
            }
        }
        if (checkHit == false) {
        	System.out.println("  Miss");
        	System.out.println("  Place in cache");
        	misses++;
        	
        	// find empty line in cache or a line that has least time to replace from RAM
        	Line victimLine = cache[index][0];
            boolean foundLine = false;
            for (int j = 0; j < cache[index].length; j++) {
                // found empty
                if (cache[index][j].getValid() == 0 && cache[index][j].getTag() == 0) {
                    victimLine = cache[index][j];
                    foundLine = true;
                    break;
                }
                // found less time (FIFO)
                if (cache[index][j].getTime() < victimLine.getTime()) {
                    victimLine = cache[index][j];
                }
        	}
        	
            // could not find empty space and eviction operation is going to happen
        	if (victimLine.getValid() == 1) {
        		evictions++;
        	}
        	// update line evicted
        	copyDataFromRam(victimLine, addressDec, index, tag, blockOffset, size);
        }
	}

	public static void dataStore(Line[][] cache, String address, int size, String data) {
		// hexadecimal address to a long value (16 indicates base 16)
        long addressDec = Long.parseLong(address, 16);

        // getting the tag, index, and block offset (<<< does not maintain sign bit)
        int tag = (int) (addressDec >>> (indexBits + blockBits));
        int index = (int) ((addressDec >>> blockBits) & ((1 << indexBits) - 1));
        int blockOffset = (int) (addressDec & ((1 << blockBits) - 1));
        boolean checkHit = false;
        
        // found in cache then replace data in cache by the given
        for (Line line : cache[index]) {
            if (line.getTag() == tag && line.getValid() == 1) {
                System.out.println("  Hit");
                System.out.println("  Store in cache and RAM");
                hits++;
                checkHit = true;
                line.setTime(System.currentTimeMillis());
                line.setData(data.getBytes(), blockOffset, size);
                break;
            }
        }
        // is not in cache get from RAM then replace data
        if (checkHit == false) {
        	System.out.println("  Miss");
        	System.out.println("  Place in cache");
        	misses++;
        	
        	// find empty line in cache or a line that has least time to replace from RAM
        	Line victimLine = cache[index][0];
            boolean foundLine = false;
            for (int j = 0; j < cache[index].length; j++) {
                // found empty
                if (cache[index][j].getValid() == 0 && cache[index][j].getTag() == 0) {
                    victimLine = cache[index][j];
                    foundLine = true;
                    break;
                }
                // found less time (FIFO)
                if (cache[index][j].getTime() < victimLine.getTime()) {
                    victimLine = cache[index][j];
                }
        	}
        	
            // could not find empty space and eviction operation is going to happen
        	if (victimLine.getValid() == 1) {
        		evictions++;
        	}
        	// update line evicted
        	copyDataFromRam(victimLine, addressDec, index, tag, blockOffset, size);
        	
        	/*   STORE DATA GIVEN !! HOW ?    */
        	
        	
        }
	}

	// copy from RAM to cache line
	public static void copyDataFromRam(Line line, long addressDec, int setIndex, int tag, int blockOffset, int size) {
		// calculate RAM address to load the line data from
        long ramAddress = (long) (tag << (indexBits + blockBits)) | (setIndex << blockBits);
        
        // initialize ramData by a B bytes from RAM
        byte[] ramData = new byte[CacheSimulator.B];
        System.arraycopy(ram, (int) ramAddress, ramData, 0, CacheSimulator.B);

        // update line fields
        line.setTag(tag);
        line.setValid((short) 1);
        line.setTime(System.currentTimeMillis());
        line.setData(ramData, blockOffset, size);
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
            try {
            accessBinFile.readFully(ram);
            }
            catch (EOFException eofe){
            accessBinFile.close();
            }
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