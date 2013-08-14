/*******************************************************************************
 * Forwarding on Gates Simulator/Emulator
 * Copyright (C) 2012, Integrated Communication Systems Group, TU Ilmenau.
 * 
 * This program and the accompanying materials are dual-licensed under either
 * the terms of the Eclipse Public License v1.0 as published by the Eclipse
 * Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 ******************************************************************************/
package de.tuilmenau.ics.fog.transfer.manager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import de.tuilmenau.ics.fog.Config;
import de.tuilmenau.ics.fog.FoGEntity;
import de.tuilmenau.ics.fog.facade.Description;
import de.tuilmenau.ics.fog.facade.properties.FunctionalRequirementProperty;
import de.tuilmenau.ics.fog.facade.properties.Property;
import de.tuilmenau.ics.fog.ui.Logging;
import de.tuilmenau.ics.fog.util.Logger;
import de.tuilmenau.ics.middleware.Serializer;


/**
 * Maps requirements given by applications to series of gate types.
 * It uses a modified context free grammar to implement the mapping. 
 */
public class RequirementsToGatesMapper
{
	private static final String DEFAULT_START_VARIABLE = "Sa";
	
	private static final String DEFAULT_FILE_NAME = "configuration\\languageEndHosts.cfl";
	private static final int MAXIMUM_RECURSION_DEPTH = 20;
	
	private static final String PROPERTY_PACKAGE_NAME = "de.tuilmenau.ics.fog.facade.properties.";
	private static final String PROPERTY_CLASS_NAME_POST_FIX = "Property";
	
	private static final boolean DEBUG_WORD_CREATION = true;
	
	private static RequirementsToGatesMapper sGlobalInstance = null;
	
	/**
	 * Factory method for mapper objects.
	 * 
	 * @param forNode TODO use specific node configurations in order to create different requ. mapper
	 * @return != null
	 */
	public static RequirementsToGatesMapper getInstance(FoGEntity forNode)
	{	
		if(sGlobalInstance == null) {
			sGlobalInstance = new RequirementsToGatesMapper();
			
			// init global instance
			sGlobalInstance.init();
		}
		
		return sGlobalInstance;
	}
	
	/**
	 * Private constructor prevents others from not using the factory method.
	 */
	private RequirementsToGatesMapper()
	{
	}
	
	public class Variable
	{
		public Variable(String name, Rule rule)
		{
			this.name = name.trim();
			this.rule = rule;
		}
		
		public Variable(Variable var, Rule rule)
		{
			this.name = var.getName();
			this.rule = rule;
		}
		
		public String getName()
		{
			return name;
		}
		
		public Rule getRule()
		{
			return rule;
		}
		
		public boolean isStreamVariable()
		{
			return name.matches("S.");
		}
		
		/**
		 * Derives IProperty class name from rule name.
		 * 
		 * @return Class name of a class supporting IProperty or null if no class available
		 */
		public Class getPropertyClass()
		{
			if(rule == null) return null;
			if(rule.getRuleName() == null) return null;
			
			try {
				String packageName = PROPERTY_PACKAGE_NAME;
				
				if(rule.getRuleName().startsWith("Video")) {
					packageName = "de.tuilmenau.ics.fog.video.properties.";
				}
				return Serializer.getInstance().getClassByName(packageName +rule.getRuleName() +PROPERTY_CLASS_NAME_POST_FIX);
			}
			catch(ClassNotFoundException exc) {
				logger.warn(this, "No property class available.", exc);
				return null;
			}
		}
		
		public boolean equals(Object obj)
		{
			if(obj == this) return true;
			
			if(obj instanceof Variable) {
				return ((Variable) obj).name.equals(name);
			}
			else if(obj instanceof String) {
				return obj.equals(name);
			}
			else {
				return false;
			}
		}
		
		public int hashCode()
		{
			return name.hashCode();
		}
		
		public String toString()
		{
			if(rule != null) {
				if(rule.getRuleName() != null) {
					// Note: Use "rule.getRuleName" and not "rule" in order to prevent endless recursion
					return name +"(" +rule.getRuleName() +")";
				}
			}
			
			return name;
		}
		
		private String name;
		private Rule rule;
	}
	
	private class Rule
	{
		public Rule(String ruleName, Variable from, String to, String[] OptimisationCriterion)
		{
			this.from = from;
			this.ruleName = ruleName;
			this.OptimisationCriterion=OptimisationCriterion;
			this.to = new Word(to, this);
			
			
		}
		
		public Variable getFrom()
		{
			return from;
		}
		
