package wikiimporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import pl.edu.mimuw.wikiontology.pn347193.Article;
import pl.edu.mimuw.wikiontology.pn347193.ArticleConsumer;
import pl.edu.mimuw.wikiontology.pn347193.Entity;
import pl.edu.mimuw.wikiontology.pn347193.FilteredOntology;
import pl.edu.mimuw.wikiontology.pn347193.Identifier;
import pl.edu.mimuw.wikiontology.pn347193.Ontology;
import pl.edu.mimuw.wikiontology.pn347193.OntologyBuilder;
import pl.edu.mimuw.wikiontology.pn347193.analysis.LinkExtractor;
import pl.edu.mimuw.wikiontology.pn347193.analysis.PhysicistClassifier;
import pl.edu.mimuw.wikiontology.pn347193.graph.Graph;
import pl.edu.mimuw.wikiontology.pn347193.graph.LinksToEdges;
import pl.edu.mimuw.wikiontology.pn347193.predicates.IsAPersonBuilderPredicate;
import pl.edu.mimuw.wikiontology.pn347193.importers.xml.XMLImporter;
import pl.edu.mimuw.wikiontology.pn347193.predicates.AlwaysTruePredicate;
import pl.edu.mimuw.wikiontology.pn347193.predicates.HasARelationPredicate;
import pl.edu.mimuw.wikiontology.pn347193.predicates.Predicate;
import pl.edu.mimuw.wikiontology.pn347193.relations.IsA;

class ArticleCounter implements ArticleConsumer {

    private int count;

    public ArticleCounter() {
        count = 0;
    }

    public int getCount() {
        return count;
    }

    @Override
    public void accept(Article article) {
        count += 1;
    }

}

public class WikiImporter {

    private Ontology ontology;
    private final HashMap<String, Predicate<Entity>> filters;
    private final HashMap<String, Graph> graphs;
    private final Scanner scanner;

    public WikiImporter() {
        ontology = null;
        filters = new HashMap<>();
        filters.put("all", new AlwaysTruePredicate<Entity>());
        filters.put("physicist", new HasARelationPredicate(new IsA(
            Identifier.PHYSICIST)));
        graphs = new HashMap<>();
        scanner = new Scanner(System.in);
    }

    private boolean importXML(String xmlPath) {
        System.out.printf("Importing file %s...\n", xmlPath);

        OntologyBuilder builder = new OntologyBuilder();
        builder.addFilter(new IsAPersonBuilderPredicate());
        builder.addAnalysis(LinkExtractor.getInstance());
        builder.addAnalysis(PhysicistClassifier.getInstance());

        ArticleCounter filteredOutCounter = new ArticleCounter();
        builder.setFilteredArticleConsumer(filteredOutCounter);

        try {
            XMLImporter importer = new XMLImporter(builder);
            FileInputStream input = new FileInputStream(xmlPath);
            importer.parse(input);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.printf("Error: %s\n", e.getMessage());
            return false;
        }

        ontology = builder.getOntology();
        System.out.printf("Imported %d entities, ignored %d articles.\n",
            ontology.entityCount(), filteredOutCounter.getCount());
        return true;
    }

    private void generateGraphs() {
        System.out.println("Generating graphs...");

        for (String filterName : filters.keySet()) {
            System.out.printf("   Using filter \"%s\"...", filterName);

            Predicate<Entity> filter = filters.get(filterName);
            FilteredOntology filteredOntology = new FilteredOntology(
                ontology, filter);
            Graph graph = new Graph(filteredOntology, new LinksToEdges());
            graphs.put(filterName, graph);

            System.out.println(" done.");
        }
    }

    private String readLine() {
        System.out.print("> ");
        return scanner.nextLine();
    }

    private Entity findEntity(String filterName, String title) {
        Graph graph = graphs.get(filterName);
        if (graph == null) {
            System.out.println("Error: invalid filter.");
            return null;
        }

        Entity entity = graph.getOntology().getEntity(new Identifier(title));
        if (entity == null) {
            System.out.printf("Error: the entity \"%s\" does not exist.\n",
                title);
            return null;
        }

        return entity;
    }

    private void repl() {
        Pattern pattern = Pattern.compile(
            "[\\s]*^([a-z]+)[\\s]+([^ ]+)[\\s]+([^ ]+)[\\s]*$");

        while (true) {
            String line = readLine();
            if (line.isEmpty()) {
                return;
            }

            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                String filterName = matcher.group(1).toLowerCase();
                String fromTitle = matcher.group(2);
                String toTitle = matcher.group(3);

                // Find the graph.
                Graph graph = graphs.get(filterName);
                if (graph == null) {
                    System.out.println("Error: invalid filter.");
                    continue;
                }
                
                // Find the entities.
                Entity from = findEntity(filterName, fromTitle);
                Entity to = findEntity(filterName, toTitle);
                if (from == null || to == null)
                    continue;

                // Find and print the path.
                List<Entity> path = graph.shortestPath(from, to);
                printPath(path);
            } else {
                System.out.println("Error: invalid input.");
            }
        }
    }

    private void printPath(List<Entity> path) {
        System.out.println("***");

        if (path.isEmpty()) {
            System.out.println("There is no path.");
        } else {
            System.out.printf("Path length: %d\n", path.size() - 1);
            for (Entity entity : path) {
                System.out.println(entity.getIdentifier().toString());
            }
        }

        System.out.println("***");
    }

    public void run(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: WikiImporter <xml file>");
            return;
        }

        if (!importXML(args[0])) {
            return;
        }

        generateGraphs();
        repl();
    }

    public static void main(String[] args) {
        WikiImporter app = new WikiImporter();
        app.run(args);
    }

}
