package ti;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.io.FileReader;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;

/**
 * This class contains the logic to run the indexing process of the search engine.
 */
public class Indexer
{
    protected File pathToIndex;
    protected File pathToCollection;
    protected DocumentProcessor docProcessor;

	/**
     * Creates a new indexer with the given paths and document processor.
     * @param pathToIndex path to the index directory.
     * @param pathToCollection path to the original documents directory.
     * @param docProcessor document processor to extract terms.
     */
    public Indexer(File pathToIndex, File pathToCollection, DocumentProcessor docProcessor)
    {
        this.pathToIndex = pathToIndex;
        this.pathToCollection = pathToCollection;
        this.docProcessor = docProcessor;
    }

	/**
     * Run the indexing process in two passes and save the index to disk.
     * @throws IOException if an error occurs while indexing.
     */
    public void run() throws IOException
    {
        Index ind = new Index(this.pathToIndex.getPath());
        this.firstPass(ind);
        this.secondPass(ind);

        // Save index
        System.err.print("Saving index...");
        ind.save();
        System.err.println("done.");
        System.err.println("Index statistics:");
        ind.printStatistics();
    }
    /**
     * Runs the first pass of the indexer.
     * It builds the inverted index by iterating all original document files and calling {@link #processDocument}.
     * @param ind the index.
     * @throws IOException if an error occurs while processing a document.
     */
    protected void firstPass(Index ind) throws IOException
    {
        DecimalFormat df = new DecimalFormat("#.##");
        long startTime = System.currentTimeMillis();
        int totalDocuments = 0;
        long totalBytesDocuments = 0;

        System.err.println("Running first pass...");
        for (File subDir : this.pathToCollection.listFiles()) {
            if (!subDir.getName().startsWith(".")) {
                for (File docFile : subDir.listFiles()) {
                    if (docFile.getPath().endsWith(".html")) {
                        try {
                            System.err.print("  Indexing file " + docFile.getName() + "...");
                            this.processDocument(docFile, ind);
                            System.err.print("done.");
                        } catch (IOException ex) {
                            System.err.println("exception!");
                            System.err.print(ex.getMessage());
                        } finally {
                            System.err.println();
                        }
                        totalDocuments++;
                        totalBytesDocuments += docFile.length();
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000d;
        double totalMegabytes = totalBytesDocuments / 1024d / 1024d;
        System.err.println("...done:");
        System.err.println("  - Documents: " + totalDocuments + " (" + df.format(totalMegabytes) + " MB).");
        System.err.println("  - Time: " + df.format(totalTime) + " seconds.");
        System.err.println("  - Throughput: " + df.format(totalMegabytes / totalTime) + " MB/s.");
    }
    /**
     * Runs the second pass of the indexer.
     * Here it traverses the inverted index to compute and store IDF, update weights in the postings,
     * build the direct index, and compute document norms.
     * @param ind the index.
     */
    protected void secondPass(Index ind)
    {
        DecimalFormat df = new DecimalFormat("#.##");
        long startTime = System.currentTimeMillis();

        System.err.println("Running second pass...");
        System.err.print("  Updating term weights and direct index...");

        // P2
        // recorrer el índice para calcular IDF y actualizar pesos
        double nd = ind.documents.size();
        for(Map.Entry<String,Tuple<Integer,Double> > e : ind.vocabulary.entrySet() ){
            Tuple<Integer,Double> termdoc = e.getValue();
            int termid = termdoc.item1;
            int ct = ind.invertedIndex.get(termid).size();
            
            termdoc.item2 = log(1 + (nd/ct));
            for(Tuple<Integer,Double> t : ind.invertedIndex.get(termid) ){
                t.item2 = termdoc.item2 * (1 + log(t.item2));
                ind.documents.get(t.item1).item2 += t.item2 * t.item2;
            }
            
        }
        
        // Inverted Index [termID] -> (docID, weight)+
        // Direct Index [docID] -> (termID, weight)+
        for(int i = 0; i < ind.invertedIndex.size(); i++){
            for(Tuple<Integer, Double> doc : ind.invertedIndex.get(i))
                ind.directIndex.get(doc.item1).add(new Tuple<>(i, doc.item2));
        }
        
        // Traverse all terms to compute IDF, direct postings, and norm summations

        System.err.println("done.");

        System.err.print("  Updating document norms...");
        
        // P2
        // actualizar normas de documentos
        for(Tuple<String,Double> t : ind.documents)
            t.item2 = sqrt(t.item2);

        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000d;
        System.err.println("done.");
        System.err.println("...done");
        System.err.println("  - Time: " + df.format(totalTime) + " seconds.");
    }
	/**
     * Process the original document in the specified path and add it to the given index.
     * <p>
     * After extracting the document terms, it populates the vocabulary and document structures,
     * and adds the corresponding postings to the inverted index.
     * @param docFile the path to the original document file.
     * @param ind the index to add the document to.
     * @throws IOException if an error occurrs while processing this document.
     */
    protected void processDocument(File docFile, Index ind) throws IOException
    {
        // P2
        
        //BufferedReader b = new BufferedReader(docFile.);
        // leer documento desde disco        
        String pfile;
        String file = new String();
        BufferedReader br = null;
        br = new BufferedReader(new FileReader(docFile.getAbsolutePath()));
        while((pfile = br.readLine()) != null){
            file += pfile + " ";
        }

        // procesarlo para obtener los términos
        // calcular pesos
        // actualizar estructuras del índice: vocabulary documents e invertedIndex
        int docId = ind.documents.size();
        ind.documents.add(new Tuple(docFile.getName().replaceAll(".html", ""), 0.0));
        //P4
        ind.directIndex.add(new ArrayList<>());
        //--P4
        ArrayList<String> tokens = docProcessor.processText(file);
        if(tokens == null)return;
        for (String token : tokens){
            
            Tuple<Integer,Double> term = ind.vocabulary.get(token);
            
            if(term == null){
                int sz = ind.vocabulary.size(); 
                ind.invertedIndex.add(new ArrayList<>());  //[termID] -> (docID, weight)+
                term = new Tuple(sz, 0.0); // Termino ID e IDF
                ind.vocabulary.put(token, term);

            }
            
            ArrayList<Tuple<Integer,Double> > docs = ind.invertedIndex.get(term.item1);
            int docsInTerm = docs.size();

            // Si el término no contiene el documento añadimos una tupla.
            // Esto puede ocurrir tanto si el término es nuevo docsInTerm == 0
            // Como si el término ya existe y contiene documentos pero no el actual.
            if(docsInTerm == 0 || docs.get(docsInTerm-1).item1 != docId ){
                docs.add(new Tuple<Integer,Double>(docId,0.0));
                //P4
                //ind.directIndex.get(docId).add(new Tuple<Integer,Double>(term.item1 , term.item2));
                //--P4
            }
            Tuple<Integer,Double> tuple = docs.get(docs.size()-1); // Guardamos el ftd para calcular posteriormente tf e IDF.
            tuple.item2 += 1.0;
        }
    }
}