		public Word getTo()
		{
			return to;
		}
		
		public boolean isNamed()
		{
			return ruleName != null;
		}
		
		public String getRuleName()
		{
			return ruleName;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(obj == this) return true;
			if(obj == null) return false;
			
			if(obj instanceof String) {
				return obj.equals(ruleName);
			}
			
			if(obj instanceof Property) {
				String name = obj.getClass().getSimpleName();

				// Modify class names in order to match names from language file 
				name = name.replaceAll(PROPERTY_CLASS_NAME_POST_FIX +"$", "");
				
				return name.equals(ruleName);
			}
			
			if(obj instanceof Rule) {
				Rule objRule = (Rule) obj;
				
				if(ruleName != null) {
					return ruleName.equals(objRule.ruleName);
				} else {
					return from.equals(objRule.from) && to.equals(objRule.to);
				}
			}
			
			return false;
		}
		
		@Override
		public String toString()
		{
			StringBuffer buffer = new StringBuffer();
			buffer.append(from);
			buffer.append(" -> ");
			
			for(Variable toitem : to) {
				buffer.append(toitem);
			}
			
			return buffer.toString();
		}
		
		public String[] getOptimisationCriterions() {
			return this.OptimisationCriterion;
		}
		
		private Variable from;
		private Word to;
		private String ruleName;
		private String[] OptimisationCriterion = null;
	}
	
	public class Word implements Iterable<Variable>
	{
		public Word(String strWord, Rule rule)
		{
			String[] array = strWord.split("\\s+");
			
			for(int i = 0; i < array.length; i++) {
				String item = array[i].trim();
				
				if(item.length() > 0) {
					word.addLast(new Variable(item, rule));
				}
			}
			
			size = word.size();
			
			if((word.size() > 0) && (rule != null)) {
				word.addFirst(rule);
				word.addLast(rule);
			}
			
		}
		
		public Word(Word orig)
		{
			word = (LinkedList<Object>) orig.word.clone();
			size = orig.size;
			numberNamedRules = orig.numberNamedRules;
			numberRulesTotal = orig.numberRulesTotal;
			this.OptimisationCriterions.addAll(orig.OptimisationCriterions);
			
		}
		
		public int size()
		{
			return size;
		}
		
		private int getIndex(int index, HashMap<String, Rule> rules)
		{
			int i = 0;
			int realIndex = 0;
			
			for(Object obj : word) {
				if(obj instanceof Variable) {
					if(index == i) return realIndex;
					else i++;
				}
				else if((obj instanceof Rule) && (rules != null)) {
					String ruleName = ((Rule) obj).getRuleName();
					if(rules.containsKey(ruleName)) {
						rules.remove(ruleName);
					} else {
						rules.put(ruleName, (Rule) obj);
					}
				}
				
				realIndex++;
			}
			
			throw new RuntimeException("No element " +index +" in " +this);
		}
		
		public Word replace(int index, Rule replaceWith, boolean forced)
		{
			int realIndex = getIndex(index, null);
			
			if(replaceWith.getFrom().equals(word.get(realIndex))) {
				Word newWord = new Word(this);
				
				
				Object removedObj = newWord.word.remove(realIndex);
				
				//newWord.word.addAll(realIndex, replaceWith.getTo().word);
				int i = 0;
				for(Object element : replaceWith.getTo().word) {
					if(forced && (element instanceof Variable) && (removedObj instanceof Variable)) {
						element = new Variable((Variable) element, ((Variable) removedObj).getRule());
					}
					
					newWord.word.add(realIndex +i, element);
					
					if(element instanceof Rule) {
						Rule elementRule = (Rule) element;
						String[] OptimisationCriterions = elementRule.getOptimisationCriterions();
						
						if(OptimisationCriterions!=null) {
							for(int j=0; j< OptimisationCriterions.length; j++) {
								newWord.OptimisationCriterions.add(OptimisationCriterions[j].trim());
							}
						}
					}
					
					i++;
				}
				
				newWord.size += replaceWith.getTo().size() -1;
				
				if(replaceWith.isNamed()) newWord.numberNamedRules++;
				newWord.numberRulesTotal++;

				return newWord;
			} else {
				throw new RuntimeException("Can not use rule " +replaceWith +" at index " +index);
			}
		}
		
		public Variable get(int index)
		{
			return (Variable) word.get(getIndex(index, null));
		}
		
		public HashMap<String, Rule> getRulesFor(int index)
		{
			HashMap<String, Rule> res = new HashMap<String, RequirementsToGatesMapper.Rule>();
			
			getIndex(index, res);
			return res;
		}
		
