/**
 * Adapter Class implementing some functionalities useful to perform WSD
 */
package com.sgametrio.wsd;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISenseKey;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Synset;

public class WordnetAdapter {
		
		private String wordnetHome = "/usr/local/WordNet-3.0";//path to WordNet home folder
		private String path = wordnetHome + File.separator + "dict";
		private URL url; 
		private IDictionary dict;
		private Map<String, POS> posMap = new HashMap<String, POS>(); //mapping of StanfordDependencyParser POS tag to WordNet ones

		/**CONSTRUCTORS
		 * 
		 */
		public WordnetAdapter(){
			try{
				this.url = new URL("file", null, path);
			}catch(MalformedURLException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}	
			this.dict = new Dictionary(url);
			this.doMapping();
		}
		
		/**
		 * Maps the StanfordDependencyParser tags to WordNet tags 
		 */
		private void doMapping(){
			
			this.posMap.put("CC", null); 			//coordinating conjunction
			this.posMap.put("CD", null); 			//cardinal number
			this.posMap.put("DT", null); 			//determiner
			this.posMap.put("EX", null); 			//existential there
			this.posMap.put("FW", null); 			//foreign word
			this.posMap.put("IN", null); 			//preposition or subordinating conjunction
			this.posMap.put("JJ", POS.ADJECTIVE);	//adjective
			this.posMap.put("JJR", POS.ADJECTIVE);	//adjective, comparative
			this.posMap.put("JJS", POS.ADJECTIVE);	//adjective, superlative
			this.posMap.put("LS", null);			//list item marker
			this.posMap.put("MD", POS.VERB);		//modal
			this.posMap.put("NN", POS.NOUN);		//noun, singular or mass
			this.posMap.put("NNS", POS.NOUN);		//noun, plural
			this.posMap.put("NNP", POS.NOUN);		//proper noun, singular
			this.posMap.put("NNPS", POS.NOUN);		//proper noun, plural
			this.posMap.put("PDT", null);			//predeterminer
			this.posMap.put("POS", null);			//possessive ending
			this.posMap.put("PRP", null);			//personal pronoun
			this.posMap.put("PRP$", null);			//possessive pronoun
			this.posMap.put("RB", POS.ADVERB);		//adverb
			this.posMap.put("RBR", POS.ADVERB);		//adverb,comparative
			this.posMap.put("RBS", POS.ADVERB);		//adverb, superlative
			this.posMap.put("RP", null);			//particle
			this.posMap.put("SYM", null);			//symbol
			this.posMap.put("TO", null);			//to
			this.posMap.put("UH", null);			//interjection
			this.posMap.put("VB", POS.VERB);		//verb, base form
			this.posMap.put("VBD", POS.VERB);		//verb, past tense
			this.posMap.put("VBG", POS.VERB);		//verb, gerund or present participle
			this.posMap.put("VBN", POS.VERB);		//verb, past participle
			this.posMap.put("VBP", POS.VERB);		//verb, non-3rd person singular present
			this.posMap.put("VBZ", POS.VERB);		//verb, 3rd person singular present
			this.posMap.put("WDT", null);			//wh-determiner
			this.posMap.put("WP", null);			//wh-pronoun
			this.posMap.put("WP$", null);			//possessive wh-pronoun
			this.posMap.put("WRB", null);			//wh-adverb
			//Wordnet Tags for evaluation
			this.posMap.put("VERB", POS.VERB);		//Wordnet verb
			this.posMap.put("NOUN", POS.NOUN);		//Wordnet noun
			this.posMap.put("ADJ", POS.ADJECTIVE);	//Wordnet adjective
			this.posMap.put("ADV", POS.ADVERB);		//Wordnet adverb
			
		}
		
