package datagenerator;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.management.RuntimeErrorException;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import chase.graph.Edge;
import chase.graph.Graph;
import chase.graph.Vertex;
import datalog.DatalogParser;
import datalog.DatalogScanner;
import gqr.DatalogQuery;
import gqr.GQRNode;
import gqr.Index;
import gqr.JoinDescription;
import gqr.JoinInView;
import gqr.Pair;
import gqr.PredicateJoin;
import gqr.SourcePredicateJoin;

public class VariablePredicateLengthWeaklyAcyclicGenerator extends RandomStatementGenerator{ 
	//this class is going to have chained queries
	//a chained query is one where all of the atoms are linked
	//by a chain of variables, and they share no other variables.
	//The differerent parameters that can be set are:
	//Function starting: THIS IS INPUT VARIABLE 'start'
	//the number at which functions will start
	//Function (George: she means function=predicate)stopping:
	//the number at which functions will stop THIS IS INPUT VARIABLE 'stop'
	//the number of each function that you're gounig to get is roughly THIS IS INPUT VARIABLE 'num_fun'
	//1/(function stopping - function starting)
	//Function length: length of each function; note, a function must have 
	//at least 2 variables, because otherwise, it can't chain.
	//currently, all lengths will be the same
	//Number of distinguished variables (George: number of returning variables)THIS IS INPUT VARIABLE 'dist'
	//The number of variables that are distinguished.  Since
	//this is a chain query, as long as one variable is distinguished, 
	//it makes sense.
	//at the moment, the first variable in the function is always the 
	//chained one


	int NumberFunctions;
	int FunctionStart;
	int FunctionStop;
	static int FunctionLength;
	int _max_number_duplicates;
	int _number_distinguished;
	RandomVariableGenerator FunctionGenerator;
	String FunctionOffset;
	ArrayList<gqr.Predicate> targetSchema = null;


	public VariablePredicateLengthWeaklyAcyclicGenerator(ArrayList<gqr.Predicate> targetSchema, int start,int stop, int num_fun,int dist, int num_dup){
		this.targetSchema = targetSchema;
		_max_number_duplicates = num_dup;
		//need to figure out how to do assertions
		//assert start <  stop 
		//assert num_fun > 0
		//assert len
		//note, I assume that it means that if NumberDistinguished = -1, then
		//it wants them all.
		//actually, i assume in this case that we have three choices,
		//1,2, all, but that's beside the point
		NumberFunctions = num_fun;
		FunctionStart = start;
		FunctionStop = stop;
		if (FunctionStart > FunctionStop){
			//then we've screwed up; return an error message and exit
			System.out.println("function stop greater than the function start in ChainRandomStatementGenerator; exiting");
			System.exit(1);
		}
		if (NumberFunctions > _max_number_duplicates * (FunctionStop - FunctionStart)){
			System.out.println("You have requested more predicates than available in ChainRandomStatementGenerator; exiting");
			System.exit(1);
		}
		FunctionLength = maxArity(targetSchema);
		_number_distinguished = dist;
		_max_number_duplicates = num_dup;
		FunctionGenerator = new RegularRandomVariableGenerator(start,stop);
		//Random rand = new Random();
		if (FunctionLength < 10){
			FunctionOffset = "00" + FunctionLength;
		}
		else if (FunctionLength < 100){
			FunctionOffset = "0" + FunctionLength;
		}
	}//end ChainRandomStatementGenerator(int,int)

	private int maxArity(ArrayList<gqr.Predicate> tSchema) {
		int max =0;
		for(int i=0; i<tSchema.size(); i++)
		{
			int psize = tSchema.get(i).getArguments().size();
			max=(max<psize)?psize:max;
		}
		
		return max;
	}

	private String getFunctionHead(){

		String retval = FunctionGenerator.getRandomVariable() + FunctionOffset;
		return retval;
	}




	public Statement getRandomStatement(boolean use_all){
		//this function returns a random statement based on the
		//values for the mean and deviation of the 
		//size of the query, and the mean and deviation of the 
		//num of variables and the number of function heads.
		//note; need to think about this more carefully for 
		//variables and function heads.
		Statement retval = new Statement();
		int i,j;
		//String [] variables;
		int extra_variable = FunctionLength+1;
		//int length;
		//int fcnlength;
		//String temp;
		Predicate apred;
		String a_function_head;
		for (i = 0; i < NumberFunctions; i++){
			apred = new Predicate();
			a_function_head = getFunctionHead();
			while (retval.numPredicateOccurances(a_function_head) == _max_number_duplicates){
				//if this is the case, then we need to pick a new one
				a_function_head = getFunctionHead();
				//note, we should be safe entering this loop, because we have carefully 
				//checked the number of predicates available earlier
			}
			
			apred.setFunctionHead(a_function_head);
			apred.addVariable((new Integer(i)).toString());
			apred.addVariable((new Integer(i+1)).toString());
			
			String predname = apred.function;
			predname = predname.substring(0, predname.lastIndexOf("00"));
			int targetRelationSize = targetSchema.get(new Integer(predname)-1).getArguments().size();
			if(targetRelationSize < 2)
				throw new InputMismatchException("Creating chains needs at least binary predicates.\n "
						+ "Found relation "+ targetSchema.get(new Integer(predname)-1));
			
			for (j = 2; j < targetRelationSize; j++){
				apred.addVariable((new Integer(extra_variable)).toString());
				extra_variable++;
			}
			
			retval.addSubgoal(apred);  

		}
		generateHead(retval); 
		return retval;

	}

