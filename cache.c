
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

unsigned char *RAM;
void changeRAM(long long adres,int size,unsigned char*data);
struct cacheType{
    int b;
    int E;
    int s;
    int nH;
    int nM;
    int nE;
    struct lineOfData** way;
} *cacheL1D,*cacheL1I,*cacheL2;


struct lineOfData {
    unsigned char *data;
    int time;
    int tag;
    int v;
};


struct cacheType* createCache(int s,int E,int b) {
    struct cacheType* cache = malloc(sizeof(struct cacheType));

    cache->s = s;
    cache->E = E;
    cache->b = b;
    cache->nH = 0;
    cache->nM = 0;
    cache->nE = 0;

    int S = 1 << cache->s;

    cache->way = malloc(sizeof(struct lineOfData *) * S);

    int m=0;
    int n=0;

    for (m = 0; m < S;m++) {
        cache-> way[m] = malloc(sizeof(struct lineOfData) * cache->E);
    }

    for (m= 0; m< S; ++m)
        for (n=0; n< cache->E;n++){
            cache->way[m][n].v = 0;
            cache->way[m][n].time = 0;
            cache->way[m][n].tag = 0; 
            cache->way[m][n].data = malloc(sizeof(unsigned char)*(1<<(cache->b))); 
            
            
        }
   
   
    return cache;
    
}

void procedure(struct cacheType** cache, long long adres,char op,int size,unsigned char*data) {
	unsigned long long sbit=0;
	if((*cache)->s!=0){
     sbit=(adres >> ((*cache)->b));
	sbit=(sbit << (64-(*cache)->s));
	sbit=(sbit >> (64-(*cache)->s));
    }
    

    long tag = (adres >> ((*cache)->b + (*cache)->s));
    
    int i ;
    
    for (i=0; i<(*cache)->E; i++){
		
		//checkes hit 
        if ((*cache)->way[sbit][i].tag == tag && (*cache)->way[sbit][i].v){
            
            (*cache)->nH ++;
            int j;	
	        for (j = 0; j < (*cache)->E; j++)
		        (*cache)->way[sbit][j].time--;
           
            (*cache)->way[sbit][i].time = (*cache)->E;
            
            int x;
            
        
        if(op=='S'){
			changeRAM(adres,size,data);
			}
			
			for(x=0;x<size;x++)
            (*cache)->way[sbit][i].data[x] = RAM[adres+x];
				
				
			
			
            return;
        }
    }
    
    //if not a hit then its a miss 
    
    (*cache)->nM++;
	 int x;
       
        
    //updating time
    int j,pos = 0;
    for (j = 0; j < (*cache)->E; j++){
        if ((*cache)->way[sbit][j].time < (*cache)->way[sbit][pos].time)
            pos = j;
    }
    for (j = 0; j < (*cache)->E; j++){
        (*cache)->way[sbit][j].time--;
    }
    (*cache)->way[sbit][pos].time = (*cache)->E;

         if(op=='S'){
			changeRAM(adres,size,data);
			}
			else{
			for(x=0;x<size;x++)
            (*cache)->way[sbit][pos].data[x] = RAM[adres+x];
          }
        
    //checkes if eviction
    if ((*cache)->way[sbit][pos].v){
       
        (*cache)->nE++;	
	

	}
	(*cache)->way[sbit][pos].tag = tag;
    (*cache)->way[sbit][pos].v = 1;
}




