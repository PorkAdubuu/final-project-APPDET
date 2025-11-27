package com.example.trackback;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CosineSimilarityMatcher {

    private static final String TAG = "CosineSimilarity";

    // Common words to ignore (stopwords)
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "through", "during",
            "is", "was", "are", "were", "been", "be", "have", "has", "had", "do",
            "does", "did", "will", "would", "could", "should", "may", "might",
            "my", "your", "his", "her", "its", "our", "their", "this", "that",
            "these", "those", "i", "you", "he", "she", "it", "we", "they"
    ));

    public static double calculateSimilarity(Item item1, Item item2) {
        // Create feature vectors for both items
        Map<String, Double> vector1 = createFeatureVector(item1);
        Map<String, Double> vector2 = createFeatureVector(item2);

        Log.d(TAG, "Vector 1 features: " + vector1.size() + " - " + vector1.keySet());
        Log.d(TAG, "Vector 2 features: " + vector2.size() + " - " + vector2.keySet());

        // Calculate cosine similarity
        double similarity = cosineSimilarity(vector1, vector2);
        Log.d(TAG, "Similarity between '" + item1.getTitle() + "' and '" + item2.getTitle() + "': " + similarity);

        return similarity;
    }

    /**
     * Create a feature vector from an item with enhanced weighting
     */
    private static Map<String, Double> createFeatureVector(Item item) {
        Map<String, Double> vector = new HashMap<>();

        Log.d(TAG, "Creating vector for: " + item.getTitle());

        // 1. TITLE features (very high weight - most important)
        if (item.getTitle() != null && !item.getTitle().isEmpty()) {
            String title = item.getTitle().toLowerCase();
            Log.d(TAG, "  Title: " + title);

            String[] titleWords = title.split("\\s+");
            for (String word : titleWords) {
                word = word.replaceAll("[^a-z0-9]", ""); // Remove punctuation
                if (word.length() > 2 && !STOPWORDS.contains(word)) {
                    // Title words get HIGH weight (6.0)
                    vector.put("title_" + word, vector.getOrDefault("title_" + word, 0.0) + 6.0);
                    Log.d(TAG, "    Title word: " + word + " (weight: 6.0)");
                }
            }
        }

        // 2. CATEGORY feature (high weight)
        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
            String category = item.getCategory().toLowerCase();
            vector.put("category_" + category, 5.0);
            Log.d(TAG, "  Category: " + category + " (weight: 5.0)");
        } else {
            Log.d(TAG, "  Category: NULL");
        }

        // 3. DESCRIPTION/ADDITIONAL INFO features (high weight)
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            String description = item.getDescription().toLowerCase();
            Log.d(TAG, "  Description: " + description);

            String[] descWords = description.split("\\s+");
            for (String word : descWords) {
                word = word.replaceAll("[^a-z0-9]", ""); // Remove punctuation
                if (word.length() > 2 && !STOPWORDS.contains(word)) {
                    // Description words get HIGH weight (4.0)
                    vector.put("desc_" + word, vector.getOrDefault("desc_" + word, 0.0) + 4.0);
                }
            }
            Log.d(TAG, "    Description words extracted: " + descWords.length);
        } else {
            Log.d(TAG, "  Description: NULL");
        }

        // 4. COLOR feature (medium-high weight) - Only if available
        if (item.getColor() != null && !item.getColor().isEmpty()) {
            String color = item.getColor().toLowerCase();
            vector.put("color_" + color, 3.5);
            Log.d(TAG, "  Color: " + color + " (weight: 3.5)");
        } else {
            Log.d(TAG, "  Color: NULL (not penalizing)");
        }

        // 5. LOCATION feature (medium weight)
        if (item.getLocation() != null && !item.getLocation().isEmpty()) {
            String location = item.getLocation().toLowerCase();
            vector.put("location_" + location, 3.0);
            Log.d(TAG, "  Location: " + location + " (weight: 3.0)");

            // Also add location words separately (helps with partial matches like "Library 2nd floor" vs "Library")
            String[] locWords = location.split("\\s+");
            for (String word : locWords) {
                word = word.replaceAll("[^a-z0-9]", "");
                if (word.length() > 2 && !STOPWORDS.contains(word)) {
                    vector.put("loc_word_" + word, 2.0);
                }
            }
        } else {
            Log.d(TAG, "  Location: NULL");
        }

        Log.d(TAG, "  Total vector size: " + vector.size());

        return vector;
    }


    private static double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        // Get all unique keys
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(vec1.keySet());
        allKeys.addAll(vec2.keySet());

        Log.d(TAG, "Total unique features to compare: " + allKeys.size());

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        int matchCount = 0;
        for (String key : allKeys) {
            double val1 = vec1.getOrDefault(key, 0.0);
            double val2 = vec2.getOrDefault(key, 0.0);

            if (val1 > 0 && val2 > 0) {
                matchCount++;
                Log.d(TAG, "  Match on feature: " + key + " (val1=" + val1 + ", val2=" + val2 + ")");
            }

            dotProduct += val1 * val2;
            magnitude1 += val1 * val1;
            magnitude2 += val2 * val2;
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        Log.d(TAG, "  Features matched: " + matchCount + "/" + allKeys.size());
        Log.d(TAG, "  Dot product: " + dotProduct);
        Log.d(TAG, "  Magnitude1: " + magnitude1);
        Log.d(TAG, "  Magnitude2: " + magnitude2);

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            Log.w(TAG, "  WARNING: Zero magnitude detected! No features to compare.");
            return 0.0;
        }

        double similarity = dotProduct / (magnitude1 * magnitude2);
        Log.d(TAG, "  Final similarity: " + similarity);

        return similarity;
    }


    public static List<ItemMatch> findMatches(Item lostItem, List<Item> foundItems, double threshold) {
        List<ItemMatch> matches = new ArrayList<>();

        Log.d(TAG, "========================================");
        Log.d(TAG, "Finding matches for LOST item: " + lostItem.getTitle());
        Log.d(TAG, "Comparing against " + foundItems.size() + " FOUND items");
        Log.d(TAG, "Threshold: " + threshold);
        Log.d(TAG, "========================================");

        for (Item foundItem : foundItems) {
            Log.d(TAG, "\n--- Comparing with FOUND item: " + foundItem.getTitle() + " ---");

            double similarity = calculateSimilarity(lostItem, foundItem);

            Log.d(TAG, "Similarity score: " + similarity + " (threshold: " + threshold + ")");

            if (similarity >= threshold) {
                String reason = generateMatchReason(lostItem, foundItem, similarity);
                matches.add(new ItemMatch(lostItem, foundItem, similarity, reason));
                Log.d(TAG, "✓ MATCH FOUND! Score: " + similarity + ", Reason: " + reason);
            } else {
                Log.d(TAG, "✗ Below threshold");
            }
        }

        Log.d(TAG, "\n========================================");
        Log.d(TAG, "TOTAL MATCHES FOUND: " + matches.size());
        Log.d(TAG, "========================================\n");

        // Sort by similarity score (highest first)
        matches.sort((m1, m2) -> Double.compare(m2.getSimilarityScore(), m1.getSimilarityScore()));

        return matches;
    }


    public static String generateMatchReason(Item lost, Item found, double similarity) {
        List<String> reasons = new ArrayList<>();

        // Check category
        if (lost.getCategory() != null && found.getCategory() != null &&
                lost.getCategory().equalsIgnoreCase(found.getCategory())) {
            reasons.add("Same category: " + lost.getCategory());
        }

        // Check color
        if (lost.getColor() != null && found.getColor() != null &&
                lost.getColor().equalsIgnoreCase(found.getColor())) {
            reasons.add("Same color: " + lost.getColor());
        }

        // Check location
        if (lost.getLocation() != null && found.getLocation() != null &&
                lost.getLocation().equalsIgnoreCase(found.getLocation())) {
            reasons.add("Same location: " + lost.getLocation());
        }

        // Check for common words in titles (most important)
        if (lost.getTitle() != null && found.getTitle() != null) {
            Set<String> lostTitleWords = extractSignificantWords(lost.getTitle());
            Set<String> foundTitleWords = extractSignificantWords(found.getTitle());

            Set<String> commonTitleWords = new HashSet<>(lostTitleWords);
            commonTitleWords.retainAll(foundTitleWords);

            if (!commonTitleWords.isEmpty()) {
                reasons.add("Matching items: " + String.join(", ", commonTitleWords));
            }
        }

        // Check for common words in descriptions
        if (lost.getDescription() != null && found.getDescription() != null) {
            Set<String> lostDescWords = extractSignificantWords(lost.getDescription());
            Set<String> foundDescWords = extractSignificantWords(found.getDescription());

            Set<String> commonDescWords = new HashSet<>(lostDescWords);
            commonDescWords.retainAll(foundDescWords);

            if (commonDescWords.size() >= 2) {
                reasons.add("Similar details: " + String.join(", ",
                        new ArrayList<>(commonDescWords).subList(0, Math.min(3, commonDescWords.size()))));
            } else if (commonDescWords.size() == 1) {
                reasons.add("Common detail: " + commonDescWords.iterator().next());
            }
        }


        String matchQuality;
        if (similarity >= 0.7) {
            matchQuality = "Excellent match";
        } else if (similarity >= 0.5) {
            matchQuality = "Strong match";
        } else if (similarity >= 0.4) {
            matchQuality = "Good match";
        } else {
            matchQuality = "Possible match";
        }

        if (reasons.isEmpty()) {
            return matchQuality + " - Similar overall characteristics";
        } else {
            return matchQuality + " - " + String.join(", ", reasons);
        }
    }


    private static Set<String> extractSignificantWords(String text) {
        Set<String> words = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return words;
        }

        String[] tokens = text.toLowerCase().split("\\s+");
        for (String word : tokens) {
            word = word.replaceAll("[^a-z0-9]", "");
            if (word.length() > 2 && !STOPWORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }
}