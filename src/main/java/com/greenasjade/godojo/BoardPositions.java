package com.greenasjade.godojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

// This should be a native BoardPosition Repository interface
// It started life as a nasty shim to the actual interface is because of
// https://stackoverflow.com/q/55964038/554807
// This shim layer implements logic that would ideally be native in custom queries.

// Now it's accumulating other handy pre-database logic as well

public class BoardPositions {

    private static final Logger log = LoggerFactory.getLogger(BoardPositions.class);

    private BoardPositionsNative bp_access;

    public BoardPositions(
            BoardPositionsNative bp_access) {
        this.bp_access = bp_access;
    }

    void save(BoardPosition position) { bp_access.save(position); }

    void removeParent(Long id) {  bp_access.removeParent(id); }

    // NOTE:  This finds _all_ nodes, including inactive ("deleted") ones
    // Inactive nodes have no parent.   Except "root" which is defined to be active.

    BoardPosition findById(Long id) { return bp_access.findById(id).orElse(null); }

    // These methods are for working with active nodes.
    // An active node must have a parent (or is root)

    BoardPosition findActiveByPlay(String play) {
        List<BoardPosition> candidates = bp_access.findByPlay(play);

        candidates = candidates.stream()
                .filter(
                        candidate -> candidate.parent != null || candidate.getPlacement().equals("root"))
                .collect(Collectors.toList());

        if (candidates.size() > 1) {
            throw(new RuntimeException("More than one active node for play " + play));
        }

        return (candidates.size() == 0 ? null : candidates.get(0));
    }

    // Note that the results here are by definition active (assuming the input node is active)
    List<BoardPosition> findByParentId(Long id) { return bp_access.findByParentId(id); }

    List<BoardPosition> findFilteredVariations(Long targetId,
                                               Long contributorId,
                                               List<Long> tagIds,
                                               Long sourceId) {
        if (tagIds == null) {
            return bp_access.findFilteredVariations(targetId, contributorId, sourceId);
        }
        else {
            return bp_access.findTagFilteredVariations(targetId, tagIds, contributorId,  sourceId);
        }
    }

    Integer countChildren(Long id) {return bp_access.countChildren(id);}

    Integer countChildrenWithTag(Long id, Long tagId) {return bp_access.countChildrenWithTag(id, tagId);}

    // Database Utility function
    //  NOTE THAT THIS DELETES *EVERYTHING* NOT JUST BOARD POSITIONS
    void deleteEverythingInDB() {
        // Do this 10,000 nodes at a time so as not to blow up the database with a huge request
        //  https://neo4j.com/developer/kb/large-delete-transaction-best-practices-in-neo4j/

        J01Application.debug("** Deleting DB contents! ...", log);
        int deleted_nodes;

        do {
            deleted_nodes = bp_access.deleteNodes(10000);
            J01Application.debug("Deleted " + deleted_nodes, log);
        } while (deleted_nodes > 0);

        J01Application.debug("... done.", log);
    }

}
