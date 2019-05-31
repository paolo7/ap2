package benchmarking;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.LogManager;



import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

import logic.Predicate;
import logic.PredicateInstantiationImpl;
import logic.RDFUtil;
import logic.Rule;

public class runBenchmark {

	public static void main(String[] args) throws Exception {
		
		// experiment 1
		experiment_1_critical_and_score_scalability_comparision();
		
		// experiment 2;
		experiment_2_score_scalability_for_large_rule_sizes();
	}
	

	private static void experiment_1_critical_and_score_scalability_comparision() throws FileNotFoundException, CloneNotSupportedException, IOException, PythonExecutionException {
		LogManager.getLogManager().reset();
		
		RDFUtil.disableRedundancyCheck = true;
		RDFUtil.ignoreConstraints = true;

		PredicateInstantiationImpl.enable_additional_constraints = false;
		
		System.out.println("RUNNING EXPERIMENT 1 - Comparison of SCORE and CRITICAL as schema size increases");
		// each configuration tests the algorithms on inputs of different size
		int configurations = 1000;
		// for each configuration a number of repetitions is done and the average score is recorded
		int repetitions = 10;
		int stepIncrease = 4;
		long millisecondTimeout = 10*60*1000; // 10 minutes timeout
		int atomsInAntecedent = 2;
		double constantCreationRate = 0.1;
		int ruleNum = 3;
		int initialSchemaviewSize =  5;
		runCriticalInstancePerformanceComparisonSchemaSize2(
				"Experiment 2",
				millisecondTimeout,
				5,
				repetitions,
				stepIncrease,
				initialSchemaviewSize,
				atomsInAntecedent,
				constantCreationRate,
				ruleNum,
				true
				);
		runCriticalInstancePerformanceComparisonSchemaSize2(
				"Experiment 2",
				millisecondTimeout,
				configurations,
				repetitions,
				stepIncrease,
				initialSchemaviewSize,
				atomsInAntecedent,
				constantCreationRate,
				ruleNum,
				false
				);
	}
	
	private static void experiment_2_score_scalability_for_large_rule_sizes() throws FileNotFoundException, CloneNotSupportedException, IOException, PythonExecutionException {
		LogManager.getLogManager().reset();
		
		RDFUtil.disableRedundancyCheck = true;
		RDFUtil.ignoreConstraints = true;

		PredicateInstantiationImpl.enable_additional_constraints = false;
		
		String experimentName = "Comparison EXPERIMENT 2 - Scalab ility of SCORE as rule size and rule length increases";
		System.out.println("RUNNING "+experimentName);
		// each configuration tests the algorithms on inputs of different size
		int configurations = 200;
		// for each configuration a number of repetitions is done and the average score is recorded
		int repetitions = 20;
		int stepIncrease = 40;
		long millisecondTimeout = 10*60*1000; // 10 minutes timeout
		int atomsInAntecedent = 8;
				//atomsInAntecedent = 12;
		double constantCreationRate = 0.1;
		int ruleNum = 0;
		int initialSchemaviewSize = 50;
		int rule_increases = 12;
		int rule_increase_step = 2;
		int sizeOfPredicateSpace = 60;
		// WARMUP
		runCriticalInstancePerformanceComparisonSchemaSizeDifferentRuleNumber(
				false,
				1,
				1,
				experimentName,
				1,
				1,
				repetitions,
				stepIncrease,
				initialSchemaviewSize,
				2,
				constantCreationRate,
				ruleNum,
				true,
				sizeOfPredicateSpace
				);
		// REAL
		runCriticalInstancePerformanceComparisonSchemaSizeDifferentRuleNumber(
				false,
				rule_increase_step,
				rule_increases,
				experimentName,
				millisecondTimeout,
				configurations,
				repetitions,
				stepIncrease,
				initialSchemaviewSize,
				atomsInAntecedent,
				constantCreationRate,
				ruleNum,
				false,
				sizeOfPredicateSpace
				);
	}
	
