package latexNGramDuplicateFinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class App {

	public static final String pathToLaTeXDir = "enter path here";  
	public static final String newLine = System.getProperty("line.separator");

	public static void main(String[] args) {
		ArrayList<String> texFiles = new ArrayList<>();
		addTexFiles(texFiles, pathToLaTeXDir);

		StringBuilder sb = new StringBuilder();

		for (String path : texFiles) {
			String texContents = FileIO.readFile(path);
			sb.append(texContents).append(newLine);
		}

		String complete = sb.toString();
		
		
		// Remove comments and write to complete.tex
		complete = preprocessRemoveComments(complete);
		FileIO.writeFile("complete.tex", complete);
		System.out.println("Number of lines: " + complete.split(newLine).length);

		// Remove lines 
		complete = preprocessRemoveLines(complete);
		System.out.println("Number of lines: " + complete.split(newLine).length + ", number of tokens: "
				+ complete.split(" ").length);

		String[] allWords = getWords(complete); 
		
		int minLength = 1; 
		int maxLength = 10; 
		for (int ngramLength=minLength;ngramLength<=maxLength;ngramLength++) {
			
			LinkedHashMap<String, ArrayList<Integer>> sortedNGrams = detectNGrams(complete, ngramLength);
			
			int topN = 10; 
			int i=0; 
			
			// Print topN ngrams
			for (Entry<String, ArrayList<Integer>> entry : sortedNGrams.entrySet()) {
				if (i >= topN) break; 
				
				if (entry.getValue().size() == 1) 
					break; 
				
				/*System.out.println(entry.getValue().size() + " entries " + 
						//entry.getValue().toString()  +
						" for ngram='" + entry.getKey() + "'");*/
				i++; 
			}
			
			
			// Repeated entries of ngrams directly after each other
			for (Entry<String, ArrayList<Integer>> entry : sortedNGrams.entrySet()) {
				String ngram = entry.getKey(); 
				List<Integer> ngramPositions = entry.getValue(); 
				// sort by position
				ngramPositions = ngramPositions.stream()
									.sorted()
									.collect(Collectors.toList()); 
				
				for (int positionIndex=0;positionIndex<ngramPositions.size()-1;positionIndex++) {
					int pos1 = ngramPositions.get(positionIndex);
					int pos2 = ngramPositions.get(positionIndex+1); 
					
					if (Math.abs(pos1-pos2) == ngramLength) {
						System.out.println("Repeated ngram='" + ngram + "' found in pos1=" + pos1 + ", pos2=" + pos2 + 
								". Surrounding: '" + concat(allWords, pos1-10, pos2+10));
					}
					
					
				}
			}
			
			
		}
		
		
		
		
	}

	/**
	 * 
	 * @param ngramLength How many words in sequence?
	 */
	static LinkedHashMap<String, ArrayList<Integer>> detectNGrams(String input, int ngramLength) {
		// String[] tokens = input.split(" ");
		System.out.println("Running with ngram length=" + ngramLength + "...");
		Map<String,ArrayList<Integer>> result = getNGrams(ngramLength, input); 
		System.out.println("Found " + result.keySet().size() + " ngrams with length=" + ngramLength);
		
		
		LinkedHashMap<String, ArrayList<Integer>> sortedResult = new LinkedHashMap<>();
        
		result.entrySet()
		    .stream()
		    //.sorted(Map.Entry.comparingByValue())
		    .sorted(Comparator.comparing(entry -> - entry.getValue().size()))  // - indices.size())); 
		    .forEachOrdered(x -> sortedResult.put(x.getKey(), x.getValue()));
			
		
		return sortedResult; 
		
	}
	
	
	public static String[] getWords(String complete) {
		String[] words = complete.split(" "); 
		return words; 
	}
	
	
	public static Map<String,ArrayList<Integer>> getNGrams(int n, String str) {
		//List<String> ngrams = new ArrayList<String>();
		Map<String,ArrayList<Integer>> ngramLocations = new HashMap<>(); 
		
		// Which words should not be counted
		ArrayList<String> exceptions = new ArrayList<>(); 
		exceptions.add("");
		exceptions.add("\\"); 
		exceptions.add("0mm"); 
		exceptions.add("\\centering"); 
		exceptions.add("\\includegraphics"); 
		exceptions.add("\\t"); 
		exceptions.add("+(0,0)"); 
		exceptions.add("+(0,1)"); 
		exceptions.add("(1,3"); 
		exceptions.add("(1,4)");
		exceptions.add("+(0,-0.2)"); 
		exceptions.add("+(0,-0.8)"); 
		
		String[] words = str.split(" ");
		for (int i = 0; i < words.length - n + 1; i++) {
			// If any of the words is an exception, skip this ngram
			
			words[i] = words[i].trim(); 
			boolean skip = false; 
			for (int j=i;j<i+n;j++) {
				if (exceptions.contains(words[j]) ||
					words[j].contains("\\")) {
					skip = true; 
				}
			}
			if (skip) continue; 
			
			
			String ngram = concat(words, i, i + n); 
			if (ngramLocations.get(ngram) == null) {
				ngramLocations.put(ngram, new ArrayList<Integer>()); 
			}
			ngramLocations.get(ngram).add(i); 
		}
		return ngramLocations;
	}
	
	

	public static String concat(String[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append((i > start ? " " : "") + words[i]);
		return sb.toString();
	}

	static String preprocessRemoveComments(String input) {
		StringBuilder output = new StringBuilder();
		// Remove comments
		for (String line : input.split(newLine)) {
			if (!line.startsWith("%")) {
				output.append(line).append(newLine);
			}
		}
		return output.toString();
	}

	static String preprocessRemoveLines(String input) {
		StringBuilder output = new StringBuilder();
		for (String line : input.split(newLine)) {
			output.append(line).append(" ");
		}
		return output.toString();
	}

	static void addTexFiles(ArrayList<String> texFiles, String pathDir) {
		File dir = new File(pathDir);
		for (String nameFile : dir.list()) {
			String path = pathDir + "\\" + nameFile;
			File file = new File(path);
			if (file.isDirectory() == false) {
				if (nameFile.contains(".tex")) {
					texFiles.add(pathDir + "\\" + nameFile);
					System.out.println("Adding file: " + nameFile);
				}

			} else {
				addTexFiles(texFiles, path);
			}

		}

	}

}
