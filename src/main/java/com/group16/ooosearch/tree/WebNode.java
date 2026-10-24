package com.group16.ooosearch.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.group16.ooosearch.model.PageResult;
import com.group16.ooosearch.model.WebPage;

/**
 * 網站樹的節點（由 HW6 的 WebNode 整理而來）。
 * Stage 0 只提供資料結構；計分與走訪由 Stage 2 實作。
 */
public class WebNode {
    private final WebPage webPage;
    private final List<WebNode> children = new ArrayList<>();
    private WebNode parent;
    /** 這個節點自己網頁的計分結果（分數與各關鍵字次數）。 */
    private PageResult pageResult;
    /** 自己的分數加上所有子節點的 nodeScore（HW6 的定義）。 */
    private double nodeScore;
    /** 節點深度，根節點為 0。 */
    private int depth = 0;
    public WebNode(WebPage webPage) {
        this.webPage = webPage;
    }

    public void addChild(WebNode child) {
        children.add(child);
        child.parent = this;
    }

    public WebPage getWebPage() {
        return webPage;
    }

    public List<WebNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public WebNode getParent() {
        return parent;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /** 根節點深度為 0（HW6 的 getDepth 從 1 開始，這裡改成 0，方便計算 0.5^深度）。 */
    public int getDepth() {
        for (WebNode node = parent; node != null; node = node.parent) {
            this.depth++;
        }
        return this.depth;
    }

    public PageResult getPageResult() {
        return pageResult;
    }

    public void setPageResult(PageResult pageResult) {
        this.pageResult = pageResult;
    }

    public double getNodeScore() {
        return nodeScore;
    }

    public void setNodeScore(double nodeScore) {
        this.nodeScore = nodeScore;
    }
    public void setDepth(int depth) {
        this.depth = depth;
    }
}
