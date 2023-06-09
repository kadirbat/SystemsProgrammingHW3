import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {
    static byte[] RAM;
    static CacheType cacheL1D;
    static CacheType cacheL1I;
    static CacheType cacheL2;

    public static void main(String[] args) {
        readRAMFile();
        readArgument(args);
        printOutput();
    }

    static class CacheType {
        int b;
        int E;
        int s;
        int nH;
        int nM;
        int nE;
        LineOfData[][] way;

        public CacheType(int s, int E, int b) {
            this.s = s;
            this.E = E;
            this.b = b;
            this.nH = 0;
            this.nM = 0;
            this.nE = 0;

            int S = 1 << this.s;
            this.way = new LineOfData[S][this.E];

            for (int i = 0; i < S; i++) {
                for (int j = 0; j < this.E; j++) {
                    this.way[i][j] = new LineOfData();
                    this.way[i][j].v = 0;
                    this.way[i][j].time = 0;
                    this.way[i][j].tag = 0;
                    this.way[i][j].data = new byte[1<<b];
                }
            }
        }
    }

    static class LineOfData {
        byte[] data;
        int time;
        int tag;
        int v;
    }

    static void procedure(CacheType cache, long adres, char op, int size, byte[] data) {
        long sbit=0;
        if(cache.s!=0) {
            sbit=(adres >> ((cache.b)));
            sbit=(sbit << (64-(cache.s)));
            sbit=(sbit >> (64-(cache.s)));
        }
        long tag = (adres >> ((cache.b) + (cache.s)));

        for (int i = 0; i < cache.E; i++) {
            if (cache.way[(int) sbit][i].tag == tag && cache.way[(int) sbit][i].v == 1) {
                cache.nH++;
                for(int j=0; j< cache.E; j++) {
                    cache.way[(int) sbit][j].time--;
                }
                cache.way[(int) sbit][i].time = cache.E;

                if (op == 'S') {
                    changeRAM(adres, size, data);
                }

                for(int x=0; x<size; x++) {
                    cache.way[(int) sbit][i].data[x] = RAM[(int) adres+x];
                }

                return;
            }
        }

        cache.nM++;
        for (int j = 0; j < cache.E; j++){
            if (cache.way[(int) sbit][j].time < cache.way[(int) sbit][0].time) {
                cache.way[(int) sbit][0].time = cache.way[(int) sbit][j].time;
            }
            cache.way[(int) sbit][j].time--;
        }
        cache.way[(int) sbit][0].time = cache.E;

        if (op == 'S') {
            changeRAM(adres, size, data);
        }
        else {
            for(int x = 0; x < size; x++) {
                cache.way[(int) sbit][0].data[x] = RAM[(int) adres+x];
            }
        }

        if (cache.way[(int) sbit][0].v == 1) {
            cache.nE++;
        }

        cache.way[(int) sbit][0].tag = (int) tag;
        cache.way[(int) sbit][0].v = 1;
    }

    static void readTrace(Scanner trace) {
        while (trace.hasNext()) {
            String line = trace.nextLine();
            String[] parts = line.split(",");
            char op = line.charAt(0);
            long adres = Long.parseLong(parts[0].substring(1), 16);
            int size = Integer.parseInt(parts[1].trim());
            byte[] data = null;

            if(op == 'S' || op == 'M') {
                data = new byte[size];
                String[] dataString = parts[2].trim().split(" ");
                for(int i=0; i<size; i++) {
                    data[i] = (byte) Integer.parseInt(dataString[i], 16);
                }
            }

            switch (op) {
                case 'I':
                    procedure(cacheL1I, adres, op, size, null);
                    procedure(cacheL2, adres, op, size, null);
                    break;
                case 'L':
                    procedure(cacheL1D, adres, op, size, null);
                    procedure(cacheL2, adres, op, size, null);
                    break;
                case 'S':
                    procedure(cacheL1D, adres, op, size, data);
                    procedure(cacheL2, adres, op, size, data);
                    break;
                case 'M':
                    procedure(cacheL1D, adres, 'L', size, null);
                    procedure(cacheL2, adres, 'L', size, null);
                    procedure(cacheL1D, adres, 'S', size, data);
                    procedure(cacheL2, adres, 'S', size, data);
                    break;
            }
        }
        trace.close();
    }

    static void readArgument(String[] args) {
        int s1 = 0,E1 = 0,b1 = 0;
        int s2 = 0,E2 = 0,b2 = 0;
        Scanner trace = null;

        for (int i=0; i<args.length; i++) {
            if (args[i].equals("-L1s")) {
                i++;
                s1 = Integer.parseInt(args[i]);
            }
            else if (args[i].equals("-L1E")) {
                i++;
                E1 = Integer.parseInt(args[i]);
            }
            else if (args[i].equals("-L1b")) {
                i++;
                b1 = Integer.parseInt(args[i]);
            }
            else if (args[i].equals("-L2s")) {
                i++;
                s2 = Integer.parseInt(args[i]);
            }
            else if (args[i].equals("-L2E")) {
                i++;
                E2 = Integer.parseInt(args[i]);
            }
            else if (args[i].equals("-L2b")) {
                i++;
                b2 = Integer.parseInt(args[i]);
            }
            else if (args[i].equals("-t")) {
                i++;
                try {
                    File file = new File(args[i]);
                    trace = new Scanner(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        cacheL1D = new CacheType(s1, E1, b1);
        cacheL1I = new CacheType(s1, E1, b1);
        cacheL2  = new CacheType(s2, E2, b2);
        readTrace(trace);
    }

    static void readRAMFile() {
        File file = new File("RAM.dat");

        long length = file.length();

        RAM = new byte[(int) length];
        int offset = 0;

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextInt()) {
                RAM[offset++] = (byte) scanner.nextInt();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static void changeRAM(long adres, int size, byte[] data) {
        for (int i = 0; i < size; i++) {
            RAM[(int) adres+i] = data[i];
        }
    }

    static void printOutput() {
        try {
            java.io.File file = new java.io.File("output_cache.txt");
            java.io.PrintWriter outfile = new java.io.PrintWriter(file);

            outfile.printf("L1I-hitts:%d L1I-miss:%d L1I-evictions:%d\n", cacheL1I.nH, cacheL1I.nM, cacheL1I.nE);
            outfile.printf("L1D-hitts:%d L1D-miss:%d L1D-evictions:%d\n", cacheL1D.nH, cacheL1D.nM, cacheL1D.nE);
            outfile.printf("L2-hitts:%d L2-miss:%d L2-evictions:%d\n", cacheL2.nH, cacheL2.nM, cacheL2.nE);

            outfile.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}