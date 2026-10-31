package com.group16.ooosearch.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

class WebTreeTest {
    private static WebNode node(double score) {
        WebPage page = new WebPage("https://example.com/" + score, "", "");
        WebNode node = new WebNode(page);
        node.setPageResult(new PageResult(page, score, Map.of()));
        return node;
    }

    @Test
    void getDepthDoesNotChangeWhenCalledRepeatedly() {
        WebNode root = node(1);
        WebNode child = node(1);
        WebNode grandchild = node(1);
        root.addChild(child);
        child.addChild(grandchild);

        assertEquals(0, root.getDepth());
        assertEquals(1, child.getDepth());
        assertEquals(1, child.getDepth());
        assertEquals(2, grandchild.getDepth());
        assertEquals(2, grandchild.getDepth());
    }

    @Test
    void totalScoreAppliesDepthDecayInPostOrder() {
        WebTree tree = new WebTree(new WebPage("https://example.com/", "", ""));
        WebNode root = tree.getRoot();
        root.setPageResult(new PageResult(root.getWebPage(), 100, Map.of()));
        WebNode child = node(40);
        WebNode grandchild = node(20);
        root.addChild(child);
        child.addChild(grandchild);

        assertEquals(125.0, tree.calculateTotalScores(0.5), 1e-9); // 100 + 40×0.5 + 20×0.25
        assertEquals(25.0, child.getNodeScore(), 1e-9);
        // 重複計算結果相同
        assertEquals(125.0, tree.calculateTotalScores(0.5), 1e-9);
    }
}
