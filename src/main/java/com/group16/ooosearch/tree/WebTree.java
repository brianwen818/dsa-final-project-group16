package com.group16.ooosearch.tree;

import com.group16.ooosearch.model.WebPage;

/**
 * 網站樹（由 HW6 的 WebTree 整理而來）。
 * Stage 0 只提供資料結構；後序走訪計分、印出樹狀結構由 Stage 2 實作。
 */
public class WebTree {
    private final WebNode root;

    public WebTree(WebPage rootPage) {
        this.root = new WebNode(rootPage);
    }

    public WebNode getRoot() {
        return root;
    }

    /** 樹中的節點總數。 */
    public int size() {
        return size(root);
    }

    private int size(WebNode node) {
        int count = 1;
        for (WebNode child : node.getChildren()) {
            count += size(child);
        }
        return count;
    }
}