	public void generateHead(Statement a_state){
		//this function generates the head of the statement.  Note that probably only the
		//variables used will really matter, because we'll change the head name anyway.
		//thus i will call them all "q", which, come to think of it, is probably 
		//why i didn't get any errors in this before.  oy, i feel dumb.
		Predicate head = new Predicate();
		head.setFunctionHead("q");
		if (_number_distinguished == 1){
			head.addVariable(a_state.subgoalI(0).variableI(0));
		}
		else if  (_number_distinguished == 2){
			head.addVariable(a_state.subgoalI(0).variableI(0));
			head.addVariable(a_state.subgoalI(a_state.size()-1).variableI(1));
		}
		else if (_number_distinguished < 0){
			System.out.println("can't have a statement with no distinguished variables in ChainRandomStatementGenerator.generateHead");
			System.out.println("system will exit");
			System.exit(1);
		}
		else if (_number_distinguished == 0){
			Vector unique_vars = a_state.findUniqueVariables();
			int i;
			_number_distinguished = unique_vars.size();
			for (i = 0; i < _number_distinguished; i++){
				head.addVariable((String)unique_vars.elementAt(i));
			}
		}

		else{
			Vector unique_vars = a_state.findUniqueVariables();


			int i;
			int rand;
			float percent_distinguished = (float) _number_distinguished /
					(float) unique_vars.size();
			int num_dist_needed = _number_distinguished;

			boolean all_done = false;
			boolean add_to_end = false;
			Random random = new Random();

			for (i = 0; all_done == false && 
					add_to_end == false &&
					i < unique_vars.size();i++){
				if (random.nextFloat() < percent_distinguished){
					//then we need to add it
					head.addVariable((String)unique_vars.elementAt(i));
					num_dist_needed--;
					if (num_dist_needed == 0){
						//then we can stop adding- break out of loop
						all_done = true;
					}//end if
				}//end if
				else if((unique_vars.size() - i   - 1) == num_dist_needed){
					//we need to check and make sure that we don't
					//need to just add the rest....
					add_to_end = true;
				}//end else if

			}//end for
			if (add_to_end){
				for (; i < unique_vars.size(); i++){
					head.addVariable((String)unique_vars.elementAt(i));
				}
			}
		}
		a_state.setHead(head);

	}//end of generateHead

	
	private static CharSequence formatViewForBullit(String constraint) {
		
		String out = constraint.replace(":-"," -> ")+".";
		out= out.replace("X", "$X");
		out=out.replace("),", ") & ");
		System.out.println(out);
		return out;
	}
	
	private static CharSequence formatViewForChaseBench(String constraint) {
		
		String out = constraint.replace(":-"," -> ")+".";
		out= out.replace("X", "?X");
		out=out.replace("),", "), ");
		System.out.println(out);
		return out;
	}

	private static CharSequence formatConstraintForBullit(String constraint) {
		
		String name = constraint.substring(0,constraint.indexOf("("));
		int sizeOfBody = new Integer(constraint.substring(constraint.indexOf("DC")+2,constraint.indexOf(")")));
		//System.out.println("name ="+name);
		//System.out.println("Size of body ="+sizeOfBody);
		int indLastAtom =  indexOfLastBodyAtom(constraint,sizeOfBody);
		//System.out.println("Index of comma after last bofy atom ="+;
		
		String out = constraint.substring(constraint.indexOf(":-")+2,indLastAtom);
		out = out+" -> "+constraint.substring(indLastAtom+1)+".";
		out= out.replace("X", "$X");
		out=out.replace("),", ") & ");
		return out;
	}
	
	private static CharSequence formatConstraintForChaseBench(String constraint) {
		
		String name = constraint.substring(0,constraint.indexOf("("));
		int sizeOfBody = new Integer(constraint.substring(constraint.indexOf("DC")+2,constraint.indexOf(")")));
		//System.out.println("name ="+name);
		//System.out.println("Size of body ="+sizeOfBody);
		int indLastAtom =  indexOfLastBodyAtom(constraint,sizeOfBody);
		//System.out.println("Index of comma after last bofy atom ="+;
		
		String out = constraint.substring(constraint.indexOf(":-")+2,indLastAtom);
		out = out+" -> "+constraint.substring(indLastAtom+1)+".";
		out= out.replace("X", "?X");
		out=out.replace("),", "), ");
		return out;
	}

