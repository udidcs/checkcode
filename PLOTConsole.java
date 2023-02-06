package com.diquest.plot.runner;

import com.diquest.jiana3.morph.JianaConst;
import com.diquest.plot.PLOT;
import com.diquest.plot.PLOTConst;
import com.diquest.plot.deep.NERModelManager;
import com.diquest.plot.result.PLOTResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class PLOTConsole{
	private final static int IDX_PLOT_RESOURCES_HOME = 0;
	private final static int IDX_PLOT_DIC_DIR = 1;
	private final static int IDX_JIANA_DIC_DIR = 2;
	private final static int IDX_LANGUAGE = 3;
	
	PLOT plot = null;
	
	/**
	 * PLOT 을 초기화한다. 
	 *
	 * @param params : param[0]-"plot/dcd" param[1]-"jiana/dcd"
	 */
	public void init(String[] params){

		if(params.length < 4){
			String[] newParams = new String[4];
			System.arraycopy(params, 0, newParams, 0, 3);
			newParams[IDX_LANGUAGE] = "KOREAN";
			params = newParams;
		}

		plot = PLOT.getInstance(params[IDX_LANGUAGE]);
		plot.init2(params[IDX_PLOT_RESOURCES_HOME], params[IDX_PLOT_DIC_DIR], params[IDX_JIANA_DIC_DIR]);
	}



	public void run() throws IOException{

		plot.setCategory("AA");
		System.out.println();

		String line = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));



		ArrayList<String> lst = new ArrayList<>();

		System.out.print("PLOT add examples or enter regex>");

		while((line=br.readLine()) != null){

			if (!line.equals("regex")) {
				lst.add(line);
				System.out.println("added");
			}

			if(line.equals("exit")){
				return;
			}

			if(line.equals("regex")){
				System.out.print("regex pattern:");
				String pattern = br.readLine();
				for (String str : lst) {
					if (Pattern.matches(pattern, str)) {
						PLOTResult result = plot.analyze(str, 1, PLOTConst.ANALYZE_OPTION.DEFAULT_OPTION, JianaConst.MIN_CORRECT_LEVEL, JianaConst.CORRECT_OPTION.ALLOFF_FLAG, true);

						System.out.println("------------------------------------------------------------------------------------------");

						System.out.println("[PLOTResult]");
						result.setregexPattern(pattern);
						System.out.println(result);

						System.out.println("[JianaResult]");
						System.out.println(plot.analyzeJianaOnly(line.toCharArray()).toString());

						System.out.println("[NounSequence]");
						System.out.println(result.getNounSequence());

						System.out.println("[ResultSequence]");
						System.out.println(result.getResultSequence());
					}
				}
				System.out.println("------------------------------------------------------------------------------------------");

			}

			if(line.startsWith("changeCat:")){
				String[] lineSplit = line.split(":");
				String category = lineSplit[1].trim();
				plot.setCategory(category);
				System.out.println();
				System.out.print("PLOT>");
				continue;
			}

			if(line.startsWith("newCat:")){
				String[] lineSplit = line.split(":");
				String category = lineSplit[1].trim();
				plot.createCategory(category);
				System.out.println();
				System.out.print("PLOT>");
				continue;
			}

			System.out.print("PLOT add examples or enter regex>");


		}
		
		br.close();
		this.plot.fine();
	}
	
	/**
	 *	@param args
	 * 	args[2] : 언어 파라미터. KOREAN, ENGLISH, JAPANESE, CHINESE 등을 설정 가능
	 */
	public static void main(String[] args){
		args = new String[3];
		args[0] = "../resources";
		args[1] = "plot/dic/korean/dcd";
		args[2] = "jiana/dic/korean/dcd";

		if(args==null || args.length < 3){
			System.err.println("[PLOT] usage : [RESOURCES_HOME] [PLOT_DCD_FOLDER] [JIANA_DCD_FOLDER] [LANGUAGE]");
			return;
		}
		
		PLOTConsole console = new PLOTConsole();

		console.init(args);
		try{
			console.run();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
}