import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class PageRank{
	
	public static final double lambda = 0.85;
	
	public int[] perplex = new int[4];
	public int count = 1;
	public double currentPerplex;
	
	public Set<String> pagesSet = new HashSet<String>();
	public Set<String> sinkPagesSet = new HashSet<String>();
	
	public Map<String, String> inLinksMap = new HashMap<String, String>();
	public Map<String, Integer> outLinkCountMap = new HashMap<String, Integer>();
	public Map<String, Double> PRMap = new HashMap<String, Double>();
	public Map<String, Double> newPRMap = new HashMap<String, Double>();
	
	public void loadStuff(){
		
		File f = new File("D:/Elastic Search/HW4/HW4/wt2g_inlinks.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = "";
			
			while((line = br.readLine()) != null && line.length() != 0){
				
				String[] split = line.split(" ",2);
				String page = split[0];
				pagesSet.add(page);
				
				if (split.length > 1){
					String inLinks = split[1];
					String[] parts = inLinks.split("\\s+");
					
					inLinksMap.put(page, inLinks);
					for( String s:parts){
						s = s.trim();
						pagesSet.add(s);
						if(outLinkCountMap.containsKey(s))
							outLinkCountMap.put(s, outLinkCountMap.get(s)+1);
						else
							outLinkCountMap.put(s,1);
					}
				}
				else{
					inLinksMap.put(page, "");
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadLinkGraph(){
		
		File f = new File("D:/Elastic Search/HW4/HW4/LinkGraph.txt");
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = "";
			
			while((line = br.readLine()) != null && line.length() != 0){
				
				String[] split = line.split(" ",2);
				String page = split[0];
				pagesSet.add(page);
				inLinksMap.put(page, "");
				
				String outLinks = split[1];
				String[] parts = outLinks.split("\\s+");

				for( String s:parts){
					s = s.trim();
					pagesSet.add(s);
					if(inLinksMap.containsKey(s))
						inLinksMap.put(s, inLinksMap.get(s) + page + " ");
					else
						inLinksMap.put(s,page + " ");
					
					outLinkCountMap.put(page,parts.length);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void getPageRank(){
		
		int N = pagesSet.size();
		double initialize = 1.00 / N;

		for(String link:pagesSet){
			PRMap.put(link, initialize);

			if(!outLinkCountMap.containsKey(link))
				sinkPagesSet.add(link);
		}
		
		while(isConverging()){

			double sinkPageRank = 0.0;

			for(String s:sinkPagesSet)
				sinkPageRank = sinkPageRank + PRMap.get(s);

			for(String p: pagesSet){
				double newPR = ( 1.0- lambda) / N;
				newPR += (lambda * sinkPageRank / N);
				newPRMap.put(p, newPR);

				if(inLinksMap.containsKey(p)){
					String[] inlinks = inLinksMap.get(p).split(" ");

					for(String q: inlinks){
						if(!q.equals("")){
							double sharOfPageRank = newPRMap.get(p) + (lambda * PRMap.get(q)/ outLinkCountMap.get(q)); 
							newPRMap.put(p, sharOfPageRank);
						}
					}
				}
			}

			for(String p: PRMap.keySet()){
				PRMap.put(p, newPRMap.get(p));
			}
		}
	}
	
	public double getPerplex(){
		double entropy = 0.0;
		for(String page: PRMap.keySet()){
			double rank = PRMap.get(page);
			double val= (Math.log(rank) / Math.log(2.0));
			entropy += rank *(val);
		}

		return Math.pow(2.0, -entropy);
	}
	
	public boolean isConverging(){
		
		currentPerplex = getPerplex();

		System.out.println("#" + count + " Perplex: " + currentPerplex);

		perplex[0] = perplex[1];
		perplex[1] = perplex[2];
		perplex[2] = perplex[3];
		perplex[3] = (int) currentPerplex;

		if(perplex[0] == perplex[1] && perplex[1] == perplex[2] && perplex[2] == perplex[3]) return false;
		count++;
		return true;
	}
	
	public void display(){
		System.out.println("Page ranks in descending order");
		Map<String,Double> sortedPageRanks = new TreeMap<String,Double>(new ValueComparator(PRMap));
		sortedPageRanks.putAll(PRMap);
		
		int i = 1;
		for(String s : sortedPageRanks.keySet()){
			if(i>10)
				break;
			else{
				System.out.println(i + " - " + s + " " + sortedPageRanks.get(s));
				i++;
			}
		}

		System.out.println(" ");
		System.out.println("Page outlink count");
		int out =1;
		Map<String,Integer> sortedOutlinks = new TreeMap<String,Integer>(new ValueComparator(outLinkCountMap));
		sortedOutlinks.putAll(outLinkCountMap);
		
		for(String s : sortedOutlinks.keySet()){
			if(out>10)
				break;
			else{
				System.out.println(out + " - " + s + "   " + sortedOutlinks.get(s));
				out++;
			}
		}
	}
	
	public static void main(String[] args) {
		
		PageRank pr = new PageRank();
		pr.loadLinkGraph();
		pr.getPageRank();
		pr.display();
	}
	
}