	private static int indexOfLastBodyAtom(String text, int sizeOfBody)
	{
	    for (int i = 0; i < text.length()-1; i++)
	    {
	        if (text.charAt(i) == ')' && text.charAt(i+1) == ',')
	        {
	            sizeOfBody--;
	            if (sizeOfBody == 0)
	            {
	                return i+1;
	            }
	        }
	    }
	    return -1;
	}
	static void addConstraintInWAGraph(Graph g, String constraint) {


		DatalogQuery con = null;
		DatalogScanner scanner = new DatalogScanner(new StringReader(constraint));
		DatalogParser parser = new DatalogParser(scanner);
		try{
			con = parser.query();	
		} catch (RecognitionException re) {
			throw new RuntimeException(re);
		} catch (TokenStreamException e) {
			throw new RuntimeException(e);
		}
		
		Pair<List<SourcePredicateJoin>,List<SourcePredicateJoin>> constraintPJs = Index.createTGDConstraintPJs(con);
		
		List<SourcePredicateJoin> antecedents  = constraintPJs.getA();
		setOriginallyExistentialVariables(antecedents,constraintPJs.getB());


		HashSet<Vertex> commonVariablesBetwenAntecedentAndConsequent = new HashSet<Vertex>();

		for(SourcePredicateJoin antecedent: antecedents)
		{
			for(Entry<Integer,GQRNode> nodeEntry: antecedent.getGqrNodes().entrySet())
			{
				int edgeNo = nodeEntry.getKey();
				GQRNode gqrNode = nodeEntry.getValue();

				JoinDescription node = new JoinDescription(antecedent.getPredicate(),edgeNo);
				Vertex from = new Vertex(node);
				from = g.addVertex(from);

				//for all joins of the from node
				for(JoinDescription join: gqrNode.getInfobox().getJoinInViews().iterator().next().getJoinDescriptions())
				{
					//create edges from "from" node to all the joined positions in consequent
					for(PredicateJoin conPJ:constraintPJs.getB())
						if(join.getPredicate().equals(conPJ.getPredicate()))
						{
							Vertex to = new Vertex(new JoinDescription(join.getPredicate(), join.getEdgeNo()));
							to = g.addVertex(to);

							g.addEdge(from,to,0); //zero means regular directed edge 

							commonVariablesBetwenAntecedentAndConsequent.add(from);
						}
				}
			}
		}

		for(SourcePredicateJoin consequent: constraintPJs.getB())
		{
			for(Entry<Integer,GQRNode> nodeEntry: consequent.getGqrNodes().entrySet())
			{
				int edgeNo = nodeEntry.getKey();
				GQRNode gqrNode = nodeEntry.getValue();

				if(gqrNode.isOriginallyExistentialInConstraint())
				{
					Vertex to = new Vertex(new JoinDescription(consequent.getPredicate(), edgeNo));
					to = g.addVertex(to);

					for(Vertex from: commonVariablesBetwenAntecedentAndConsequent)
						g.addEdge(from,to,1);//1 means "starred" directed edge
				}
			}
		}
	}
	
	private static void setOriginallyExistentialVariables(List<SourcePredicateJoin> antecedents, List<SourcePredicateJoin> consequents) {

		for(PredicateJoin pj: consequents)
		{
			Map<Integer, GQRNode> map = pj.getGqrNodes();

			for(int i=1; i<=map.size(); i++)
			{
				GQRNode pjsNode = map.get(new Integer(i));
				List<JoinDescription> joinsWithAntecedent = new ArrayList<JoinDescription>();
				List<JoinDescription> joinsWithConsequents = new ArrayList<JoinDescription>();

				assert(pjsNode.getInfobox().getJoinInViews().size() == 1);

				JoinInView jv = pjsNode.getInfobox().getJoinInViews().iterator().next();

				for(JoinDescription jd : jv.getJoinDescriptions())
				{
					boolean joinsWithAnt = false;
					for(PredicateJoin antPJ:antecedents)
						if(jd.getPredicate().equals(antPJ.getPredicate()))
						{
							joinsWithAnt = true;
							break;
						}

					if(joinsWithAnt)
					{
						joinsWithAntecedent.add(jd);
						pjsNode.setOriginallyExistentialInConstraint(false);//we're setting this multiple times but it's ok
					}
					else
						joinsWithConsequents.add(jd);
				}

				pjsNode.setJoinsWithAntecedent(joinsWithAntecedent);		
				pjsNode.setJoinsWithConsequents(joinsWithConsequents);

			}
		}
	}