		public Property getRequirementFor(int i, Description requirements)
		{
			Variable var = get(i);
			Class<?> requClazz = var.getPropertyClass();
			
			if(requirements == null) return null;
			else return requirements.get(requClazz);
		}
		
		public int getNumberNamedRules()
		{
			return numberNamedRules;
		}
		
		public int getNumberRulesTotal()
		{
			return numberRulesTotal;
		}
		
		public double satisfies(Description requirements)
		{
			int nonFunctionalSatisfyCounter = 0;
			int notFullfilledRequirements = 0;
			int functionalRequirements = 0;
			Description FunctionalRequ = requirements.getFunctional();
			Description nonFunctionalReq = requirements.getNonFunctional();
			
			// check for any not fullfilled requirements
			for(Property FuncRequ : FunctionalRequ) {
				functionalRequirements++;	
				if(!satisfiesRequirement(FuncRequ)) {
					notFullfilledRequirements++;
				}
			}
			
			if(Config.Connection.OPTIMISATION_CRITERIONS_ACTIVATED) {
				if(!nonFunctionalReq.isEmpty() && !this.OptimisationCriterions.isEmpty()) {
					//Non Functional Requirements
					for(Property NonFuncRequ : nonFunctionalReq) { 
						String Requirement = NonFuncRequ.toString();
						String ShortRequirement = Requirement.substring(0,Requirement.indexOf("Property")); //From Description																							
					
						if(this.OptimisationCriterions.contains(ShortRequirement)) {
							nonFunctionalSatisfyCounter++;
						} 
					}
				}
			}
			
			if(notFullfilledRequirements == 0) {
				// more requirements than requested?
				if(functionalRequirements > 0) {
					double satisfyNonFunc = 0;
					satisfyNonFunc = (double) numberNamedRules / ((double) functionalRequirements);
					
					//------------- Match += N+(Z*W1)*W2 ---------
					if(Config.Connection.OPTIMISATION_CRITERIONS_ACTIVATED ) { 
						double notSatisfiedOptiCrits = (nonFunctionalReq.size() - nonFunctionalSatisfyCounter >= 0 ) 
								? nonFunctionalReq.size() - nonFunctionalSatisfyCounter : 0 ; 
						double OverprovisioningOptiCrits = (OptimisationCriterions.size() - nonFunctionalSatisfyCounter >= 0 ) 
								? OptimisationCriterions.size() - nonFunctionalSatisfyCounter : 0 ;
						OverprovisioningOptiCrits = (notSatisfiedOptiCrits == 0) ? 0 : OverprovisioningOptiCrits;
						satisfyNonFunc +=  ((notSatisfiedOptiCrits + (OverprovisioningOptiCrits*Weight1)) * Weight2);
					}
					return satisfyNonFunc;
					
				} else {
					if(numberNamedRules == 0) return 1.0d;        // no requested, no in solution = optimal
					else return (double) numberNamedRules / 0.1d; // generate a high value
				}
			} else {
				// solution not suitable anyway
				// Note: requirements.size() != 0 since notFullfilledRequirements > 0
				return (double) (functionalRequirements -notFullfilledRequirements) / (double) functionalRequirements;
			}
		}
		
		private boolean satisfiesRequirement(Property requ)
		{
			for(Object obj : word) {
				if(obj instanceof Rule) {
					if(requ instanceof FunctionalRequirementProperty) {
						if(obj.equals(requ)) return true;
					}
					
				}				
			}
			
			return false;
		}
		
		private class VariableIterator implements Iterator<Variable>
		{
			public VariableIterator(Iterator<Object> iter)
			{
				iterator = iter;
			}
			
			@Override
			public boolean hasNext()
			{
				if(!nextValid) {
					while(iterator.hasNext()) {
						Object obj = iterator.next();
						if(obj instanceof Variable) {
							nextValid = true;
							next = (Variable) obj;
							break;
						}
					}
				}
				
				return nextValid;
			}

			@Override
			public Variable next()
			{
				if(!nextValid) {
					hasNext();
				}
				
				if(nextValid) {
					nextValid = false;
					return next;
				} else {
					throw new RuntimeException("No next element for " +this);
				}
			}

			@Override
			public void remove()
			{
				// TODO Auto-generated method stub
			}
			
			private Iterator<Object> iterator;
			private boolean nextValid = false;
			private Variable next = null;
		}
		
		@Override
		public Iterator<Variable> iterator()
		{
			return new VariableIterator(word.iterator());
		}
		
