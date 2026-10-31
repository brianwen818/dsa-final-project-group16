package com.group16.ooosearch.tree;

import com.group16.ooosearch.model.WebPage;
import java.util.Map;

public class WebTree {
    private final WebNode root;

    public WebTree(WebPage rootPage) {
        this.root = new WebNode(rootPage);
    }

    public WebNode getRoot() {
        return root;
    }

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

    public double calculateTotalScores(double decayFactor) {
        return postOrderScore(root, decayFactor);
    }

    private double postOrderScore(WebNode node, double decayFactor) {
        if (node == null) return 0.0;

        double childrenScoreSum = 0.0;
        for (WebNode child : node.getChildren()) {
            childrenScoreSum += postOrderScore(child, decayFactor);
        }

        double selfPageScore = (node.getPageResult() != null) ? node.getPageResult().getScore() : 0.0;
        double decayedSelfScore = selfPageScore * Math.pow(decayFactor, node.getDepth());
        double totalNodeScore = decayedSelfScore + childrenScoreSum;
        
        node.setNodeScore(totalNodeScore);
        return totalNodeScore;
    }

    /** 印出樹狀結構、分數以及各節點的關鍵字次數 */
    public void printTree() {
    printNodeRecursive(root, 0);
}

private void printNodeRecursive(WebNode node, int indent) {
    if (node == null || node.getWebPage() == null) return;

    String spaces = "    ".repeat(indent);
    
    // 取得顯示名稱
    String displayName = node.getWebPage().getTitle();
    if (displayName == null || displayName.trim().isEmpty()) {
        displayName = node.getWebPage().getUrl();
    }

    // 印出樹狀結構與名稱
    System.out.print(spaces);
    if (indent > 0) {
        System.out.print("├── ");
    }
    System.out.print(displayName);

    // 取得分數（單頁分與累積總分）
    double pageScore = (node.getPageResult() != null) ? node.getPageResult().getScore() : 0.0;
    System.out.print(String.format(" [單頁分: %.1f, 累積總分: %.1f]", pageScore, node.getNodeScore()));

    // 印出關鍵字次數
    if (node.getPageResult() != null && node.getPageResult().getKeywordCounts() != null) {
        Map<String, Integer> counts = node.getPageResult().getKeywordCounts();
        java.util.List<String> formattedCounts = new java.util.ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                formattedCounts.add(entry.getKey() + ":" + entry.getValue());
            }
        }
        
        if (!formattedCounts.isEmpty()) {
            System.out.print(" | 關鍵字次數: " + String.join(", ", formattedCounts));
        }
    }
    System.out.println();

    // 遞迴印出子節點
    for (WebNode child : node.getChildren()) {
        printNodeRecursive(child, indent + 1);
    }
}
}