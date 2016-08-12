import java.io.*;
import java.util.*;




class Prec{
	public int queryNo, rel_count=0, no_ret=0, rel_ret=0;
	public double[] precs, recalls, f1s;
	public double avg_prec, r_prec, ndcg;
}

public class Eval {

	String FilePath = "D:/Elastic Search/HW4/HW4/"; // common directory
	Map<Integer, Map> qrels = new HashMap<Integer, Map>(); // qrels map
	Map<Integer, Integer> relCountMap = new HashMap<Integer, Integer>(); // relevant doc count
	File qrels_file,result_file;
	int[] cutoff = {5, 10, 15, 20, 30, 100};
	int tot_relevant, tot_rel_ret, tot_retrieved, queryCount;
	double[] total_precs = new double[cutoff.length], total_recalls= new double[cutoff.length], total_f1s= new double[cutoff.length];
	double[] avg_precs = new double[cutoff.length], avg_recalls= new double[cutoff.length], avg_f1s= new double[cutoff.length];
	double total_avg_prec, mean_prec, total_r1_prec, mean_r1, total_ndcg, mean_ndcg;


	public Eval(String qrelsFile, String resultFile){ // Initialize stuff
		qrels_file = new File(FilePath + qrelsFile);
		result_file = new File(FilePath + resultFile);
		tot_relevant = tot_rel_ret = tot_retrieved = queryCount =0;
	}
	
	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
	    Comparator<K> valueComparator =  new Comparator<K>() {
	        public int compare(K k1, K k2) {
	            int compare = map.get(k2).compareTo(map.get(k1));
	            if (compare == 0) return 1;
	            else return compare;
	        }
	    };
	    Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	    sortedByValues.putAll(map);
	    return sortedByValues;
	}


	public void evaluate(boolean eachQuery){

		try {
			// loading qrels file to hashmap
			BufferedReader br = new BufferedReader(new FileReader(qrels_file));
			String line ="";
			int queryNo = -1;
			int relCount = 0;
			Map<String, Integer> map = new HashMap<String, Integer>();

			while((line= br.readLine()) != null){

				String[] parts = line.split(" ");
				int query = Integer.parseInt(parts[0]);

				if(queryNo == -1) queryNo = query;
				if(queryNo != query){ // before we go to next query
					qrels.put(queryNo, map);
					relCountMap.put(queryNo, relCount);
					relCount= 0;
					map = new HashMap<String, Integer>();
					queryNo = query;
				}

				double rel = Double.parseDouble(parts[3]);
				map.put(parts[2], (int)rel);

				relCount += rel;
			}

			// after while exits, store the last one too
			qrels.put(queryNo, map);
			relCountMap.put(queryNo, relCount);

			br.close();


			// reading results file
			br = new BufferedReader(new FileReader(result_file));
			line = "";
			queryNo = -1;
			Prec currentQueryPrec = new Prec();
			Map<String, Double> tempMap = null;
			while((line= br.readLine()) != null){

				String[] parts = line.split(" ");
				int query = Integer.parseInt(parts[0]);
				if(queryNo == -1) { // first loop
					queryNo = query;

					// Initialize for first query
					tempMap = new HashMap<String, Double>();
					currentQueryPrec = new Prec();
					currentQueryPrec.queryNo = queryNo;
					currentQueryPrec.rel_count = (relCountMap.get(queryNo) == null? 0:relCountMap.get(queryNo));
					currentQueryPrec.f1s = new double[1000+1];
					currentQueryPrec.precs = new double[1000+1];
					currentQueryPrec.recalls = new double[1000+1];
				}

				if(queryNo != query ){ // before going to next query

					// Sort by score
//					Map<String, Double> sortedMap = new TreeMap<String, Double>(new ValueComparator(tempMap));
//					sortedMap.putAll(tempMap);
					Map<String, Double> sortedMap = sortByValues(tempMap);
					Set<String> docs = sortedMap.keySet();
					calculateResults(currentQueryPrec,docs,eachQuery);

					queryNo = query;

					// Initialize for next query
					tempMap = new HashMap<String, Double>();
					currentQueryPrec = new Prec();
					currentQueryPrec.queryNo = queryNo;
					currentQueryPrec.rel_count = (relCountMap.get(queryNo) == null? 0:relCountMap.get(queryNo));
					currentQueryPrec.f1s = new double[1000+1];
					currentQueryPrec.precs = new double[1000+1];
					currentQueryPrec.recalls = new double[1000+1];
				}
				String link = parts[2];
				double score = Double.parseDouble(parts[4]);
				tempMap.put(link, score);
				
			}

			// Finally for the last one
//			Map<String, Double> sortedMap = new TreeMap<String, Double>(new ValueComparator(tempMap));
//			sortedMap.putAll(tempMap);
			Map<String, Double> sortedMap = sortByValues(tempMap);
			Set<String> docs = sortedMap.keySet();
			calculateResults(currentQueryPrec,docs,eachQuery);

			// Summarize
			mean_prec = total_avg_prec/queryCount;
			mean_r1 = total_r1_prec/queryCount;
			mean_ndcg = total_ndcg/queryCount;

			for(int i=0; i<cutoff.length; i++){
				avg_f1s[i] = total_f1s[i]/queryCount;
				avg_precs[i] = total_precs[i]/queryCount;
				avg_recalls[i] = total_recalls[i]/queryCount;
			}

			printResults(null); // print total results

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void calculateResults(Prec currentQueryPrec, Set<String> set, boolean eachQuery){

		double prec_sum = 0.0;
		double dcg = 0.0;
		Integer[] rels = new Integer[set.size()+1];
		rels[0] = Integer.MAX_VALUE;
		for(String link:set){

			if(currentQueryPrec.no_ret >= 1000) continue; // max limit
			currentQueryPrec.no_ret++;
			int queryNo = currentQueryPrec.queryNo;
			int relevance = (qrels.get(queryNo).get(link) == null? 0:(int)qrels.get(queryNo).get(link));

			if(relevance > 0)
				prec_sum += (double) relevance * (1.00+ currentQueryPrec.rel_ret)/currentQueryPrec.no_ret;

			currentQueryPrec.rel_ret += relevance;
			rels[currentQueryPrec.no_ret] = relevance;

			// scores
			double precision = (double)currentQueryPrec.rel_ret/currentQueryPrec.no_ret;
			double recall = (double)currentQueryPrec.rel_ret/currentQueryPrec.rel_count;
			double f1;
			if (precision == 0.0 && recall == 0.0) 
				f1 = 0.0;
			else
				f1 = (2* precision* recall)/(precision +recall);
			
			dcg += (currentQueryPrec.no_ret == 1? relevance: (relevance/Math.log(currentQueryPrec.no_ret)));
			
			currentQueryPrec.precs[currentQueryPrec.no_ret] = precision;
			currentQueryPrec.recalls[currentQueryPrec.no_ret] = recall;
			currentQueryPrec.f1s[currentQueryPrec.no_ret] = f1;
		}
		
		// Calculate nDCG
		Arrays.sort(rels, Collections.reverseOrder());
		double dcg1 =0.0;
		
		for(int i=1;i<rels.length;i++){
			dcg1 += (i == 1? rels[1]: (rels[i]/Math.log(i)));
		}
		
		currentQueryPrec.ndcg = dcg/dcg1;
		
		// average precision
		currentQueryPrec.avg_prec = prec_sum/currentQueryPrec.rel_count;
		
		double finalRecall = currentQueryPrec.rel_ret/currentQueryPrec.rel_count;
		for(int i=currentQueryPrec.no_ret; i<=1000;i++){
			currentQueryPrec.precs[i] = currentQueryPrec.rel_ret/i;
			currentQueryPrec.recalls[i] = finalRecall;
		}

		// R - precision
		if(currentQueryPrec.rel_count > currentQueryPrec.no_ret)
			currentQueryPrec.r_prec = currentQueryPrec.rel_ret/currentQueryPrec.rel_count;
		else
			currentQueryPrec.r_prec = currentQueryPrec.precs[currentQueryPrec.rel_count];

		// if -q then print results
		if(eachQuery) printResults(currentQueryPrec);

		prec_sum=0; // reset
		tot_relevant += currentQueryPrec.rel_count;
		tot_rel_ret += currentQueryPrec.rel_ret;
		tot_retrieved += currentQueryPrec.no_ret;
		total_avg_prec += currentQueryPrec.avg_prec;
		total_r1_prec += currentQueryPrec.r_prec;
		total_ndcg += currentQueryPrec.ndcg;
		queryCount++;

		for(int i=0; i<cutoff.length; i++){ // only values at cutoff marks are stored
			total_f1s[i] += currentQueryPrec.f1s[cutoff[i]];
			total_recalls[i] += currentQueryPrec.recalls[cutoff[i]];
			total_precs[i] += currentQueryPrec.precs[cutoff[i]];
		}

	}

	public void printResults(Prec result){

		if(result !=null){ // for each query

			StringBuffer sb = new StringBuffer();

			sb.append("Query Number: ");
			sb.append(result.queryNo);
			sb.append(System.getProperty("line.separator"));

			sb.append("Document statistics: ");
			sb.append(System.getProperty("line.separator"));
			sb.append("   Retrieved: ");
			sb.append(result.no_ret);
			sb.append(System.getProperty("line.separator"));
			sb.append("   Relevant:  ");
			sb.append(result.rel_count);
			sb.append(System.getProperty("line.separator"));
			sb.append("   Rel_ret:   ");
			sb.append(result.rel_ret);
			sb.append(System.getProperty("line.separator"));
			sb.append("Precisions at k documents:");
			sb.append(System.getProperty("line.separator"));
			sb.append("   ");
			sb.append("k       Prec      Recall    F1");
			sb.append(System.getProperty("line.separator"));

			for (int i=0;i<cutoff.length;i++){
				sb.append("   ");
				String k = String.valueOf(cutoff[i]);
				sb.append(k);
				int remainder = 8 - k.length();
				while(remainder > 0){
					sb.append(" ");
					remainder--;
				}

				sb.append(String.format("%.4f", result.precs[cutoff[i]]));
				sb.append("    ");
				sb.append(String.format("%.4f", result.recalls[cutoff[i]]));
				sb.append("    ");
				sb.append(String.format("%.4f", result.f1s[cutoff[i]]));
				sb.append(System.getProperty("line.separator"));

			}
			sb.append("Average Precision: " + String.format("%.4f",result.avg_prec));
			sb.append(System.getProperty("line.separator"));
			sb.append("R-Precision: " + String.format("%.4f",result.r_prec));
			sb.append(System.getProperty("line.separator"));
			sb.append("nDCG: " + String.format("%.4f",result.ndcg));
			sb.append(System.getProperty("line.separator"));
			sb.append(System.getProperty("line.separator"));

			System.out.println(sb.toString());
		}

		else {

			StringBuffer sb = new StringBuffer();
			sb.append(" -- Consolidated precision -- ");
			sb.append(System.getProperty("line.separator"));
			sb.append(System.getProperty("line.separator"));
			sb.append("Query count: ");
			sb.append(queryCount);
			sb.append(System.getProperty("line.separator"));

			sb.append("Document statistics: ");
			sb.append(System.getProperty("line.separator"));
			sb.append("   Total Retrieved: ");
			sb.append(tot_retrieved);
			sb.append(System.getProperty("line.separator"));
			sb.append("   Total Relevant:  ");
			sb.append(tot_relevant);
			sb.append(System.getProperty("line.separator"));
			sb.append("   Total Rel_ret:   ");
			sb.append(tot_rel_ret);
			sb.append(System.getProperty("line.separator"));
			sb.append("Precisions at k documents:");
			sb.append(System.getProperty("line.separator"));
			sb.append("   ");
			sb.append("k       Prec      Recall    F1");
			sb.append(System.getProperty("line.separator"));

			for (int i=0;i<cutoff.length;i++){
				sb.append("   ");
				String k = String.valueOf(cutoff[i]);
				sb.append(k);
				int remainder = 8 - k.length();
				while(remainder > 0){
					sb.append(" ");
					remainder--;
				}

				sb.append(String.format("%.4f", avg_precs[i]));
				sb.append("    ");
				sb.append(String.format("%.4f", avg_recalls[i]));
				sb.append("    ");
				sb.append(String.format("%.4f", avg_f1s[i]));
				sb.append(System.getProperty("line.separator"));

			}
			sb.append("Average Precision: " + String.format("%.4f",mean_prec));
			sb.append(System.getProperty("line.separator"));
			sb.append("R-Precision: " + String.format("%.4f",mean_r1));
			sb.append(System.getProperty("line.separator"));
			sb.append("nDCG: " + String.format("%.4f",mean_ndcg));
			sb.append(System.getProperty("line.separator"));
			sb.append(System.getProperty("line.separator"));

			System.out.println(sb.toString());

		}
	}

	public static void main(String[] args) {

		Eval ev = new Eval("finalLinkScore.txt","Results.txt");
		ev.evaluate(true);

	}

}
