import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class LineOfData {
    public byte[] data;
    public int time;
    public int tag;
    public boolean v;

    public LineOfData(int dataSize) {
        data = new byte[dataSize];
        time = 0;
        tag = 0;
        v = false;
    }
}

class CacheType {
    public int b;
    public int E;
    public int s;
    public int nH;
    public int nM;
    public int nE;
    public LineOfData[][] way;

    public CacheType(int s, int E, int b) {
        this.s = s;
        this.E = E;
        this.b = b;
        nH = 0;
        nM = 0;
        nE = 0;
        int S = 1 << s;
        way = new LineOfData[S][E];

        for (int m = 0; m < S; m++) {
            for (int n = 0; n < E; n++) {
                way[m][n] = new LineOfData(1 << b);
            }
        }
    }
}

public class CacheSimulation {
    private static byte[] RAM;
    private static CacheType cacheL1D;
    private static CacheType cacheL1I;
    private static CacheType cacheL2;

    private static CacheType createCache(int s, int E, int b) {
        CacheType cache = new CacheType(s, E, b);
        int S = 1 << s;

        for (int m = 0; m < S; m++) {
            for (int n = 0; n < E; n++) {
                cache.way[m][n].v = false;
                cache.way[m][n].time = 0;
                cache.way[m][n].tag = 0;
            }
        }

        return cache;
    }

    private static void procedure(CacheType cache, long adres, char op, int size, byte[] data) {
        long sbit = 0;
        if (cache.s != 0) {
            sbit = (adres >> cache.b);
            sbit = (sbit << (64 - cache.s));
            sbit = (sbit >> (64 - cache.s));
        }

        long tag = (adres >> (cache.b + cache.s));

        for (int i = 0; i < cache.E; i++) {
            if (cache.way[(int) sbit][i].tag == tag && cache.way[(int) sbit][i].v) {
                cache.nH++;

                for (int j = 0; j < cache.E; j++) {
                    cache.way[(int) sbit][j].time--;
                }

                cache.way[(int) sbit][i].time = cache.E;

                if (op == 'S') {
                    changeRAM(adres, size, data);
                }

                for (int x = 0; x < size; x++) {
                    cache.way[(int) sbit][i].data[x] = RAM[(int) (adres + x)];
                }

                return;
            }
        }

        cache.nM++;

        int pos = 0;
        for (int j = 0; j < cache.E; j++) {
            if (cache.way[(int) sbit][j].time < cache.way[(int) sbit][pos].time) {
                pos = j;
            }
        }

        for (int j = 0; j < cache.E; j++) {
            cache.way[(int) sbit][j].time--;
        }

        cache.way[(int) sbit][pos].time = cache.E;

        if (op == 'S') {
            changeRAM(adres, size, data);
        } else {
            for (int x = 0; x < size; x++) {
                cache.way[(int) sbit][pos].data[x] = RAM[(int) (adres + x)];
            }
        }

        if (cache.way[(int) sbit][pos].v) {
            cache.nE++;
        }

        cache.way[(int) sbit][pos].tag = (int) tag;
        cache.way[(int) sbit][pos].v = true;
    }

    private static void readTrace(String traceFile) {
        try (BufferedReader br = new BufferedReader(new FileReader(traceFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                char c = line.charAt(0);
                String[] parts = line.substring(2).split(",");
                long adres = Long.parseLong(parts[0], 16);
                int size = Integer.parseInt(parts[1]);

                switch (c) {
                    case 'I':
                        procedure(cacheL1I, adres, 'I', size, null);
                        procedure(cacheL2, adres, 'I', size, null);
                        break;
                    case 'L':
                        procedure(cacheL1D, adres, 'L', size, null);
                        procedure(cacheL2, adres, 'L', size, null);
                        break;
                    case 'S':
                        byte[] data = new byte[size];
                        String[] dataParts = parts[2].split("");
                        for (int i = 0; i < size; i++) {
                            data[i] = (byte) Integer.parseInt(dataParts[i], 16);
                        }
                        procedure(cacheL1D, adres, 'S', size, data);
                        procedure(cacheL2, adres, 'S', size, data);
                        break;
                    case 'M':
                        data = new byte[size];
                        dataParts = parts[2].split("");
                        for (int i = 0; i < size; i++) {
                            data[i] = (byte) Integer.parseInt(dataParts[i], 16);
                        }
                        procedure(cacheL1D, adres, 'L', size, null);
                        procedure(cacheL2, adres, 'L', size, null);
                        procedure(cacheL1D, adres, 'S', size, data);
                        procedure(cacheL2, adres, 'S', size, data);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readRAMFile() {
        try {
            String filePath = "RAM.dat";
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            long filelen = 2147483647; // Adjust this value according to the actual file size

            RAM = new byte[(int) filelen];
            br.read(RAM, 0, (int) filelen);
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void changeRAM(long adres, int size, byte[] data) {
        for (int i = 0; i < size; i++) {
            RAM[(int) (adres + i)] = data[i];
        }
    }

    private static void printOutput() {
        try {
            String outputPath = "output_cache.txt";
            FileWriter fw = new FileWriter(outputPath);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write("L1I-hitts:" + cacheL1I.nH + " L1I-miss:" + cacheL1I.nM + " L1I-evictions:" + cacheL1I.nE + "\n");
            bw.write("L1D-hitts:" + cacheL1D.nH + " L1D-miss:" + cacheL1D.nM + " L1D-evictions:" + cacheL1D.nE + "\n");
            bw.write("L2-hitts:" + cacheL2.nH + " L2-miss:" + cacheL2.nM + " L2-evictions:" + cacheL2.nE + "\n");

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        readRAMFile();
        readArgument(args);
        printOutput();
    }
}