		public Iterator<Variable> descendingIterator()
		{
			return new VariableIterator(word.descendingIterator());
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if(obj == this) return true;

			if(obj instanceof Word) {
				Word w = (Word) obj;
				
				return w.word.equals(word);
			}
			
			return false;
		}
		
		@Override
		public String toString()
		{
			// use long output or short one?
			if(DEBUG_WORD_CREATION) {
				return word.toString();
			} else {
				Iterator<Variable> iter = iterator();
				StringBuffer result = new StringBuffer();
				
				result.append("[");
				while(iter.hasNext()) {
					result.append(iter.next());
					
					if(iter.hasNext()) result.append(", ");
				}
				result.append("]");
				return result.toString();
			}
		}
		
		/*public void setUsed(boolean palreadyUsed) {
			used = palreadyUsed;
		}
		
		public boolean getUsed() {
			return used;
		}*/
		
		private Set<String> OptimisationCriterions = new HashSet<String>();
		private LinkedList<Object> word = new LinkedList<Object>();
		private int size = 0;
		private int numberNamedRules = 0;
		private int numberRulesTotal = 0;
		private final static double Weight1 = 0.75;
		private final static double Weight2 = 0.75;

	}
	
	private void addRule(String ruleName, String fromName, String to, String[] OptimisationCriterion)
	{
		Variable from = new Variable(fromName, null);
		LinkedList<Rule> tos = rules.get(from);
		
		if(tos == null) {
			tos = new LinkedList<Rule>();
			rules.put(from, tos);
		}
		
		Rule rule = new Rule(ruleName, from, to, OptimisationCriterion);
		logger.log(this, "Rule: " +rule + " (" +ruleName +")");
		tos.add(rule);
	}
	
	/**
	 * Adds a word to list of solutions if it is not already included.
	 * 
	 * @param solutionList List to which the solution should be added
	 * @param word Solution to add to list
	 * @param usedRules Meta infos about solution
	 * @param incomplete If word is a list of terminators only
	 */
	private void addSolution(LinkedList<Word> solutionList, Word word, boolean incomplete)
	{
		if(addSolution(solutionList, word)) {
			if(incomplete) {
				logger.log(this, "incomplete solution = " +word);
			} else {
				logger.log(this, "solution = " +word);
			}
		} else {
			if(DEBUG_WORD_CREATION) {
				logger.trace(this, "existing solution = " +word);
			}
		}
	}
	
	private static boolean addSolution(LinkedList<Word> solutionList, Word solution)
	{
		boolean existsAlready = false;
		
		for(Word sol : solutionList) {
			if(sol.equals(solution)) {
				existsAlready = true;
				break;
			}
		}
		
		if(!existsAlready) {
			solutionList.add(solution);
			return true;
		} else {
			return false;
		}
	}
	
	public Word getSolutionFor(Description requirements)
	{
		LinkedList<Word> res = new LinkedList<Word>();
		
		// find all solution satisfying the requirements
		for(Word sol : solutions) {
			double match = sol.satisfies(requirements);
			
			logger.trace(this, sol +" matches " +match);
			
			if(match == 1.0d) {
				// exact match, is the best one
				res.addFirst(sol);
			}
			else if(match > 1.0d) {
				// "over-provisioning" is not so good
				res.addLast(sol);
			}
			// else: no match, since not all requ. in
		}
		
		logger.log(this, "Found " +res.size() +" possible solutions for " +requirements);
		
		// select on of the solutions
		if(res.size() > 0) {		
			logger.log(this, "Selecting solution " +res.getFirst());
			return res.getFirst();
		} else {
			return null;
		}
	}
	
	
	public void init()
	{
		rules.clear();
		solutions.clear();
		warnInfiniteRecursion = true;
		
		int importedRules = readLanguage(DEFAULT_FILE_NAME);
		logger.info(this, "Number imported rules: " +importedRules);
		
		// create start word containing only the start variable
		Word word = new Word(DEFAULT_START_VARIABLE, null);
		
		createTree(0, word);
		logger.info(this, "Number possible solutions: " +solutions.size());
	}
	