	private static void runCriticalInstancePerformanceComparisonSchemaSize2(String experimentName, long millisecondTimeout, int configurations, int repetitions, int stepIncrease, int initialSchemaviewSize, int atomsInAntecedent, double constantCreationRate, int ruleNum, boolean warmup) throws FileNotFoundException, CloneNotSupportedException, IOException, PythonExecutionException {
		
		
		if(warmup) {
			System.out.println("\n(Warmup run "+experimentName+")\n");
		} else {
			System.out.println("\n(Nromal run "+experimentName+")\n");
		}
		
		// SCORE
		List<Double> timeGPPG = new LinkedList<Double>();
		List<Double> timeCritical = new LinkedList<Double>();
		//List<Double> timeGPPGeach = new LinkedList<Double>();
		//List<Double> timeCriticaleach = new LinkedList<Double>();
		List<Double> xAxisComlexityGPPG = new LinkedList<Double>();
		List<Double> xAxisComlexityCritical = new LinkedList<Double>();
		
		RDFUtil.excludePredicatesFromCriticalInstanceConstants = false;
		
		String labelScore = "r'\\texttt{score}'";
		String labelCritical = "r'\\texttt{critical}'";
		String ylabel = "Seconds";
		String xlabel = "Schema Size";
		
		boolean stopRecordingCritical = false;
		boolean stopRecordingSCORE = false;
		for(int j = 0; j <= configurations; j += stepIncrease) {
			List<ScoreResult> scoresGPPG = new LinkedList<ScoreResult>();
			List<ScoreResult> scoresCritical = new LinkedList<ScoreResult>();
			int newschemaviewSize = initialSchemaviewSize +j;
			int newsizeOfPredicateSpace = (int) (((double)newschemaviewSize)*1.5); // J
			int newconstantPool = newschemaviewSize;
			int newruleNum = ruleNum;
			// do one of each, so not to cluster all the SCORE and all the critical together
			for(int i = 0; i < repetitions*2; i++) {
				if(i % 2 == 0) {
					
					// CRITICAL
					if(!stopRecordingCritical) {						
						ScoreResult srCritical = GeneratorUtil.evaluatePerformanceIteration(4, newsizeOfPredicateSpace, newruleNum, i/2, newschemaviewSize, atomsInAntecedent, newconstantPool, constantCreationRate, false);
						scoresCritical.add(srCritical);
						if(GeneratorUtil.avgResult(scoresCritical).time*(scoresCritical.size()) > millisecondTimeout*repetitions) {
							stopRecordingCritical = true;
							System.out.println("EARLY TERMINATION! STOP RECORDING CRITICAL AFTER "+scoresCritical.size()+" COMPUTE TIME "+GeneratorUtil.avgResult(scoresCritical).time+" > "+repetitions+" times "+millisecondTimeout);
							System.out.println("Last time recorded: "+srCritical.time);
						}
					}
				} else {
					// SCORE
					if(!stopRecordingSCORE) {						
						ScoreResult srSCORE = GeneratorUtil.evaluatePerformanceIteration(3, newsizeOfPredicateSpace, newruleNum, i/2, newschemaviewSize, atomsInAntecedent, newconstantPool, constantCreationRate, false);				
						scoresGPPG.add(srSCORE);
					}
				}				
			}
			// Average results
			ScoreResult scoreGPPG = GeneratorUtil.avgResult(scoresGPPG);
			ScoreResult scoreCritical = GeneratorUtil.avgResult(scoresCritical);
			// terminate if average too high:
			if(scoreCritical.time > millisecondTimeout) {
				stopRecordingCritical = true;
				System.out.println("STOP RECORDING CRITICAL AFTER COMPUTE TIME "+scoreCritical.time);
			} 
			if(scoreGPPG.time > millisecondTimeout) {
				stopRecordingSCORE = true;
				System.out.println("STOP RECORDING SCORE AFTER COMPUTE TIME "+scoreGPPG.time);
			}
			System.out.print("\n["+j+"] ");
			if(!stopRecordingCritical) {		
				xAxisComlexityCritical.add((double)newschemaviewSize);
				timeCritical.add(scoreCritical.time/1000);
				//timeCriticaleach.add(scoreCritical.averageRuleApplicationTime);
				System.out.print(" Critical: "+scoreCritical.time);
				//System.out.println(j+"               Critical2 "+scoreCritical.averageRuleApplicationTime);				
			}
			if(!stopRecordingSCORE) {		
				//timeGPPGeach.add(scoreGPPG.averageRuleApplicationTime);
				xAxisComlexityGPPG.add((double)newschemaviewSize);
				timeGPPG.add(scoreGPPG.time/1000);
				System.out.print(" SCORE: "+scoreGPPG.time);
				//System.out.println(j+"               GPPG2 "+scoreGPPG.averageRuleApplicationTime);
			}
			String pythonDataSCORE = xAxisComlexityGPPG+","+timeGPPG;
			String pythonDataCRITICAL = xAxisComlexityCritical+","+timeCritical;
			System.out.println("\n plt.plot("+pythonDataSCORE+", linestyle='-', marker=',', color='C0', label="+labelScore+")\n");
			System.out.println(" plt.plot("+pythonDataCRITICAL+", linestyle='-', marker='^', color='C1', label="+labelCritical+")\n");
			System.out.print("\n");
		}
		
		
		
		if(!warmup) {
			String pythonDataSCORE = xAxisComlexityGPPG+","+timeGPPG;
			String pythonDataCRITICAL = xAxisComlexityCritical+","+timeCritical;
			String pythonScript = "from mpl_toolkits.mplot3d import Axes3D\n" + 
					"from matplotlib.pyplot import figure\n" + 
					"from matplotlib import rcParams\n" + 
					"figure(num=None, figsize=(6.4, 2.7), dpi=300, facecolor='w', edgecolor='k')\n" + 
					"import matplotlib.pyplot as plt\n"+
					"import numpy as np\n" + 
					"rc('font', **{'family':'serif', 'serif':['Computer Modern Roman'], 'monospace': ['Computer Modern Typewriter'], 'size': ['larger']})\n" + 
					"plt.rc('text', usetex=True)\n" + 
					"params = {'axes.labelsize': 13,'axes.titlesize':13, 'legend.fontsize': 13, 'xtick.labelsize': 13, 'ytick.labelsize': 13 }\n" + 
					"rcParams.update(params)\n" + 
					"#### EXPERIMENT: "+experimentName+"\n"+
					"#### Date: "+new Date().toString()+"\n"+
					"# MAIN PARAMETERS:\n"+
					"# atomsInAntecedent (n_A) = "+atomsInAntecedent+"\n"+
					"# constantCreationRate (\\pi_C) = "+constantCreationRate+"\n"+
					"# ruleNum (R) = "+ruleNum+"\n"+
					"# initialSchemaviewSize (S)= "+initialSchemaviewSize+"\n"+
					"# constant pools (U and L)= S\n"+
					"# predicate space (P)= 1.5*S\n"+
					"# EXPERIMENT LENGTH:\n"+
					"# configurations added to S from 0 to "+configurations+" tested with steps increases of "+stepIncrease+"\n"+
					"# average time cutoff "+millisecondTimeout+" milliseconds\n"+
					"# number of different datasets per configuration: "+repetitions+"\n\n"+
					"plt.plot("+pythonDataSCORE+", linestyle='-', marker='o', color='C0', label="+labelScore+")\n" + 
					"plt.plot("+pythonDataCRITICAL+", linestyle='-', marker='^', color='C1', label="+labelCritical+")\n" + 
					"plt.ylabel('"+ylabel+"')\n"+
					"plt.xlabel('"+xlabel+"')\n"+
					"plt.legend()\n"+
					"plt.tight_layout()\n"+
					"plt.show()\n\n";
			System.out.println(pythonScript);
			
			try {
				Path p = Paths.get(System.getProperty("user.dir")+"/resultOutputs.txt");
				Files.createDirectories(p.getParent());
				if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS))
				    Files.createFile(p);
				//Files.createFile(p);
			    Files.write(p, pythonScript.getBytes(), StandardOpenOption.APPEND);
			}catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}
			
		} else {
			System.out.println("End Warmup run\n\n");
		}
	
		System.out.println("\nEnd Run\n");

	}
	private static void printScorePlot(boolean compact, String experimentName, int atomsInAntecedent, double constantCreationRate, int ruleNum,
			int configurations, int stepIncrease, int rule_increases, int rule_increase_step, long millisecondTimeout, int repetitions,
			int r, Map<Integer,List<Double>> xAxisComlexitiesGPPG, Map<Integer,List<Double>> timesGPPG, Map<Integer,List<Integer>> antecedentsSizeGPPG,
			Map<Integer,List<Double>> newFacts) {
			String labelScore = "SCORE";
			String labelCritical = "CRITICAL";
			String ylabel = "Seconds";
			String xlabel = "Number of Rules";
			String zlabel = "Antecedent Triple Size";
			String pythonScript = "from mpl_toolkits.mplot3d import Axes3D\n"
					+ "from matplotlib.pyplot import figure\n"
					+ "from matplotlib import rcParams\n"
					+ "figure(num=None, figsize=(6.4, 2.7), dpi=300, facecolor='w', edgecolor='k')\n"
					+ "import matplotlib.pyplot as plt\n"
					+ "import numpy as np\n" + 
					"rc('font', **{'family':'serif', 'serif':['Computer Modern Roman'], 'monospace': ['Computer Modern Typewriter'], 'size': ['larger']})\n" + 
					"plt.rc('text', usetex=True)\n" + 
					"params = {'axes.labelsize': 13,'axes.titlesize':13, 'legend.fontsize': 13, 'xtick.labelsize': 13, 'ytick.labelsize': 13 }\n" + 
					"rcParams.update(params)\n" + 
					"#### EXPERIMENT: "+experimentName+"\n"+
					"#### Date: "+new Date().toString()+"\n"+
					"# MAIN PARAMETERS:\n"+
					"# atomsInAntecedent (n_A) = "+atomsInAntecedent+"\n"+
					"# constantCreationRate (\\pi_C) = "+constantCreationRate+"\n"+
					"# ruleNum (R) = "+ruleNum+"\n"+
					"# initialSchemaviewSize (S) = 30\n"+
					"# constant pools (U and L)= S\n"+
					"# predicate space (P)= 1.5*S\n"+
					"# EXPERIMENT LENGTH:\n"+
					"# configurations added to R from 0 to "+configurations+" tested with steps increases of "+stepIncrease+"\n"+
					"# configurations of antecedent length from 0 to "+rule_increases+" tested with steps increases of "+rule_increase_step+"\n"+
					"# average time cutoff "+millisecondTimeout+" milliseconds\n"+
					"# number of different datasets per configuration: "+repetitions+"\n\n";			
			if(compact) pythonScript = "# ...COMPACT\n";
			int colorIndex = 0;	
			for(int r1 = 0; r1 <= r; r1 += rule_increase_step)  {
					String pythonDataSCORE = xAxisComlexitiesGPPG.get(new Integer(r1))+","+timesGPPG.get(new Integer(r1));
					//String pythonDataCRITICAL = xAxisComlexitiesCritical.get(new Integer(r1))+","+timesCritical.get(new Integer(r1));
					pythonScript = pythonScript+
							"plt.plot("+pythonDataSCORE+", linestyle='-', marker='$"+(colorIndex)+"$', color='C"+(colorIndex)+"',  label='"+labelScore+" ("+(r1+atomsInAntecedent)+")')\n"
							//+"plt.plot("+pythonDataCRITICAL+", linestyle='-', marker='^', color='C"+r+"', label='"+labelCritical+"_"+r+"')\n"
							;
					colorIndex++;
				}
			if(!compact) pythonScript = pythonScript+"plt.ylabel('"+ylabel+"')\n"+
						"plt.xlabel('"+xlabel+"')\n"+
						"plt.legend()\n"+
						"plt.tight_layout()\n"+
						"plt.show()\n";
			//}
			System.out.println(pythonScript);
			
			try {
			    Files.write(Paths.get("resultOutputs.txt"), pythonScript.getBytes(), StandardOpenOption.APPEND);
			}catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}
			
			labelScore = "SCORE";
			labelCritical = "CRITICAL";
			ylabel = "Applicable Rules";
			xlabel = "Rule Number";
			zlabel = "Antecedent Triple Size";
			pythonScript = "from mpl_toolkits.mplot3d import Axes3D\n"
					+ "from matplotlib.pyplot import figure\n"
					+ "figure(num=None, figsize=(6.4, 2.7), dpi=300, facecolor='w', edgecolor='k')\n"
					+ "import matplotlib.pyplot as plt\n"
					+ "import numpy as np\n" + 
					"#### EXPERIMENT: "+experimentName+"\n"+
					"#### Date: "+new Date().toString()+"\n"+
					"# MAIN PARAMETERS:\n"+
					"# atomsInAntecedent (n_A) = "+atomsInAntecedent+"\n"+
					"# constantCreationRate (\\pi_C) = "+constantCreationRate+"\n"+
					"# ruleNum (R) = "+ruleNum+"\n"+
					"# initialSchemaviewSize (S) = 30\n"+
					"# constant pools (U and L)= S\n"+
					"# predicate space (P)= 1.5*S\n"+
					"# EXPERIMENT LENGTH:\n"+
					"# configurations added to R from 0 to "+configurations+" tested with steps increases of "+stepIncrease+"\n"+
					"# configurations of antecedent length from 0 to "+rule_increases+" tested with steps increases of "+rule_increase_step+"\n"+
					"# average time cutoff "+millisecondTimeout+" milliseconds\n"+
					"# number of different datasets per configuration: "+repetitions+"\n\n";		
			if(compact) pythonScript = "# ...COMPACT\n";
			colorIndex = 0;	
			for(int r1 = 0; r1 <= r; r1 += rule_increase_step)  {
					String pythonDataSCORE = xAxisComlexitiesGPPG.get(new Integer(r1))+","+newFacts.get(new Integer(r1));
					pythonScript = pythonScript+
							"plt.plot("+pythonDataSCORE+", linestyle='-', marker='$"+(colorIndex)+"$', color='C"+(colorIndex)+"',  label='"+labelScore+" ("+(r1+atomsInAntecedent)+")')\n"
							//+"plt.plot("+pythonDataCRITICAL+", linestyle='-', marker='^', color='C"+r+"', label='"+labelCritical+"_"+r+"')\n"
							;
					colorIndex++;
				}
			if(!compact) pythonScript = pythonScript+"plt.ylabel('"+ylabel+"')\n"+
						"plt.xlabel('"+xlabel+"')\n"+
						"plt.legend()\n"+
						"plt.tight_layout()\n"+
						"plt.show()\n";
			//}
				System.out.println(pythonScript+"\n\n");
			
			try {
			    Files.write(Paths.get("resultOutputs.txt"), pythonScript.getBytes(), StandardOpenOption.APPEND);
			}catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}
	}
	
	private static void runCriticalInstancePerformanceComparisonSchemaSizeDifferentRuleNumber(boolean schema_proportional_to_rule_num, int rule_increase_step, int rule_increases, String experimentName, long millisecondTimeout, int configurations, int repetitions, int stepIncrease, int initialSchemaviewSize, int atomsInAntecedent, double constantCreationRate, int ruleNum, boolean warmup, int sizeOfPredicateSpace) throws FileNotFoundException, CloneNotSupportedException, IOException, PythonExecutionException {
		
		
		if(warmup) {
			System.out.println("\n(Warmup run "+experimentName+")\n");
		} else {
			System.out.println("\n(Normal run "+experimentName+")\n");
		}
		
		Map<Integer,List<Double>> timesGPPG = new HashMap<Integer,List<Double>>();
		Map<Integer,List<Double>> timesCritical = new HashMap<Integer,List<Double>>();
		Map<Integer,List<Double>> xAxisComlexitiesGPPG = new HashMap<Integer,List<Double>>();
		Map<Integer,List<Double>> xAxisComlexitiesCritical = new HashMap<Integer,List<Double>>();
		Map<Integer,List<Integer>> antecedentsSizeGPPG = new HashMap<Integer,List<Integer>>();
		Map<Integer,List<Integer>> antecedentsSizeCritical = new HashMap<Integer,List<Integer>>();
		Map<Integer,List<Double>> newFacts = new HashMap<Integer,List<Double>>();
		
		for(int r = 0; r < rule_increases; r += rule_increase_step) {
			// SCORE
			Integer R = new Integer(r);
			List<Double> timeGPPG = new LinkedList<Double>();
			timesGPPG.put(R, timeGPPG);
			List<Double> newFactsGPPG = new LinkedList<Double>();
			newFacts.put(R, newFactsGPPG);
			List<Double> timeCritical = new LinkedList<Double>();
			timesCritical.put(R, timeCritical);
			//List<Double> timeGPPGeach = new LinkedList<Double>();
			//List<Double> timeCriticaleach = new LinkedList<Double>();
			List<Double> xAxisComlexityGPPG = new LinkedList<Double>();
			xAxisComlexitiesGPPG.put(R, xAxisComlexityGPPG);
			List<Double> xAxisComlexityCritical = new LinkedList<Double>();
			xAxisComlexitiesCritical.put(R, xAxisComlexityCritical);
			List<Integer> antecedentSizeGPPG = new LinkedList<Integer>();
			antecedentsSizeGPPG.put(R, antecedentSizeGPPG);
			List<Integer> antecedentSizeCritical = new LinkedList<Integer>();
			antecedentsSizeCritical.put(R, antecedentSizeCritical);
			
			RDFUtil.excludePredicatesFromCriticalInstanceConstants = false;
		
			boolean stopRecordingCritical = true;
			boolean stopRecordingSCORE = false;
			for(int j = 0; j <= configurations; j += stepIncrease) {
				List<ScoreResult> scoresGPPG = new LinkedList<ScoreResult>();
				List<ScoreResult> scoresCritical = new LinkedList<ScoreResult>();
				int newschemaviewSize = initialSchemaviewSize;
				int newconstantPool = newschemaviewSize;
				int newruleNum = ruleNum+j;
				int newAtomsInAntecedent = atomsInAntecedent+r;
				if(schema_proportional_to_rule_num) {
					newschemaviewSize = initialSchemaviewSize*newAtomsInAntecedent;
					//newschemaviewSize = newruleNum*(newAtomsInAntecedent-1);
				}
				if(newruleNum == 0) newruleNum = 1;
				int newsizeOfPredicateSpace = sizeOfPredicateSpace;
				if(newsizeOfPredicateSpace < 0) newsizeOfPredicateSpace = (int) (((double)newschemaviewSize)*1.5); // J
				// do one of each, so not to cluster all the SCORE and all the critical together
				for(int i = 0; i < repetitions*2; i++) {
					if(i % 2 == 0) {
						// CRITICAL
						if(!stopRecordingCritical) {						
							ScoreResult srCritical = GeneratorUtil.evaluatePerformanceIteration(4, newsizeOfPredicateSpace, newruleNum, i/2, newschemaviewSize, newAtomsInAntecedent, newconstantPool, constantCreationRate, false);
							scoresCritical.add(srCritical);
							if(GeneratorUtil.avgResult(scoresCritical).time*(scoresCritical.size()) > millisecondTimeout*repetitions) {
								stopRecordingCritical = true;
								System.out.println("EARLY TERMINATION! STOP RECORDING CRITICAL AFTER "+scoresCritical.size()+" COMPUTE TIME "+GeneratorUtil.avgResult(scoresCritical).time+" > "+repetitions+" times "+millisecondTimeout);
								System.out.println("Last time recorded: "+srCritical.time);
							}
						}
					} else {
						// SCORE
						if(!stopRecordingSCORE) {						
							ScoreResult srSCORE = GeneratorUtil.evaluatePerformanceIteration(3, newsizeOfPredicateSpace, newruleNum, i/2, newschemaviewSize, newAtomsInAntecedent, newconstantPool, constantCreationRate, false);				
							scoresGPPG.add(srSCORE);
						}
					}				
				}
				
				// Average results
				ScoreResult scoreGPPG = GeneratorUtil.avgResult(scoresGPPG);
				ScoreResult scoreCritical = GeneratorUtil.avgResult(scoresCritical);
				// terminate if average too high:
				if(scoreCritical.time > millisecondTimeout) {
					stopRecordingCritical = true;
					System.out.println("STOP RECORDING CRITICAL AFTER COMPUTE TIME "+scoreCritical.time);
				} 
				if(scoreGPPG.time > millisecondTimeout) {
					stopRecordingSCORE = true;
					System.out.println("STOP RECORDING SCORE AFTER COMPUTE TIME "+scoreGPPG.time);
				}
				System.out.print("\n["+j+"] ");
				if(!stopRecordingCritical) {		
					xAxisComlexityCritical.add((double)newruleNum);
					antecedentSizeCritical.add((int)newAtomsInAntecedent);
					timeCritical.add(scoreCritical.time/1000);
					//timeCriticaleach.add(scoreCritical.averageRuleApplicationTime);
					System.out.print(" Critical: "+scoreCritical.time/1000);
					//System.out.println(j+"               Critical2 "+scoreCritical.averageRuleApplicationTime);				
				}
				if(!stopRecordingSCORE) {		
					//timeGPPGeach.add(scoreGPPG.averageRuleApplicationTime);
					xAxisComlexityGPPG.add((double)newruleNum);
					antecedentSizeGPPG.add((int)newAtomsInAntecedent);
					timeGPPG.add(scoreGPPG.time/1000);
					newFactsGPPG.add(scoreGPPG.applicableRules);
					System.out.print(" SCORE: "+scoreGPPG.time/1000);
					//System.out.println(j+"               GPPG2 "+scoreGPPG.averageRuleApplicationTime);
				}
				System.out.print("\n");
				if(!warmup) printScorePlot(true, experimentName, atomsInAntecedent, constantCreationRate, ruleNum, configurations, stepIncrease, rule_increases, rule_increase_step, millisecondTimeout, repetitions, r, xAxisComlexitiesGPPG, timesGPPG, antecedentsSizeGPPG, newFacts);

			}
			
			if(!warmup) {
				printScorePlot(false, experimentName, atomsInAntecedent, constantCreationRate, ruleNum, configurations, stepIncrease, rule_increases, rule_increase_step, millisecondTimeout, repetitions, r, xAxisComlexitiesGPPG, timesGPPG, antecedentsSizeGPPG, newFacts);
			}
		}
		
		
		
		
		if(!warmup) {
			System.out.println("\n\n FINAL RESULTS!\n\n");
			printScorePlot(false, experimentName, atomsInAntecedent, constantCreationRate, ruleNum, configurations, stepIncrease, rule_increases, rule_increase_step, millisecondTimeout, repetitions, rule_increases, xAxisComlexitiesGPPG, timesGPPG, antecedentsSizeGPPG, newFacts);
			
		} else {
			System.out.println("End Warmup run\n\n");
		}
	
		System.out.println("\nEnd Run\n");

	}
}