		//METHODS
		/**
		 * Returns a list containing all the glosses of the given word for the given POS
		 * @param wordID: the word for which the glosses have to be retrieved
		 * @return: gloss of the given word for the given POS 
		 */
		public String[] getGloss(IWordID wordID){	
			try{
				
				dict.open();	
				IWord dictword = dict.getWord(wordID);
				String[] glossAndSenseKey = {dictword.getSynset().getGloss(), dictword.getSenseKey().toString()};
				dict.close();
				return glossAndSenseKey;
				
			}catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}catch(NullPointerException e){
				System.err.println("Word \""+wordID.toString()+"\" not found in WordNet.");
			}
			return null;
		}
		
		public String getOnlyGloss(IWordID wordID){	
			try{
				
				dict.open();	
				IWord dictword = dict.getWord(wordID);
				String gloss = dictword.getSynset().getGloss();
				dict.close();
				return gloss;
				
			}catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}catch(NullPointerException e){
				System.err.println("Word \""+wordID.toString()+"\" not found in WordNet.");
			}
			return null;
		}
		
		/**
		 * Returns a list containing all the glosses of the given word for the given POS
		 * @param word: the word for which the glosses have to be retrieved
		 * @param pos: part of speech of the word
		 * @return: a list containing all the word IDS of glosses of the given word for the given POS 
		 */
		public ArrayList<IWordID> getWordsIds(String word, String pos) {
			ArrayList<IWordID> ids = new ArrayList<IWordID>();
			try{			
				dict.open();	
				POS wnPos = this.posMap.get(pos);		
				IIndexWord idxWord = dict.getIndexWord(word, wnPos);
				if (idxWord != null) {
					for(IWordID wordID: idxWord.getWordIDs()){
						ids.add(wordID);
					}
				}
				dict.close();	
			}catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}catch(NullPointerException e){
				System.err.println("Word \""+word+"\" with POS " + pos + "not found in WordNet.");
			}
			return ids;
		}
		
		public IWord getWord(ISenseKey senseKey) {
			IWord word = null;
			try {
				dict.open();
				word = dict.getWord(senseKey);
				dict.close();
			} catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}
			return word;
		}
		
		public IWord getWord(IWordID id) {
			IWord word = null;
			try {
				dict.open();
				word = dict.getWord(id);
				dict.close();
			} catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}
			return word;
		}
		
		public ArrayList<IWord> getSynsetWords(IWordID wordId) {
			ArrayList<IWord> words = new ArrayList<IWord>();
			try {
				dict.open();
				for (IWord word : dict.getWord(wordId).getSynset().getWords()) {
					words.add(word);
				}
				dict.close();
			} catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}
			
			return words;
		}
		
		/**
		 * Return IWords list of semantically related synset of wordId synset
		 * @param wordId
		 * @return list containing IWords from semantically related synset of wordId synset
		 */
		public ArrayList<IWord> getRelatedSynsetWords(IWordID wordId) {
			ArrayList<IWord> words = new ArrayList<IWord>();
			try {
				dict.open();
				for (ISynsetID synsetID : dict.getWord(wordId).getSynset().getRelatedSynsets()) {
					for (IWord word : dict.getSynset(synsetID).getWords()) {
						words.add(word);
					}
				}
				dict.close();
			} catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}
			
			return words;
		}
		
		//GETTER METHODS
		public Map<String, POS> getPosMap(){
			return this.posMap;
		}
		
		public String getWordnetHome(){
			return this.wordnetHome;
		}
		
		//SETTER METHODS
		public void setWordnetHome(String wordnetHomePath){
			this.wordnetHome = wordnetHomePath;
		}

		public ArrayList<IWord> getWordsList(String word, String pos) {
			ArrayList<IWord> words = new ArrayList<IWord>();
			try{			
				dict.open();	
				POS wnPos = this.posMap.get(pos);		
				IIndexWord idxWord = dict.getIndexWord(word, wnPos);
				if (idxWord != null) {
					for(IWordID wordID: idxWord.getWordIDs()){
						words.add(dict.getWord(wordID));
					}
				}
				dict.close();	
			}catch(IOException e){
				System.err.print(Thread.currentThread().getStackTrace()[1].getMethodName()+" threw: ");
				System.err.println(e);
			}catch(NullPointerException e){
				System.err.println("Word \""+word+"\" with POS " + pos + "not found in WordNet.");
			}
			return words;
		}
		
}