	private int readLanguage(String filename)
	{
		InputStream fileStream = getClass().getClassLoader().getResourceAsStream("/" +filename);
		BufferedReader reader = null;
		int rulesImported = 0;
		
		try {
			reader = new BufferedReader(new InputStreamReader(fileStream));
			String rule = null;

			while ((rule = reader.readLine()) != null) {
				// filter comments and empty lines
				if(!rule.matches("\\s*\\#.*") && !rule.matches("\\s*") ) {
					String[] parts = rule.split("->");
					if(parts.length == 2) {
						String[] conditions = parts[0].split(":");
						String[] possibilities = parts[1].split("\\|");
						String[] OptimisationCriterion = null;
						
						if(possibilities[possibilities.length-1].contains(":")) {
						    String[] TempSplit = possibilities[possibilities.length-1].split(":");
						    if(TempSplit[1].contains(",")) {
						    	// If we got more than one NonFunctionalRequ they are splitted by ","
						    	OptimisationCriterion = TempSplit[1].split(",");
						    } else {// Only one NonFunctionalRequirement
						    	// If we get only one NonFunctionalRequirement, we have to fit it to String[]
						    	String[] TempStringArray = new String[1];
						    	TempStringArray[0] = TempSplit[1].trim();
						    	OptimisationCriterion = TempStringArray;
						    }
						    	
						}
						
						String condition;
						String ruleName = null;
						
						if(conditions.length > 1) {
							condition = conditions[1];
							ruleName = conditions[0];
						} else {
							condition = conditions[0];
						}
	
						// at least one entry (original string), if no splitting done
						for(String poss : possibilities) {
							if(poss.contains(":")) {
								poss = poss.substring(0,poss.indexOf(":"));
							}
							addRule(ruleName, condition, poss, OptimisationCriterion);
							rulesImported++;
						}
					} else {
						logger.warn(this, "line " +rule +" does not contain a '->'. Ignoring it.");
					}
				}
			}
		}
		catch(Exception exc) {
			logger.err(this, "Error while parsing", exc);
		}
		
		return rulesImported;
	}
	
	private Word doingForcedRules(Word wordOrig)
	{
		int i = 0;
		Word word = wordOrig;
		
		while(i < word.size()) {
			Variable wordToReplace = word.get(i);
			LinkedList<Rule> possibleRules = rules.get(wordToReplace);
			
			if(possibleRules != null) {
				// is there is only a single possibility to replace the variable
				// with, do it
				if(possibleRules.size() == 1) {
					// check if rule should be used
					Rule rule = possibleRules.getFirst();
					boolean useRule = true;
					
					if(rule.isNamed()) {
						HashMap<String, Rule> usedRules = word.getRulesFor(i);
						
						useRule = !usedRules.containsKey(rule.getRuleName());
//						numberUsedNamedRules++;
					}
					
					// doing recursive call for next rule
					if(useRule) {
						word = word.replace(i, rule, true); // TODO forced?
					} else {
						i++;
					}
				} else {
					i++;
				}
			} else {
				i++;
			}
		}
		
		return word;
	}
	
	private int createTree(int recursion, Word word)
	{
		boolean onlyTerminators = true;
		int numberUsedNamedRules = 0;
		int sumChildrenNumberUsedNamedRules = 0;
		
		word = doingForcedRules(word);
		
		for(int i = 0; i < word.size(); i++) {
			Variable wordToReplace = word.get(i);
			LinkedList<Rule> possibleRules = rules.get(wordToReplace);
			
			if(possibleRules != null) {
				onlyTerminators = false;
				
				// restrict maximum iteration depth, in order to break endless loops from some languages
				if(recursion < MAXIMUM_RECURSION_DEPTH) {
					for(Rule rule : possibleRules) {
						
						// check if rule should be used
						boolean useRule = true;
						HashMap<String, Rule> usedRules = word.getRulesFor(i);
						
						if(rule.isNamed()) {
							useRule = !usedRules.containsKey(rule.getRuleName());
							numberUsedNamedRules++;
						}
						
						// doing recursive call for next rule
						if(useRule) {
							Word wordNew = word.replace(i, rule, false);
							
							int childrenNumberUsedNamedRule = createTree(recursion +1, wordNew);
							sumChildrenNumberUsedNamedRules += childrenNumberUsedNamedRule;
						}
					}
				} else {
					if(warnInfiniteRecursion) {
						warnInfiniteRecursion = false;
						logger.warn(this, "Infinite recursion while generating all solutions. Example: " +word +". All further warnings will be suppressed.");
					}
					// else: ignore it, due to "infinite" recursion
				}	
			}
		}
		
		if(onlyTerminators) {
			addSolution(solutions, word, false);
		}

		return numberUsedNamedRules +sumChildrenNumberUsedNamedRules;
	}
	
	
	private Logger logger = Logging.getInstance();
	private HashMap<Variable, LinkedList<Rule>> rules = new HashMap<Variable, LinkedList<Rule>>();
	private LinkedList<Word> solutions = new LinkedList<Word>();
	private boolean warnInfiniteRecursion = true;
}