	public static boolean graphRemainsWeaklyAcyclic(Graph g, String constraint) {

		addConstraintInWAGraph(g, constraint);

		for(Edge e:g.findCycles())
		{
			if(e.getCost() == 1)
				return false;
		}


		return true;
	}
	//	public static void main(String []args){
	//		String dir = "/opt/GQRexperiments/ucq_fixedQ2VRelevanceUpto180_thereafterIcreasing/data/";
	//				
	////		new File().mkdir();
	//		
	//		
	//		
	//		//This 3x10 queries UCQ with 1000 views (one viewset file)
	//		for(int i=0; i<100; i++)
	//		{
	//			
	//			new File(dir+"/run_"+i).mkdir();
	//			
	//
	//			//=============================== 10 queries for case i with pscpace size  = 20
	//			
	//			File file1 = new File(dir+"/run_"+i+"/queries_"+i+"_20p.txt");
	//			
	//			try {
	//				file1.createNewFile();
	//			} catch (IOException e) {
	//				throw new RuntimeException(e);
	//			}
	//			PrintWriter outfile1;
	//			try {
	//				outfile1 = new PrintWriter(file1);
	//			} catch (FileNotFoundException e) {
	//				throw new RuntimeException(e);
	//			}
	//			
	//			for(int j=0; j<10; j++)
	//			{
	//				//ChainRandomStatementGenerator(start_pred_space 1, stop_pred_space 20, #preds/body 8, predlength 4, #dist 10, #dupl 5);
	//				ChainRandomStatementGenerator a1 = new ChainRandomStatementGenerator(1, 20, 8, 4, 7, 4);
	//				Statement foo1 = a1.getRandomStatement(false);
	//				foo1.getHead().setFunctionHead("q"+i+j+"20p"); //add prefix q in the heads
	//
	//				Vector vec2 = new Vector();
	//				for(Object ob:foo1.getHead().variables)
	//				{
	//					String str = ((String)ob);
	//					vec2.add("X"+str);// add prefix X in all variables
	//				}
	//				foo1.getHead().variables = vec2;
	//
	//				for(Predicate pred:foo1.body)
	//				{
	//					Vector vec3 = new Vector();
	//					if(pred == null)
	//						continue;
	//					pred.setFunctionHead("m"+pred.getFunctionHead()); //put prefix m in front of a predicate's name
	//					for(Object ob:pred.variables)
	//					{
	//						String str = ((String)ob);
	//						vec3.add("X"+str);
	//					}
	//					pred.variables = vec3;
	//				}
	//
	//				foo1.print();
	//				outfile1.append(foo1.printString()).append(System.getProperty("line.separator"));
	//			}
	//			outfile1.close();
	//
	//			//==============================
	//			
	//			//=============================== 10 queries for case i with pscpace size  = 40
	//			
	//			file1 = new File(dir+"/run_"+i+"/queries_"+i+"_40p.txt");
	//			
	//			try {
	//				file1.createNewFile();
	//			} catch (IOException e) {
	//				throw new RuntimeException(e);
	//			}
	//
	//			try {
	//				outfile1 = new PrintWriter(file1);
	//			} catch (FileNotFoundException e) {
	//				throw new RuntimeException(e);
	//			}
	//			
	//			for(int j=0; j<10; j++)
	//			{
	//				//ChainRandomStatementGenerator(start_pred_space 1, stop_pred_space 20, #preds/body 8, predlength 4, #dist 10, #dupl 5);
	//				ChainRandomStatementGenerator a1 = new ChainRandomStatementGenerator(21, 60, 8, 4, 7, 4);
	//				Statement foo1 = a1.getRandomStatement(false);
	//				foo1.getHead().setFunctionHead("q"+i+""+j+"40p"); //add prefix q in the heads
	//
	//				Vector vec2 = new Vector();
	//				for(Object ob:foo1.getHead().variables)
	//				{
	//					String str = ((String)ob);
	//					vec2.add("X"+str);// add prefix X in all variables
	//				}
	//				foo1.getHead().variables = vec2;
	//
	//				for(Predicate pred:foo1.body)
	//				{
	//					Vector vec3 = new Vector();
	//					if(pred == null)
	//						continue;
	//					pred.setFunctionHead("m"+pred.getFunctionHead()); //put prefix m in front of a predicate's name
	//					for(Object ob:pred.variables)
	//					{
	//						String str = ((String)ob);
	//						vec3.add("X"+str);
	//					}
	//					pred.variables = vec3;
	//				}
	//
	//				foo1.print();
	//				outfile1.append(foo1.printString()).append(System.getProperty("line.separator"));
	//			}
	//			outfile1.close();
	//
	//			//==============================
	//			
	//			//=============================== 10 queries for case i with pscpace size  = 60
	//			
	//			file1 = new File(dir+"/run_"+i+"/queries_"+i+"_60p.txt");
	//			
	//			try {
	//				file1.createNewFile();
	//			} catch (IOException e) {
	//				throw new RuntimeException(e);
	//			}
	//
	//			try {
	//				outfile1 = new PrintWriter(file1);
	//			} catch (FileNotFoundException e) {
	//				throw new RuntimeException(e);
	//			}
	//			
	//			for(int j=0; j<10; j++)
	//			{
	//				//ChainRandomStatementGenerator(start_pred_space 1, stop_pred_space 20, #preds/body 8, predlength 4, #dist 10, #dupl 5);
	//				ChainRandomStatementGenerator a1 = new ChainRandomStatementGenerator(1, 60, 8, 4, 7, 4);
	//				Statement foo1 = a1.getRandomStatement(false);
	//				foo1.getHead().setFunctionHead("q"+i+""+j+"60p"); //add prefix q in the heads
	//
	//				Vector vec2 = new Vector();
	//				for(Object ob:foo1.getHead().variables)
	//				{
	//					String str = ((String)ob);
	//					vec2.add("X"+str);// add prefix X in all variables
	//				}
	//				foo1.getHead().variables = vec2;
	//
	//				for(Predicate pred:foo1.body)
	//				{
	//					Vector vec3 = new Vector();
	//					if(pred == null)
	//						continue;
	//					pred.setFunctionHead("m"+pred.getFunctionHead()); //put prefix m in front of a predicate's name
	//					for(Object ob:pred.variables)
	//					{
	//						String str = ((String)ob);
	//						vec3.add("X"+str);
	//					}
	//					pred.variables = vec3;
	//				}
	//
	//				foo1.print();
	//				outfile1.append(foo1.printString()).append(System.getProperty("line.separator"));
	//			}
	//			outfile1.close();
	//			//==============================
	//			
	//			
	//			// CONSTRUCT VIEWS IN THE SAME FILE
	//			
	//			File file = new File(dir+"/run_"+i+"/views_for_q_"+i+".txt");
	//			try {
	//				file.createNewFile();
	//			} catch (IOException e) {
	//				throw new RuntimeException(e);
	//			}
	//			PrintWriter outfile;
	//			try {
	//				outfile = new PrintWriter(file);
	////				foo1.getHead().setFunctionHead("vq"); ///COPY THE QUERYY IN THE VIEWSSSSSSSSSS
	////				outfile.append(foo1.printString()).append(System.getProperty("line.separator"));
	//			} catch (FileNotFoundException e) {
	//				throw new RuntimeException(e);
	//			}
	//			
	//			for(int j=0; j<10000; j++)
	//			{
	//				ChainRandomStatementGenerator a = null;
	//				if(j<180)
	//				{
	//					if(j%4 == 0)
	//						a = new ChainRandomStatementGenerator(1, 20, 8, 4, 10, 4);
	//					else if(j%4 == 1 || j%4 == 2)
	//						a = new ChainRandomStatementGenerator(21, 60, 8, 4, 10, 4);
	//					else
	//						a = new ChainRandomStatementGenerator(61, 80, 8, 4, 10, 4);
	//				}
	//				else
	//				{
	//					a = new ChainRandomStatementGenerator(1, j, 8, 4, 3, 4);
	////					if(j%4 == 0)
	////						a = new ChainRandomStatementGenerator(1, 20, 8, 4, 3, 4);
	////					else if(j%4 == 1 || j%4 == 2)
	////						a = new ChainRandomStatementGenerator(21, 60, 8, 4, 3, 4);
	////					else
	////						a = new ChainRandomStatementGenerator(61, 80, 8, 4, 3, 4);
	//				}
	////				
	//					
	//				Statement foo = a.getRandomStatement(false);
	//				foo.getHead().setFunctionHead("v"+j);
	//				
	//				Vector vec = new Vector();
	//				for(Object ob:foo.getHead().variables)
	//				{
	//					String str = ((String)ob);
	//					vec.add("X"+str);
	//				}
	//				foo.getHead().variables = vec;
	//				
	//				for(Predicate pred:foo.body)
	//				{
	//					Vector vec1 = new Vector();
	//					if(pred == null)
	//						continue;
	//					pred.setFunctionHead("m"+pred.getFunctionHead());
	//					for(Object ob:pred.variables)
	//					{
	//						String str = ((String)ob);
	//						vec1.add("X"+str);
	//					}
	//					pred.variables = vec1;
	//				}
	//				
	//				
	//				//foo.print();
	//				
	//				outfile.append(foo.printString()).append(System.getProperty("line.separator"));
	//			}
	//			
	//			outfile.close();
	//		}
	//	}