void readTrace(FILE * trace) {
    
    char c;
    while (fscanf(trace,"%c",&c) != EOF){
       
        long long adres;
		int size;
		unsigned char *data;
		
		switch (c){
			case 'I':
			    fscanf(trace,"%llx,%d",&adres,&size);
			    procedure(&cacheL1I,adres,'I',size,NULL);
                procedure(&cacheL2,adres,'I',size,NULL);
                
			    break;
			
            case 'L' :
                fscanf(trace,"%llx,%d",&adres,&size);
                procedure(&cacheL1D,adres,'L',size,NULL);
                procedure(&cacheL2,adres,'L',size,NULL);
				break;
				
            case 'S' :
            
                 fscanf(trace,"%llx,%d,",&adres,&size);
                data=malloc(sizeof(unsigned char)*size);
                int x;
                for(x=0;x<size;x++)
                fscanf(trace,"%x",&(data[x]));
                procedure(&cacheL1D,adres,'S',size,data);
                procedure(&cacheL2,adres,'S',size,data);
				break;  
				          
			case 'M' :
                fscanf(trace,"%llx,%d,",&adres,&size);
                data=malloc(sizeof(unsigned char)*size);
                for(x=0;x<size;x++)
                fscanf(trace,"%x",&(data[x]));
                procedure(&cacheL1D,adres,'L',size,NULL);
                procedure(&cacheL2,adres,'L',size,NULL);
                procedure(&cacheL1D,adres,'S',size,data);
                procedure(&cacheL2,adres,'S',size,data);
				break;            
        }
    }
    fclose(trace);
}

void readArgument(int argc,char* argv[]){
	FILE* trace;
    
    int s1 = 0,E1 = 0,b1 = 0;
    int s2 = 0,E2 = 0,b2 = 0;
    int m;
    
    for (m=0;m<argc; m++){
        if (strcmp(argv[m],"-L1s") == 0){
            m++;
            s1 = atoi(argv[m]);
        } 
        else if (strcmp(argv[m],"-L1E") == 0){
            m++;
            E1 = atoi(argv[m]);
        } 
        else if (strcmp(argv[m],"-L1b") == 0){
            m++;
            b1 = atoi(argv[m]);

        } 
        else if (strcmp(argv[m],"-L2s") == 0){
            m++;
            s2 = atoi(argv[m]);
        } 
        else if (strcmp(argv[m],"-L2E") == 0){
            m++;
            E2 = atoi(argv[m]);
        } 
        else if (strcmp(argv[m],"-L2b") == 0){
            m++;
            b2 = atoi(argv[m]);
        } 
        else if (strcmp(argv[m],"-t") == 0){
            m++;
            trace = fopen(argv[m],"r");
        } 
    }
    cacheL1D = createCache(s1,E1,b1);
    cacheL1I = createCache(s1,E1,b1);
    cacheL2  = createCache(s2,E2,b2);
    readTrace(trace);
}

void readRAMFile(){

    FILE *fileptr;
    
     long  filelen;

    fileptr = fopen("RAM.dat", "rb");  // Open the file in binary mode
    fseek(fileptr, 0, SEEK_END);          // Jump to the end of the file
    filelen = ftell(fileptr);   
      // Get the current byte offset in the file
    rewind(fileptr);                      // Jump back to the beginning of the file
    filelen =2147483647;
    
    RAM = (unsigned char *)malloc(filelen * sizeof(unsigned char)); // Enough memory for the file
    fread(RAM, filelen, 1, fileptr); // Read in the entire file
    fclose(fileptr);
	
	
	
	
	}
	
void changeRAM(long long adres,int size,unsigned char*data){
	int i=0;
	for(i=0;i<size;i++){
		RAM[adres+i]=data[i];
		}
	
	
	
	}

void printOutput(){
	FILE *output=fopen("output_cache.txt","w");
	
	fprintf(output,"L1I-hitts:%d L1I-miss:%d L1I-evictions:%d\n",cacheL1I->nH,cacheL1I->nM,cacheL1I->nE);
	fprintf(output,"L1D-hitts:%d L1D-miss:%d L1D-evictions:%d\n",cacheL1D->nH,cacheL1D->nM,cacheL1D->nE);
	fprintf(output,"L2-hitts:%d L2-miss:%d L2-evictions:%d\n",cacheL2->nH,cacheL2->nM,cacheL2->nE);
	
	fclose(output);	
	}

int main(int argc, char* argv[]){
	
	readRAMFile();
    readArgument(argc,argv);
    printOutput();
    
    return 0;
}
