package rest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;



class Content{
	public String link;
	public String dscore, gscore, vscore;
}

@Path("/submit")
public class RWS {

	static HashMap<String,Integer> hmDScore = new HashMap<String, Integer>();
	static HashMap<String,Integer> hmGScore = new HashMap<String, Integer>();
	static HashMap<String,Integer> hmVScore = new HashMap<String, Integer>();


	@POST
	@Path("/")
	@Consumes("application/json")
	public void process(Content query){
		System.out.println("asd  = " +query.link);
		System.out.println("asd  = " +query.dscore);
		System.out.println("asd  = " +query.vscore);
		System.out.println("asd  = " +query.gscore);
		System.out.println("----------------------");
		hmDScore.put(query.link, Integer.parseInt(query.dscore));
		hmGScore.put(query.link, Integer.parseInt(query.gscore));
		hmVScore.put(query.link, Integer.parseInt(query.vscore));
//		if(hmDScore.size() >= 100){
			writeLinkScore(hmDScore, "LinkScore_D_Divya", " D_Divya ");
			writeLinkScore(hmGScore, "LinkScore_C_Guna", " C_Guna ");
			writeLinkScore(hmVScore, "LinkScore_S_Vivek", " S_Vivek ");
			writeCombinedLinkScore(hmDScore, hmGScore, hmVScore);
			hmDScore.clear();
			hmGScore.clear();
			hmVScore.clear();
//		}

	}

	@GET
	@Path("/asd")
	//@Consumes("text/plain")
	public void process(){
		System.out.println("asd call ");
	}

	public void writeLinkScore(HashMap<String,Integer> hmMap , String filename, String user){
		try {



			//			int sc = Math.max(Math.max(ddScore, gunaScore),vivekScore);

			File file = new File("D:/Elastic Search/AP_DATA/" + filename + ".txt");
			String query = "gulab jamun";
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bw = new BufferedWriter(fw);

			StringBuffer sb = new StringBuffer();
			String content = null;

			int queryNo = 2;

			//Print Divya's score

			for(String s : hmMap.keySet()){
				sb.append(queryNo+ user + s + " "+hmMap.get(s));
				sb.append(System.getProperty("line.separator"));
			}

			content = sb.toString();
			bw.write(content);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}

	public void writeCombinedLinkScore(HashMap<String,Integer> hmMap1, HashMap<String,Integer> hmMap2,HashMap<String,Integer> hmMap3){
		try {
			File file = new File("D:/Elastic Search/AP_DATA/finalLinkScore.txt");

			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bw = new BufferedWriter(fw);

			StringBuffer sb = new StringBuffer();
			String content = null;
			int queryNo = 2;
			for(String s : hmMap1.keySet()){
				double avg = (hmMap1.get(s)+ hmMap2.get(s) + hmMap3.get(s))/ 3;
				double score = Math.ceil(avg);
				sb.append(queryNo+ " DGV " + s + " "+ score);
				sb.append(System.getProperty("line.separator"));
			}
			content = sb.toString();
			bw.write(content);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
	}
}

