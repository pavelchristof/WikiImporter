package wikiimporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import pl.edu.mimuw.wikiontology.pn347193.Article;
import pl.edu.mimuw.wikiontology.pn347193.ArticleConsumer;
import pl.edu.mimuw.wikiontology.pn347193.Entity;
import pl.edu.mimuw.wikiontology.pn347193.Identifier;
import pl.edu.mimuw.wikiontology.pn347193.OntologyBuilder;
import pl.edu.mimuw.wikiontology.pn347193.analysis.LinkExtractor;
import pl.edu.mimuw.wikiontology.pn347193.analysis.PhysicistClassifier;
import pl.edu.mimuw.wikiontology.pn347193.filters.PersonFilter;
import pl.edu.mimuw.wikiontology.pn347193.importers.xml.XMLImporter;
import pl.edu.mimuw.wikiontology.pn347193.relations.IsA;
import pl.edu.mimuw.wikiontology.pn347193.relations.Relation;

class ArticleTitlePrinter implements ArticleConsumer {

    @Override
    public void accept(Article article) {
        System.out.printf("Filtered out article %s.\n", article.getTitle());
    }

}

public class WikiImporter {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: WikiImporter <xml file>");
            return;
        }

        OntologyBuilder builder = new OntologyBuilder();
        builder.addFilter(PersonFilter.getInstance());
        builder.addAnalysis(LinkExtractor.getInstance());
        builder.addAnalysis(PhysicistClassifier.getInstance());
        builder.setFilteredArticleConsumer(new ArticleTitlePrinter());

        try {
            XMLImporter importer = new XMLImporter(builder);
            FileInputStream input = new FileInputStream(args[0]);
            importer.parse(input);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println("Cannot read the xml file.");
            System.out.println(e.getLocalizedMessage());
        }

        ArrayList<Entity> ontology = builder.build();
        System.out.println(String.format("Imported %d ontologies.", ontology.
            size()));
        for (Entity entity : ontology) {
            System.out.print(entity.getIdentifier().toString());
            boolean isPhysicist = entity.hasRelation(new IsA(
                Identifier.PHYSICIST));
            if (isPhysicist) {
                System.out.println(" is a physicist.");
            } else {
                System.out.println(" is not a physicist.");
            }
        }
        
        Entity entity = ontology.get(0);
        System.out.println(entity.getIdentifier().toString());
        for (Relation rel : entity.getRelations()) {
            System.out.println("    " + rel.toString());
        }
    }

}