	public static void generateChaseConstraintData(ArrayList<gqr.Predicate> tSchema,PrintWriter constraintFileBlt,int noOfdataSets, int numberOfconstraints,int constraintSize, int maxNoOfRepeteadRelsPerConstraint) throws CloneNotSupportedException {

		int sizeOfPredicateSpace = tSchema.size()+1;
		
		for(int i=0; i<noOfdataSets; i++) //WE ARE GOING TO CONSTRUCT this many DATASETS (EACH SET HAS 1 QUERY, 50 CONSTRAINTS, 500 VIEWS)
		{
			//System.out.println(new File(dir+"/run_"+i).mkdir());
			Graph g = new Graph();

			//CREATE CONSTRAINTS
			if(i>0)
				throw new RuntimeException("Parameterize name below with i");


			Vector<String> constraints = new Vector<String>();

			for(int j=0; j<numberOfconstraints; j++)
			{
				VariablePredicateLengthWeaklyAcyclicGenerator a = null; //new WeaklyAcyclicLAVGenerator(1, 20, 4, 4, 1, 3);

				//				if(j<21)
				//					a = new WeaklyAcyclicLAVGenerator(1, 20, 8, 4, 1, 3);
				//				else if(j<81)
				//					a = new WeaklyAcyclicLAVGenerator(1, j, 8, 4, 1, 3);
				//				else
				//					a = new WeaklyAcyclicLAVGenerator(1, 500, 8, 4, 1, 3);

				//					if(j<20)
				//						a = new WeaklyAcyclicLAVGenerator(1, 20, 4, 4, 1, 3);
									//if(j%10 == 1)
				 						//a = new VariablePredicateLengthWeaklyAcyclicGenerator(tSchema,1, (int) (sizeOfPredicateSpace/5), constraintSize, arity, 1, maxNoOfRepeteadRelsPerConstraint);
				 					//else
				 						a = new VariablePredicateLengthWeaklyAcyclicGenerator(tSchema, 1, sizeOfPredicateSpace, constraintSize, 1, maxNoOfRepeteadRelsPerConstraint);
				 						
				 						
				Statement constraintStatement = null;
				
				while(constraintStatement == null)
				{
					try{
						constraintStatement = a.getRandomStatement(false);
					}catch(InputMismatchException e)
					{
						constraintStatement = null;
						System.err.println("Cannot use binary relation -- retracting constraint");
					}
				}
				
				constraintStatement.getHead().setFunctionHead("c"+j);

				Vector vec = new Vector();
				for(Object ob:constraintStatement.getHead().variables)
				{
					String str = ((String)ob);
					Random r = new Random();
					vec.add("DC"+(r.nextInt(constraintSize-1)+1));//don't care
				}
				constraintStatement.getHead().variables = vec;

				for(Predicate pred:constraintStatement.body)
				{
					Vector vec1 = new Vector();
					if(pred == null)
						continue;
					pred.setFunctionHead("m"+pred.getFunctionHead());
					for(Object ob:pred.variables)
					{
						String str = ((String)ob);
						vec1.add("X"+str);
					}
					pred.variables = vec1;
				}

				String constraint = constraintStatement.printString().toString();

				//it seems that the chain generator is partial to generating the same first predicate in a rule. In the case of queries/views it doesn't matter. In the case of LAV constraints, the first predicate is the antecedent, so we'll mix things up a bit.

				int start = constraint.indexOf("m");
				int end = constraint.indexOf(",m");
				String firstPredicate = constraint.substring(start,end);
				constraint = constraint.substring(0,start)+constraint.substring(end+1)+","+firstPredicate;
				//				System.out.println(constraint);

				if(graphRemainsWeaklyAcyclic((Graph)g.clone(), constraint))
				{
					addConstraintInWAGraph(g,constraint);
					
					constraint = rewriteConstraintOverTargeSchema(tSchema,constraint,constraintSize);
					//constraintFile.append(constraint).append(System.getProperty("line.separator"));
					constraintFileBlt.append(formatConstraintForChaseBench(constraint)).append(System.getProperty("line.separator"));
				//	if(j<=10 || j%2 == 0)
						constraints.add(constraint);
//					else
//					{
//						a = new WeaklyAcyclicLAVGenerator(1, 300, 5, 4, 1, 3);
//
//						constraintStatement = a.getRandomStatement(false);
//						constraintStatement.getHead().setFunctionHead("c"+j);
//
//						vec = new Vector();
//						for(Object ob:constraintStatement.getHead().variables)
//						{
//							String str = ((String)ob);
//							vec.add("DC");//don't care
//						}
//						constraintStatement.getHead().variables = vec;
//
//						for(Predicate pred:constraintStatement.body)
//						{
//							Vector vec1 = new Vector();
//							if(pred == null)
//								continue;
//							pred.setFunctionHead("m"+pred.getFunctionHead());
//							for(Object ob:pred.variables)
//							{
//								String str = ((String)ob);
//								vec1.add("X"+str);
//							}
//							pred.variables = vec1;
//						}
//
//						constraint = constraintStatement.printString().toString();
//						constraint = constraint.substring(0,constraint.lastIndexOf(",m"));
//						constraints.add(constraint);
//						constraintFile.append(constraint).append(System.getProperty("line.separator"));
//						//
//					}
				}
				else
				{
					System.out.println("Retracting constraint, Weak Acyclicity violated");
					j--;
				}
			}
			//constraintFile.close();
			constraintFileBlt.close();
		}


			//END CREATE CONSTRAINTS---------------------------------------------		
		
	}

