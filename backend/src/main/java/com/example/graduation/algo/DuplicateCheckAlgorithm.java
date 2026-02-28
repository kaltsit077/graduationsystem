package com.example.graduation.algo;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 选题去重检测算法
 * 使用 Jaccard 相似度 + 余弦相似度融合
 */
@Component
public class DuplicateCheckAlgorithm {
    
    private static final double SIMILARITY_THRESHOLD = 0.7; // 相似度阈值
    
    /**
     * 计算两个文本的融合相似度
     * @param text1 文本1（标题+描述）
     * @param text2 文本2（标题+描述）
     * @return 相似度值（0-1之间）
     */
    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> words1 = tokenize(text1);
        Set<String> words2 = tokenize(text2);
        
        // Jaccard相似度
        double jaccard = calculateJaccard(words1, words2);
        
        // 余弦相似度
        double cosine = calculateCosine(words1, words2);
        
        // 融合相似度（权重：Jaccard 0.4，Cosine 0.6）
        double similarity = 0.4 * jaccard + 0.6 * cosine;
        
        return similarity;
    }
    
    /**
     * Jaccard相似度计算
     */
    private double calculateJaccard(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 余弦相似度计算
     */
    private double calculateCosine(Set<String> set1, Set<String> set2) {
        // 构建词向量
        Set<String> allWords = new HashSet<>(set1);
        allWords.addAll(set2);
        
        List<String> wordList = new ArrayList<>(allWords);
        Map<String, Integer> vector1 = buildVector(set1, wordList);
        Map<String, Integer> vector2 = buildVector(set2, wordList);
        
        // 计算点积
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String word : wordList) {
            int v1 = vector1.getOrDefault(word, 0);
            int v2 = vector2.getOrDefault(word, 0);
            
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 构建词频向量
     */
    private Map<String, Integer> buildVector(Set<String> words, List<String> wordList) {
        Map<String, Integer> vector = new HashMap<>();
        for (String word : wordList) {
            vector.put(word, words.contains(word) ? 1 : 0);
        }
        return vector;
    }
    
    /**
     * 文本分词（简化版）
     * 实际应该使用jieba等分词工具
     */
    private Set<String> tokenize(String text) {
        // 简单分词：按空格、标点分割
        String[] tokens = text.toLowerCase()
                .replaceAll("[\\p{Punct}]", " ")
                .split("\\s+");
        
        return Arrays.stream(tokens)
                .filter(token -> token.length() >= 2) // 过滤短词
                .collect(Collectors.toSet());
    }
    
    /**
     * 检查是否通过去重检测
     */
    public boolean isPassed(double similarity) {
        return similarity < SIMILARITY_THRESHOLD;
    }
}

