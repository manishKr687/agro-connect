package com.agroconnect.service;

import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes Hinglish / Hindi-romanized crop names to their canonical English
 * equivalents using an exact-match dictionary with fuzzy fallback.
 */
@Slf4j
@Service
public class CropNormalizerClient {

    private static final int FUZZY_THRESHOLD = 80;

    private static final Map<String, String> CROP_DICTIONARY = new HashMap<>();
    private static final List<String> DICT_KEYS;

    static {
        // Tomato
        CROP_DICTIONARY.put("tamatar", "Tomato"); CROP_DICTIONARY.put("tamater", "Tomato");
        CROP_DICTIONARY.put("tamato", "Tomato");  CROP_DICTIONARY.put("tomato", "Tomato");
        // Potato
        CROP_DICTIONARY.put("aalu", "Potato"); CROP_DICTIONARY.put("aloo", "Potato");
        CROP_DICTIONARY.put("alu", "Potato");  CROP_DICTIONARY.put("potato", "Potato");
        // Onion
        CROP_DICTIONARY.put("pyaz", "Onion");  CROP_DICTIONARY.put("piyaz", "Onion");
        CROP_DICTIONARY.put("pyaaz", "Onion"); CROP_DICTIONARY.put("kanda", "Onion");
        CROP_DICTIONARY.put("onion", "Onion");
        // Garlic
        CROP_DICTIONARY.put("lehsun", "Garlic"); CROP_DICTIONARY.put("lahasun", "Garlic");
        CROP_DICTIONARY.put("lasan", "Garlic");  CROP_DICTIONARY.put("garlic", "Garlic");
        // Ginger
        CROP_DICTIONARY.put("adrak", "Ginger"); CROP_DICTIONARY.put("adrakh", "Ginger");
        CROP_DICTIONARY.put("ginger", "Ginger");
        // Wheat
        CROP_DICTIONARY.put("gehun", "Wheat"); CROP_DICTIONARY.put("gehu", "Wheat");
        CROP_DICTIONARY.put("gahu", "Wheat");  CROP_DICTIONARY.put("wheat", "Wheat");
        // Rice / Paddy
        CROP_DICTIONARY.put("chawal", "Rice"); CROP_DICTIONARY.put("chaawal", "Rice");
        CROP_DICTIONARY.put("dhan", "Rice");   CROP_DICTIONARY.put("rice", "Rice");
        CROP_DICTIONARY.put("paddy", "Rice");
        // Maize / Corn
        CROP_DICTIONARY.put("makka", "Maize"); CROP_DICTIONARY.put("makki", "Maize");
        CROP_DICTIONARY.put("bhutta", "Maize"); CROP_DICTIONARY.put("maize", "Maize");
        CROP_DICTIONARY.put("corn", "Maize");
        // Mustard
        CROP_DICTIONARY.put("sarson", "Mustard"); CROP_DICTIONARY.put("sarsoon", "Mustard");
        CROP_DICTIONARY.put("mustard", "Mustard");
        // Sugarcane
        CROP_DICTIONARY.put("ganna", "Sugarcane"); CROP_DICTIONARY.put("ikh", "Sugarcane");
        CROP_DICTIONARY.put("sugarcane", "Sugarcane");
        // Cotton
        CROP_DICTIONARY.put("kapas", "Cotton"); CROP_DICTIONARY.put("rui", "Cotton");
        CROP_DICTIONARY.put("cotton", "Cotton");
        // Soybean
        CROP_DICTIONARY.put("soya", "Soybean"); CROP_DICTIONARY.put("soyabean", "Soybean");
        CROP_DICTIONARY.put("soybean", "Soybean");
        // Groundnut / Peanut
        CROP_DICTIONARY.put("moongfali", "Groundnut"); CROP_DICTIONARY.put("mungfali", "Groundnut");
        CROP_DICTIONARY.put("groundnut", "Groundnut"); CROP_DICTIONARY.put("peanut", "Groundnut");
        // Chickpea / Gram
        CROP_DICTIONARY.put("chana", "Chickpea"); CROP_DICTIONARY.put("chanaa", "Chickpea");
        CROP_DICTIONARY.put("chole", "Chickpea"); CROP_DICTIONARY.put("gram", "Chickpea");
        CROP_DICTIONARY.put("chickpea", "Chickpea");
        // Lentil
        CROP_DICTIONARY.put("masoor", "Lentil"); CROP_DICTIONARY.put("masur", "Lentil");
        CROP_DICTIONARY.put("lentil", "Lentil");
        // Green Gram / Moong
        CROP_DICTIONARY.put("moong", "Green Gram"); CROP_DICTIONARY.put("mung", "Green Gram");
        CROP_DICTIONARY.put("green gram", "Green Gram");
        // Black Gram / Urad
        CROP_DICTIONARY.put("urad", "Black Gram"); CROP_DICTIONARY.put("udad", "Black Gram");
        CROP_DICTIONARY.put("black gram", "Black Gram");
        // Pigeon Pea / Tur
        CROP_DICTIONARY.put("arhar", "Pigeon Pea"); CROP_DICTIONARY.put("tur", "Pigeon Pea");
        CROP_DICTIONARY.put("toor", "Pigeon Pea"); CROP_DICTIONARY.put("pigeon pea", "Pigeon Pea");
        // Carrot
        CROP_DICTIONARY.put("gajar", "Carrot"); CROP_DICTIONARY.put("carrot", "Carrot");
        // Radish
        CROP_DICTIONARY.put("mooli", "Radish"); CROP_DICTIONARY.put("muli", "Radish");
        CROP_DICTIONARY.put("radish", "Radish");
        // Spinach
        CROP_DICTIONARY.put("palak", "Spinach"); CROP_DICTIONARY.put("spinach", "Spinach");
        // Fenugreek
        CROP_DICTIONARY.put("methi", "Fenugreek"); CROP_DICTIONARY.put("fenugreek", "Fenugreek");
        // Coriander
        CROP_DICTIONARY.put("dhaniya", "Coriander"); CROP_DICTIONARY.put("dhania", "Coriander");
        CROP_DICTIONARY.put("coriander", "Coriander");
        // Chilli
        CROP_DICTIONARY.put("mirch", "Chilli");    CROP_DICTIONARY.put("mirchi", "Chilli");
        CROP_DICTIONARY.put("lalmirch", "Chilli"); CROP_DICTIONARY.put("chilli", "Chilli");
        CROP_DICTIONARY.put("chili", "Chilli");    CROP_DICTIONARY.put("pepper", "Chilli");
        // Brinjal / Eggplant
        CROP_DICTIONARY.put("baingan", "Brinjal"); CROP_DICTIONARY.put("baigan", "Brinjal");
        CROP_DICTIONARY.put("brinjal", "Brinjal"); CROP_DICTIONARY.put("eggplant", "Brinjal");
        // Okra
        CROP_DICTIONARY.put("bhindi", "Okra"); CROP_DICTIONARY.put("ladyfinger", "Okra");
        CROP_DICTIONARY.put("okra", "Okra");
        // Pumpkin
        CROP_DICTIONARY.put("kaddu", "Pumpkin"); CROP_DICTIONARY.put("pumpkin", "Pumpkin");
        // Bottle Gourd
        CROP_DICTIONARY.put("lauki", "Bottle Gourd"); CROP_DICTIONARY.put("dudhi", "Bottle Gourd");
        CROP_DICTIONARY.put("bottle gourd", "Bottle Gourd");
        // Bitter Gourd
        CROP_DICTIONARY.put("karela", "Bitter Gourd"); CROP_DICTIONARY.put("bitter gourd", "Bitter Gourd");
        // Cucumber
        CROP_DICTIONARY.put("kheera", "Cucumber"); CROP_DICTIONARY.put("khira", "Cucumber");
        CROP_DICTIONARY.put("cucumber", "Cucumber");
        // Cauliflower
        CROP_DICTIONARY.put("gobhi", "Cauliflower");      CROP_DICTIONARY.put("phulgobi", "Cauliflower");
        CROP_DICTIONARY.put("phool gobhi", "Cauliflower"); CROP_DICTIONARY.put("cauliflower", "Cauliflower");
        // Cabbage
        CROP_DICTIONARY.put("bandgobi", "Cabbage"); CROP_DICTIONARY.put("pattagonbi", "Cabbage");
        CROP_DICTIONARY.put("cabbage", "Cabbage");
        // Peas
        CROP_DICTIONARY.put("matar", "Peas"); CROP_DICTIONARY.put("mattar", "Peas");
        CROP_DICTIONARY.put("peas", "Peas");
        // Banana
        CROP_DICTIONARY.put("kela", "Banana"); CROP_DICTIONARY.put("banana", "Banana");
        // Mango
        CROP_DICTIONARY.put("aam", "Mango"); CROP_DICTIONARY.put("mango", "Mango");
        // Guava
        CROP_DICTIONARY.put("amrood", "Guava"); CROP_DICTIONARY.put("guava", "Guava");
        // Papaya
        CROP_DICTIONARY.put("papita", "Papaya"); CROP_DICTIONARY.put("papaya", "Papaya");
        // Watermelon
        CROP_DICTIONARY.put("tarbooz", "Watermelon"); CROP_DICTIONARY.put("watermelon", "Watermelon");
        // Grapes
        CROP_DICTIONARY.put("angoor", "Grapes"); CROP_DICTIONARY.put("grapes", "Grapes");
        // Pomegranate
        CROP_DICTIONARY.put("anar", "Pomegranate"); CROP_DICTIONARY.put("pomegranate", "Pomegranate");
        // Lemon
        CROP_DICTIONARY.put("nimbu", "Lemon"); CROP_DICTIONARY.put("nimbo", "Lemon");
        CROP_DICTIONARY.put("lemon", "Lemon");
        // Turmeric
        CROP_DICTIONARY.put("haldi", "Turmeric"); CROP_DICTIONARY.put("turmeric", "Turmeric");
        // Cumin
        CROP_DICTIONARY.put("jeera", "Cumin"); CROP_DICTIONARY.put("jira", "Cumin");
        CROP_DICTIONARY.put("cumin", "Cumin");
        // Sesame
        CROP_DICTIONARY.put("til", "Sesame"); CROP_DICTIONARY.put("sesame", "Sesame");
        // Sorghum / Jowar
        CROP_DICTIONARY.put("jowar", "Sorghum"); CROP_DICTIONARY.put("sorghum", "Sorghum");
        // Pearl Millet / Bajra
        CROP_DICTIONARY.put("bajra", "Pearl Millet"); CROP_DICTIONARY.put("pearl millet", "Pearl Millet");
        // Finger Millet / Ragi
        CROP_DICTIONARY.put("ragi", "Finger Millet"); CROP_DICTIONARY.put("nachni", "Finger Millet");
        CROP_DICTIONARY.put("finger millet", "Finger Millet");
        // Barley
        CROP_DICTIONARY.put("jau", "Barley"); CROP_DICTIONARY.put("jow", "Barley");
        CROP_DICTIONARY.put("barley", "Barley");
        // Sunflower
        CROP_DICTIONARY.put("surajmukhi", "Sunflower"); CROP_DICTIONARY.put("sunflower", "Sunflower");
        // Castor
        CROP_DICTIONARY.put("arandi", "Castor"); CROP_DICTIONARY.put("castor", "Castor");
        // Jute
        CROP_DICTIONARY.put("jute", "Jute"); CROP_DICTIONARY.put("paat", "Jute");

        DICT_KEYS = new ArrayList<>(CROP_DICTIONARY.keySet());
    }

