import java.io.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.EOFException;


class LineType {
    int v, time;
    long tag;
    byte[] data;

    public LineType(int E, int B) {
        v = 0;
        time = 0;
        tag = 0;
        data = new byte[B];
    }
}

class CacheType {
    int s, E, b, nH, nM, nE, d, a;
    LineType[][] way;

    public CacheType(int S, int E, int B) {
        s = S;
        b = B;
        nH = nM = nE = 0;
        a = (int) ((int)Math.log(B)/Math.log(2));
        d = E*B;
        way = new LineType[S][E];
        for(int i=0;i<S;i++) 
            for(int j=0;j<E;j++) {
                way[i][j] = new LineType(E,B);
            }
    }
}

public class aaa {

    static byte[] ram;

    public static void main(String[] args) {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        //parse inputs
        int[] params = new int[4];
        try {
            String[] inp = input.readLine().split(" ");
            for(int i=0;i<4;i++)
                params[i]=Integer.parseInt(inp[i]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        readRam();

        CacheType[] cache = new CacheType[2];
        cache[0] = new CacheType(params[0],params[1],params[2]);
        cache[1] = new CacheType(params[3],1,params[2]);
        String line;

        while(true){
            try {
                if((line = input.readLine()) == null)
                    break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            String[] inp = line.split(" ");
            char op = inp[0].charAt(0);
            long address = Long.parseLong(inp[1]);
            int size = Integer.parseInt(inp[2]);
            if(op == 'I')
                continue;
            if(op == 'M') {
                accessCache(address,size,'R',cache);
                accessCache(address,size,'W',cache);
            }
            else {
                accessCache(address,size,op,cache);
            }
        }

        System.out.println("Hits: "+cache[0].nH+" Misses: "+cache[0].nM+" Evictions: "+cache[0].nE);
    }

    /**
     * @param address
     * @param size
     * @param op
     * @param cache
     */
    static void accessCache(long address, int size, char op, CacheType[] cache) {
        if(cache.length!=2 && op=='W')
            return;
        if(cache[0].s==0 && cache[0].E==1) {
            if(op=='R') {
                cache[0].nH++;
            }
            else {
                cache[0].nH++;
                cache[0].nE++;
            }           
            return;
        }
        cache[0].a = (int) ((int)Math.log(cache[0].b)/Math.log(2));
        cache[1].a = (int) ((int)Math.log(cache[1].b)/Math.log(2));
        long sbit = (long)((address >> cache[0].a) & ((1 << cache[0].s) - 1));
        if(cache.length==2) {
            accessCache(address,cache[1].d,'R',cache);
            accessCache(address,cache[1].d,'W',cache);
        }
        if(cache[0].s==0 && cache[0].E==1) {
            if(op=='R') {
                cache[0].nH++;
            }
            else {
                cache[0].nH++;
                cache[0].nE++;
            }           
            return;
        }

        if(cache.length==2) {
            long mbit = address/cache[0].b;
            accessCache(mbit,1,'R',cache);
        }

        if(op == 'W') {
            accessCache(address,size,'R',cache);
        }

        if(size<cache[0].b) {
            byte[] data = new byte[cache[0].b];
            int diff = cache[0].b - size;
            Arrays.fill(data,(byte)0);
            for(int i=0;i<size;i++) {
                data[diff+i] = ram[(int)address+i];
            }
            size = cache[0].b;
        }
        else {
            byte[] data = new byte[size];
            Arrays.fill(data,(byte)0);
            for(int i=0;i<size;i++) {
                data[i] = ram[(int)address+i];
            }
        }

        if(cache.length==2) {
            if(op=='R') {
                if(cache[1].way[(int)(address/cache[0].b)][0].v==0) {
                    return;
                }
                else {
                    for(int i=0;i<size;i++) {
                        ram[(int)(address+i)] = cache[0].way[(int)sbit][0].data[(int)((address%cache[0].b)+i)];
                    }
                }
                return;
            }
            else {
                for(int i=0;i<size;i++) {
                    cache[0].way[(int)sbit][0].data[(int)((address%cache[0].b)+i)] = ram[(int)(address+i)];
                }
                cache[0].way[(int)sbit][0].v = 1;
                cache[0].way[(int)sbit][0].tag = (long)(address/cache[0].b);
                updateTime(cache);
                return;
            }
        }

        if(address+size-cache[0].b >= ram.length) {
            size = ram.length-(int)address+cache[0].b;
        }
        byte[] data;
        if(cache[0].E == 1) {
            if(cache[0].way[(int)sbit][0].v == 1 && cache[0].way[(int)sbit][0].tag == ((long)address/cache[0].b)) {
                cache[0].nH++;
                if(op == 'S') {
                    for(int i=0;i<size;i++) {
                        cache[0].way[(int)sbit][0].data[(int)((address%cache[0].b)+i)] = data[i];
                    }
                }
                return;
            }
            else {
                cache[0].nM++;
                if(cache[0].way[(int)sbit][0].v == 1) {
                    cache[0].nE++;
                }
                cache[0].way[(int)sbit][0].v = 1;
                cache[0].way[(int)sbit][0].tag = (long)(address/cache[0].b);
                if(op == 'S') {
                    for(int i=0;i<size;i++) {
                        cache[0].way[(int)sbit][0].data[(int)((address%cache[0].b)+i)] = data[i];
                    }
                }
                else {
                    for(int i=0;i<size;i++) {
                        cache[0].way[(int)sbit][0].data[(int)((address%cache[0].b)+i)] = ram[(int)(address+i)];
                    }
                }               
                return;
            }
        }

        if(cache[0].E == -1) {
            cache[0].nH++;
            if(op == 'S') {
                for(int i=0;i<size;i++) {
                    cache[0].way[(int)sbit][0].data[(int)((address%cache[0].b)+i)] = data[i];
                }
            }
            return;
        }

        if(cache[0].E > 1) {
            cache[0].a = (int) ((int)Math.log(cache[0].b)/Math.log(2));
            cache[1].a = (int) ((int)Math.log(cache[1].b)/Math.log(2));
            if(op == 'S') {
                for(int i=0;i<cache[0].E;i++) {
                    if(cache[0].way[(int)sbit][i].v == 1 && cache[0].way[(int)sbit][i].tag == ((long)address/cache[0].b)) {
                        cache[0].way[(int)sbit][i].time = 0;
                        cache[0].nH++;
                        for(int j=0;j<size;j++) {
                            cache[0].way[(int)sbit][i].data[(int)((address%cache[0].b)+j)] = data[j];
                        }
                        updateTime(cache);
                        return;
                    }
                }
            }
            cache[0].nM++;
            int emptyIndex = -1;
            int minTime = Integer.MAX_VALUE;
            int minTimeIndex = 0;
            int hitIndex = -1;
            for(int i=0;i<cache[0].E;i++) {
                if(cache[0].way[(int)sbit][i].v == 0) {
                    emptyIndex = i;
                }
                else {
                    if(cache[0].way[(int)sbit][i].tag == ((long)address/cache[0].b)) {
                        hitIndex = i;
                    }
                    else {
                        if(cache[0].way[(int)sbit][i].time < minTime) {
                            minTime = cache[0].way[(int)sbit][i].time;
                            minTimeIndex = i;
                        }
                    }
                }
            }
            if(hitIndex != -1) {
                cache[0].way[(int)sbit][hitIndex].time = 0;
                if(op == 'S') {
                    for(int i=0;i<size;i++) {
                        cache[0].way[(int)sbit][hitIndex].data[(int)((address%cache[0].b)+i)] = data[i];
                    }
                }
                return;
            }
            else {
                if(emptyIndex != -1) {
                    cache[0].nE = cache[0].nE<1?1:cache[0].nE+1;
                    cache[0].way[(int)sbit][emptyIndex].v = 1;
                    cache[0].way[(int)sbit][emptyIndex].time = 0;
                    cache[0].way[(int)sbit][emptyIndex].tag = (long)(address/cache[0].b);
                    if(op == 'S') {
                        for(int i=0;i<size;i++) {
                            cache[0].way[(int)sbit][emptyIndex].data[(int)((address%cache[0].b)+i)] = data[i];
                        }
                    }
                    else {
                        for(int i=0;i<size;i++) {
                            cache[0].way[(int)sbit][emptyIndex].data[(int)((address%cache[0].b)+i)] = ram[(int)(address+i)];
                        }
                    }
                    updateTime(cache);
                    return;
                }
                else {
                    cache[0].nE++;
                    cache[0].way[(int)sbit][minTimeIndex].v = 1;
                    cache[0].way[(int)sbit][minTimeIndex].time = 0;
                    cache[0].way[(int)sbit][minTimeIndex].tag = (long)(address/cache[0].b);
                    if(op == 'S') {
                        for(int i=0;i<size;i++) {
                            cache[0].way[(int)sbit][minTimeIndex].data[(int)((address%cache[0].b)+i)] = data[i];
                        }
                    }
                    else {
                        for(int i=0;i<size;i++) {
                            cache[0].way[(int)sbit][minTimeIndex].data[(int)((address%cache[0].b)+i)] = ram[(int)(address+i)];
                        }
                    }
                    updateTime(cache);
                    return;
                }
            }
        }
    }

    static void updateTime(CacheType[] c) {
        for(int i=0; i< c[0].way.length;i++)
            for(int j=0; j< c[0].way[0].length;j++) {
                if(c[0].way[i][j].v==1) {
                    c[0].way[i][j].time++;
                }
                if(c.length==2 && c[1].way[0][0].v==1) {
                    c[1].way[0][0].time++;
                }
                if(c.length==2 && c[1].way[c[1].way.length-1][c[1].way[0].length-1].v==1) {
                    c[1].way[c[1].way.length-1][c[1].way[0].length-1].time++;
                }
            }
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
            } catch (EOFException eofe) {
                accessBinFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}