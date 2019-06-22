package com.greenasjade.godojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class J01Application {

    private static final Logger log = LoggerFactory.getLogger(J01Application.class);

    public static void main(String[] args) {
        SpringApplication.run(J01Application.class, args);
    }

    private BoardPositions bp_access;
    private JosekiSources js_access;
    private Tags tags_access;
    private Users user_access;
    private AppInfos app_info_access;


    @Bean
    public CommandLineRunner initialise (
            BoardPositionsNative store_by_bp,
            JosekiSources js_access,
            Tags tags_access,
            Users user_access,
            AppInfos app_info_access
    ) {
        return args -> {
            log.info("Initialising...");

            this.bp_access = new BoardPositions(store_by_bp);
            this.js_access = js_access;
            this.tags_access = tags_access;
            this.user_access = user_access;
            this.app_info_access = app_info_access;

            // note: there should only be one (or zero) app_info in app_infos!
            Iterable<AppInfo> app_info = app_info_access.findAll();

            // First introduction of schema id
            if (!app_info.iterator().hasNext()) {
                AppInfo new_info = new AppInfo();
                new_info.setSchema_id(1);
                migrateToSchema(1);
                //app_info_access.save(new_info);
            }

            BoardPosition rootNode = bp_access.findActiveByPlay(".root");

            if (rootNode == null) {
                resetDB();
            }
            rootNode = bp_access.findActiveByPlay(".root");

            log.info(rootNode.getInfo());
        };
    }

    void migrateToSchema(Integer schema_id){
        switch (schema_id) {
            case 1:
                // This is the introduction of BoardPosition.variation_label
                // with the deprecation of BoardPosition.seq,
                // plus introducing tags
                log.info("Migrating to schema 1...");

                // lets just reset the DB for this
                resetDB();

                return;
            default:
                // should maybe throw here I guess
                log.error("Unrecognised schema ID! " + schema_id);
        }

    }

    void resetDB() {
        log.info("resetting DB...");

        bp_access.deleteEverythingInDB();

        Long GajId = 168L;  // Initial moves came from GreenAsJade!  This is GaJ Beta ID.

        Long AnoekId = 1L;  // He get da powaz too

        // Give'm all da powaz...

        for (Long admin_id: Arrays.asList(GajId, AnoekId)) {
            User admin = new User(admin_id);
            admin.setCanEdit(true);
            admin.setAdministrator(true);
            admin.setCanComment(true);

            user_access.save(admin);

            User check = user_access.findByUserId(admin_id);

            log.info(check.toString());
        }

        // Set up the basic content...

        js_access.save(new JosekiSource("Dwyrin", "https://www.patreon.com/dwyrin", GajId));
        js_access.save(new JosekiSource("Traditional", "", GajId));

        tags_access.save(new Tag("Position is settled"));

        BoardPosition rootNode = new BoardPosition("", "root", GajId);
        rootNode.addComment("Let's do this thing...", GajId);
        rootNode.setDescription("## Empty Board\n\nInfinite possibilities await!", GajId);

        bp_access.save(rootNode);

        log.info("After save of root: " + rootNode.getInfo() );

        Long root_id = rootNode.id;

        BoardPosition child;
        
        child = rootNode.addMove("Q16", GajId);
        child.setVariationLabel('A');
        child = rootNode.addMove("R16", GajId);
        child.setVariationLabel('B');
        child = rootNode.addMove("R17", GajId);
        child.setVariationLabel('C');
        child.setDescription("## San San", GajId);

        child = rootNode.addMove("Q15", PlayCategory.GOOD, GajId);
        child = rootNode.addMove("R15", PlayCategory.GOOD, GajId);

        child = rootNode.addMove("K10", PlayCategory.GOOD, GajId);
        child.setDescription("## Tengen\nDwyrin's favourite!", GajId);

        child = rootNode.addMove("S18", PlayCategory.MISTAKE, GajId);

        bp_access.save(rootNode);

        log.info("After save of root with children: " + rootNode.getInfo() );

        /* Figuring out how/when/what Neo4j loads */
        log.info("Loading and looking at child moves...");

        rootNode = bp_access.findActiveByPlay(".root");
        log.info("reloaded root: " + rootNode.getInfo());

        /* Test reload of a position */

        rootNode = bp_access.findById(root_id);
        log.info("After findById: " + rootNode.getInfo() );

        log.info("Adding second level moves to a node...");

        child.addMove("Q16", GajId);
        child.addMove("R16", GajId);
        child.addMove("R17",GajId);
        bp_access.save(child);

        // Test adding a comment later
        rootNode.addComment("... initial setup in place!", GajId);
        bp_access.save(rootNode);

        // Test changing a category
        child.setCategory(PlayCategory.QUESTION, GajId);
        bp_access.save(child);

        log.info("...DB reset done");
    }
}