	private static String rewriteConstraintOverTargeSchema(ArrayList<gqr.Predicate> tSchema, String constraint, int constraintSize) {
		
		int flength = new Integer(FunctionLength).toString().length();
	//	System.out.println("FunctionLength: "+FunctionLength+" Decimal digits: "+flength);
	//	System.out.println("Constraint: "+constraint);
		String pre =  constraint.substring(0,constraint.indexOf("m"));
	//	System.out.println("prefix: "+pre);
		String conrest = constraint.substring(constraint.indexOf("m"));
	//	System.out.println("Rest: "+conrest);
//		System.out.println("going in for: "+constraintSize);
		int i=1;
		while(conrest.indexOf("00"+FunctionLength+"(")>=0)
		{

	//		System.out.println("Inside for: "+i++);
			int startofzeros = conrest.indexOf("00"+FunctionLength+"(");
		//	System.out.println("Start of 00X: "+startofzeros);
			
			int predicatestart = conrest.substring(0,startofzeros).lastIndexOf("m");
			//System.out.println("The next m() predicates starts at character: "+predicatestart);
			
			int predicateend = conrest.indexOf("00"+FunctionLength+"(")+flength+2;
			//System.out.println("The next m() predicates ends at character: "+(predicateend));
			
			String sub = conrest.substring(predicatestart, predicateend);
			//System.out.println("And it is: "+sub);
			int prednumber = Integer.parseInt(sub.substring(sub.indexOf("m")+1,sub.indexOf("00"+FunctionLength)));
			//System.out.println("The integer it contains:"+prednumber);
			//System.out.println("Replacing "+sub+ " by "+tSchema.get(new Integer(prednumber)-1).name);
			conrest = conrest.replace(sub, tSchema.get(new Integer(prednumber)-1).name);
			//System.out.println(pre+conrest);
			//System.out.println("-----------------------------------");
		}
		//System.out.println(pre+conrest);
		
		return pre+conrest;
	}