    /**
     * Returns the normalized (English) version of the given crop name.
     */
    public String normalize(String cropName) {
        if (cropName == null || cropName.isBlank()) return cropName;

        String key = cropName.trim().toLowerCase();

        // 1. Exact match
        if (CROP_DICTIONARY.containsKey(key)) {
            String english = CROP_DICTIONARY.get(key);
            if (!english.equals(cropName)) {
                log.debug("Crop name normalized: '{}' -> '{}'", cropName, english);
            }
            return english;
        }

        // 2. Fuzzy match
        ExtractedResult result = FuzzySearch.extractOne(key, DICT_KEYS, FuzzySearch::weightedRatio);
        if (result != null && result.getScore() >= FUZZY_THRESHOLD) {
            String english = CROP_DICTIONARY.get(result.getString());
            log.debug("Crop name fuzzy-normalized: '{}' -> '{}' (score={})", cropName, english, result.getScore());
            return english;
        }

        // 3. Unrecognized — return as-is
        return cropName;
    }

    /**
     * Returns normalized name plus metadata (corrected, valid) for the frontend.
     */
    public Map<String, Object> normalizeWithMeta(String cropName) {
        Map<String, Object> result = new HashMap<>();

        if (cropName == null || cropName.isBlank()) {
            result.put("normalized", cropName);
            result.put("corrected", false);
            result.put("valid", true);
            return result;
        }

        String key = cropName.trim().toLowerCase();

        // 1. Exact match
        if (CROP_DICTIONARY.containsKey(key)) {
            String english = CROP_DICTIONARY.get(key);
            result.put("normalized", english);
            result.put("corrected", !english.equals(cropName.trim()));
            result.put("valid", true);
            return result;
        }

        // 2. Fuzzy match
        ExtractedResult fuzzy = FuzzySearch.extractOne(key, DICT_KEYS, FuzzySearch::weightedRatio);
        if (fuzzy != null && fuzzy.getScore() >= FUZZY_THRESHOLD) {
            String english = CROP_DICTIONARY.get(fuzzy.getString());
            result.put("normalized", english);
            result.put("corrected", true);
            result.put("valid", true);
            return result;
        }

        // 3. Unrecognized
        result.put("normalized", cropName);
        result.put("corrected", false);
        result.put("valid", false);
        return result;
    }
}