	/*
     	public static void main(String []args){
		String dir = System.getProperty("user.home")+"/users_link/gkonstant/Desktop/" +
				"experiments/chain_10qX10000v+qAsView_20PredSpaceTill20v_" +
				"500predSpaceAfter20_8PredBody_4var_10Dtill80v_3Dtill10000v_5repMax/data";

		//new File(dir).mkdir();


		//FOR 100qX140 views with each viewset in different file : 1) go to end of file 2) uncomment code 3) look at remaining comments
		//This is for 10 queries with 10000 views (one viewset file)
		for(int i=0; i<10; i++)
		{

			new File(dir+"/run_"+i).mkdir();
			File file1 = new File(dir+"/run_"+i+"/query_"+i+".txt");

			try {
				file1.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			PrintWriter outfile1;
			try {
				outfile1 = new PrintWriter(file1);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

			//ChainRandomStatementGenerator(start_pred_space 1, stop_pred_space 20, #preds/body 8, predlength 4, #dist 10, #dupl 5);
			ChainRandomStatementGenerator a1 = new ChainRandomStatementGenerator(1, 20, 8, 4, 10, 5);
			Statement foo1 = a1.getRandomStatement(false);
			foo1.getHead().setFunctionHead("q"+i); //add prefix q in the heads

			Vector vec2 = new Vector();
			for(Object ob:foo1.getHead().variables)
			{
				String str = ((String)ob);
				vec2.add("X"+str);// add prefix X in all variables
			}
			foo1.getHead().variables = vec2;

			for(Predicate pred:foo1.body)
			{
				Vector vec3 = new Vector();
				if(pred == null)
					continue;
				pred.setFunctionHead("m"+pred.getFunctionHead()); //put prefix m in front of a predicate's name
				for(Object ob:pred.variables)
				{
					String str = ((String)ob);
					vec3.add("X"+str);
				}
				pred.variables = vec3;
			}

			foo1.print();

			outfile1.append(foo1.printString()).append(System.getProperty("line.separator"));
			outfile1.close();


			// CONSTRUCT VIEWS IN THE SAME FILE

			File file = new File(dir+"/run_"+i+"/views_for_q_"+i+".txt");
			try {
				file.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			PrintWriter outfile;
			try {
				outfile = new PrintWriter(file);///COPY THE QUERYY IN THE VIEWSSSSSSSSSS
				foo1.getHead().setFunctionHead("vq");
				outfile.append(foo1.printString()).append(System.getProperty("line.separator"));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

			for(int j=0; j<81; j++)
			{
				ChainRandomStatementGenerator a = null;
				if(j<21)
					a = new ChainRandomStatementGenerator(1, 20, 8, 4, 10, 5);
				else
					a = new ChainRandomStatementGenerator(1, j, 8, 4, 10, 5);


				Statement foo = a.getRandomStatement(false);
				foo.getHead().setFunctionHead("v"+j);

				Vector vec = new Vector();
				for(Object ob:foo.getHead().variables)
				{
					String str = ((String)ob);
					vec.add("X"+str);
				}
				foo.getHead().variables = vec;

				for(Predicate pred:foo.body)
				{
					Vector vec1 = new Vector();
					if(pred == null)
						continue;
					pred.setFunctionHead("m"+pred.getFunctionHead());
					for(Object ob:pred.variables)
					{
						String str = ((String)ob);
						vec1.add("X"+str);
					}
					pred.variables = vec1;
				}


				//foo.print();

				outfile.append(foo.printString()).append(System.getProperty("line.separator"));
			}

			for(int j=81; j<10000; j++)
			{
				if(j%1000 == 0)
					System.out.println(j);

				ChainRandomStatementGenerator a = new ChainRandomStatementGenerator(1, 500, 8, 4, 3, 5);
				Statement foo = a.getRandomStatement(false);
				foo.getHead().setFunctionHead("v"+j);

				Vector vec = new Vector();
				for(Object ob:foo.getHead().variables)
				{
					String str = ((String)ob);
					vec.add("X"+str);
				}
				foo.getHead().variables = vec;

				for(Predicate pred:foo.body)
				{
					Vector vec1 = new Vector();
					if(pred == null)
						continue;
					pred.setFunctionHead("m"+pred.getFunctionHead());
					for(Object ob:pred.variables)
					{
						String str = ((String)ob);
						vec1.add("X"+str);
					}
					pred.variables = vec1;
				}

				outfile.append(foo.printString()).append(System.getProperty("line.separator"));
			}
			outfile.close();
		}
	}
	 */
}
/*
for(int i=1; i<10; i++)
//int i=0;
{


	new File(dir+"/run_"+i).mkdir();
//	new File(dir+"/run_"+i+"/views_for_q_"+i).mkdir();

//	File file1 = new File(dir+"/run_"+i+"/views_for_q_"+i+"/query_"+i+".txt");
	File file1 = new File(dir+"/run_"+i+"/query_"+i+".txt");
	try {
		file1.createNewFile();
	} catch (IOException e) {
		throw new RuntimeException(e);
	}
	PrintWriter outfile1;
	try {
		outfile1 = new PrintWriter(file1);
	} catch (FileNotFoundException e) {
		throw new RuntimeException(e);
	}

	ChainRandomStatementGenerator a1 = new ChainRandomStatementGenerator(1, 20, 8, 4, 10, 5);
	Statement foo1 = a1.getRandomStatement(false);
	foo1.getHead().setFunctionHead("q"+i); //add prefix q in the heads

	Vector vec2 = new Vector();
	for(Object ob:foo1.getHead().variables)
	{
		String str = ((String)ob);
		vec2.add("X"+str);// add prefix X in all variables
	}
	foo1.getHead().variables = vec2;

	for(Predicate pred:foo1.body)
	{
		Vector vec3 = new Vector();
		if(pred == null)
			continue;
		pred.setFunctionHead("m"+pred.getFunctionHead()); //put prefix m in front of a predicate's name
		for(Object ob:pred.variables)
		{
			String str = ((String)ob);
			vec3.add("X"+str);
		}
		pred.variables = vec3;
	}

	foo1.print();

	outfile1.append(foo1.printString()).append(System.getProperty("line.separator"));
	outfile1.close();

	File file = new File(dir+"/run_"+i+"/views_for_q_"+i+".txt");
	try {
		file.createNewFile();
	} catch (IOException e) {
		throw new RuntimeException(e);
	}
	PrintWriter outfile;
	try {
		outfile = new PrintWriter(file);
	} catch (FileNotFoundException e) {
		throw new RuntimeException(e);
	}

	for(int j=0; j<81; j++)
	{

//		File file = new File(dir+"/run_"+i+"/views_for_q_"+i+"/view_"+j+".txt");
//		try {
//			file.createNewFile();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		PrintWriter outfile;
//		try {
//			outfile = new PrintWriter(file);
//		} catch (FileNotFoundException e) {
//			throw new RuntimeException(e);
//		}


		ChainRandomStatementGenerator a = new ChainRandomStatementGenerator(1, 20, 8, 4, 10, 5);
		Statement foo = a.getRandomStatement(false);
		foo.getHead().setFunctionHead("v"+j);

		Vector vec = new Vector();
		for(Object ob:foo.getHead().variables)
		{
			String str = ((String)ob);
			vec.add("X"+str);
		}
		foo.getHead().variables = vec;

		for(Predicate pred:foo.body)
		{
			Vector vec1 = new Vector();
			if(pred == null)
				continue;
			pred.setFunctionHead("m"+pred.getFunctionHead());
			for(Object ob:pred.variables)
			{
				String str = ((String)ob);
				vec1.add("X"+str);
			}
			pred.variables = vec1;
		}


		foo.print();

//		if(j>0)
//		{
//			 BufferedReader in;
//			try {
//				in = new BufferedReader(new FileReader(dir+"/run_"+i+"/views_for_q_"+i+"/view_"+(j-1)+".txt"));
//			} catch (FileNotFoundException e1) {
//				throw new RuntimeException(e1);
//			}
//			
//			 String line;
//			try {
//				line = in.readLine();
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//			
//			while (line != null)
//			{
//				outfile.append(line).append(System.getProperty("line.separator"));
//				try {
//					line = in.readLine();
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//			}
//			
//			try {
//				in.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

		outfile.append(foo.printString()).append(System.getProperty("line.separator"));
//		outfile.close();
	}

	for(int j=81; j<10000; j++)
	{
//		File file = new File(dir+"/run_"+i+"/views_for_q_"+i+"/view_"+j+".txt");
//		try {
//			file.createNewFile();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		PrintWriter outfile;
//		try {
//			outfile = new PrintWriter(file);
//		} catch (FileNotFoundException e) {
//			throw new RuntimeException(e);
//		}

		ChainRandomStatementGenerator a = new ChainRandomStatementGenerator(1, 20, 8, 4, 3, 5);
		Statement foo = a.getRandomStatement(false);
		foo.getHead().setFunctionHead("v"+j);

		Vector vec = new Vector();
		for(Object ob:foo.getHead().variables)
		{
			String str = ((String)ob);
			vec.add("X"+str);
		}
		foo.getHead().variables = vec;

		for(Predicate pred:foo.body)
		{
			Vector vec1 = new Vector();
			if(pred == null)
				continue;
			pred.setFunctionHead("m"+pred.getFunctionHead());
			for(Object ob:pred.variables)
			{
				String str = ((String)ob);
				vec1.add("X"+str);
			}
			pred.variables = vec1;
		}


//		foo.print();

//		if(j>0)
//		{
//			 BufferedReader in;
//			try {
//				in = new BufferedReader(new FileReader(dir+"/run_"+i+"/views_for_q_"+i+"/view_"+(j-1)+".txt"));
//			} catch (FileNotFoundException e1) {
//				throw new RuntimeException(e1);
//			}
//			
//			 String line;
//			try {
//				line = in.readLine();
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//			
//			while (line != null)
//			{
//				outfile.append(line).append(System.getProperty("line.separator"));
//				try {
//					line = in.readLine();
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//			}
//			
//			try {
//				in.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

		outfile.append(foo.printString()).append(System.getProperty("line.separator"));
//		outfile.close();
	}
	outfile.close();
 */