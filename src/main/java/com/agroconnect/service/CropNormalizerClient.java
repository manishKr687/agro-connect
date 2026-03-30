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
        // ── Devanagari (Hindi script) entries ──────────────────────────────
        CROP_DICTIONARY.put("टमाटर", "Tomato");
        CROP_DICTIONARY.put("आलू", "Potato");      CROP_DICTIONARY.put("आलु", "Potato");
        CROP_DICTIONARY.put("प्याज", "Onion");     CROP_DICTIONARY.put("प्याज़", "Onion");
        CROP_DICTIONARY.put("लहसुन", "Garlic");
        CROP_DICTIONARY.put("अदरक", "Ginger");
        CROP_DICTIONARY.put("गेहूं", "Wheat");     CROP_DICTIONARY.put("गेहूँ", "Wheat");
        CROP_DICTIONARY.put("चावल", "Rice");       CROP_DICTIONARY.put("धान", "Rice");
        CROP_DICTIONARY.put("मक्का", "Maize");     CROP_DICTIONARY.put("मकई", "Maize");
        CROP_DICTIONARY.put("सरसों", "Mustard");   CROP_DICTIONARY.put("सरसो", "Mustard");
        CROP_DICTIONARY.put("गन्ना", "Sugarcane"); CROP_DICTIONARY.put("ईख", "Sugarcane");
        CROP_DICTIONARY.put("कपास", "Cotton");
        CROP_DICTIONARY.put("सोयाबीन", "Soybean"); CROP_DICTIONARY.put("सोया", "Soybean");
        CROP_DICTIONARY.put("मूंगफली", "Groundnut");
        CROP_DICTIONARY.put("चना", "Chickpea");    CROP_DICTIONARY.put("छोले", "Chickpea");
        CROP_DICTIONARY.put("मसूर", "Lentil");     CROP_DICTIONARY.put("मसूरी", "Lentil");
        CROP_DICTIONARY.put("मूंग", "Green Gram");
        CROP_DICTIONARY.put("उड़द", "Black Gram");  CROP_DICTIONARY.put("उरद", "Black Gram");
        CROP_DICTIONARY.put("अरहर", "Pigeon Pea"); CROP_DICTIONARY.put("तूर", "Pigeon Pea");
        CROP_DICTIONARY.put("गाजर", "Carrot");
        CROP_DICTIONARY.put("मूली", "Radish");
        CROP_DICTIONARY.put("पालक", "Spinach");
        CROP_DICTIONARY.put("मेथी", "Fenugreek");
        CROP_DICTIONARY.put("धनिया", "Coriander");
        CROP_DICTIONARY.put("मिर्च", "Chilli");    CROP_DICTIONARY.put("मिर्ची", "Chilli");
        CROP_DICTIONARY.put("बैंगन", "Brinjal");
        CROP_DICTIONARY.put("भिंडी", "Okra");
        CROP_DICTIONARY.put("कद्दू", "Pumpkin");
        CROP_DICTIONARY.put("लौकी", "Bottle Gourd"); CROP_DICTIONARY.put("दूधी", "Bottle Gourd");
        CROP_DICTIONARY.put("करेला", "Bitter Gourd");
        CROP_DICTIONARY.put("खीरा", "Cucumber");   CROP_DICTIONARY.put("ककड़ी", "Cucumber");
        CROP_DICTIONARY.put("गोभी", "Cauliflower"); CROP_DICTIONARY.put("फूल गोभी", "Cauliflower");
        CROP_DICTIONARY.put("बंद गोभी", "Cabbage"); CROP_DICTIONARY.put("पत्ता गोभी", "Cabbage");
        CROP_DICTIONARY.put("मटर", "Peas");
        CROP_DICTIONARY.put("तोरई", "Ridge Gourd"); CROP_DICTIONARY.put("तुरई", "Ridge Gourd");
        CROP_DICTIONARY.put("सहजन", "Drumstick");
        CROP_DICTIONARY.put("शकरकंद", "Sweet Potato");
        CROP_DICTIONARY.put("अरबी", "Taro");        CROP_DICTIONARY.put("अरवी", "Taro");
        CROP_DICTIONARY.put("शलजम", "Turnip");
        CROP_DICTIONARY.put("चुकंदर", "Beetroot");
        CROP_DICTIONARY.put("फ्रेंच बीन्स", "French Beans"); CROP_DICTIONARY.put("सेम", "French Beans");
        CROP_DICTIONARY.put("लोबिया", "Cowpea");    CROP_DICTIONARY.put("बोड़ी", "Cowpea");
        CROP_DICTIONARY.put("ग्वार", "Cluster Beans");
        CROP_DICTIONARY.put("केला", "Banana");
        CROP_DICTIONARY.put("आम", "Mango");
        CROP_DICTIONARY.put("अमरूद", "Guava");
        CROP_DICTIONARY.put("पपीता", "Papaya");
        CROP_DICTIONARY.put("तरबूज", "Watermelon");
        CROP_DICTIONARY.put("अंगूर", "Grapes");
        CROP_DICTIONARY.put("अनार", "Pomegranate");
        CROP_DICTIONARY.put("नींबू", "Lemon");      CROP_DICTIONARY.put("नीम्बू", "Lemon");
        CROP_DICTIONARY.put("हल्दी", "Turmeric");
        CROP_DICTIONARY.put("जीरा", "Cumin");
        CROP_DICTIONARY.put("तिल", "Sesame");
        CROP_DICTIONARY.put("ज्वार", "Sorghum");
        CROP_DICTIONARY.put("बाजरा", "Pearl Millet");
        CROP_DICTIONARY.put("रागी", "Finger Millet");
        CROP_DICTIONARY.put("जौ", "Barley");
        CROP_DICTIONARY.put("सूरजमुखी", "Sunflower");
        CROP_DICTIONARY.put("अरंडी", "Castor");
        CROP_DICTIONARY.put("जूट", "Jute");         CROP_DICTIONARY.put("पाट", "Jute");
        // ── Romanized Hinglish entries ──────────────────────────────────────
        // Tomato
        CROP_DICTIONARY.put("tamatar", "Tomato"); CROP_DICTIONARY.put("tamater", "Tomato");
        CROP_DICTIONARY.put("tamato", "Tomato");  CROP_DICTIONARY.put("tomato", "Tomato");
        // Potato
        CROP_DICTIONARY.put("aalu", "Potato"); CROP_DICTIONARY.put("aloo", "Potato");
        CROP_DICTIONARY.put("alu", "Potato");  CROP_DICTIONARY.put("potato", "Potato");
        // Onion
        CROP_DICTIONARY.put("pyaz", "Onion");  CROP_DICTIONARY.put("piyaz", "Onion");
        CROP_DICTIONARY.put("pyaj", "Onion");
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
        CROP_DICTIONARY.put("gajar", "Carrot");  CROP_DICTIONARY.put("gaajar", "Carrot");
        CROP_DICTIONARY.put("carrot", "Carrot");
        // Radish
        CROP_DICTIONARY.put("mooli", "Radish");  CROP_DICTIONARY.put("muli", "Radish");
        CROP_DICTIONARY.put("moori", "Radish");  CROP_DICTIONARY.put("radish", "Radish");
        // Spinach
        CROP_DICTIONARY.put("palak", "Spinach"); CROP_DICTIONARY.put("paalak", "Spinach");
        CROP_DICTIONARY.put("saag", "Spinach");  CROP_DICTIONARY.put("spinach", "Spinach");
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
        CROP_DICTIONARY.put("baingan", "Brinjal");  CROP_DICTIONARY.put("baigan", "Brinjal");
        CROP_DICTIONARY.put("begun", "Brinjal");    CROP_DICTIONARY.put("vangi", "Brinjal");
        CROP_DICTIONARY.put("ringna", "Brinjal");   CROP_DICTIONARY.put("ringana", "Brinjal");
        CROP_DICTIONARY.put("brinjal", "Brinjal");  CROP_DICTIONARY.put("eggplant", "Brinjal");
        // Okra
        CROP_DICTIONARY.put("bhindi", "Okra");      CROP_DICTIONARY.put("bhende", "Okra");
        CROP_DICTIONARY.put("lady finger", "Okra"); CROP_DICTIONARY.put("ladyfinger", "Okra");
        CROP_DICTIONARY.put("okra", "Okra");
        // Pumpkin
        CROP_DICTIONARY.put("kaddu", "Pumpkin");    CROP_DICTIONARY.put("kaddoo", "Pumpkin");
        CROP_DICTIONARY.put("sitaphal", "Pumpkin"); CROP_DICTIONARY.put("pumpkin", "Pumpkin");
        // Bottle Gourd
        CROP_DICTIONARY.put("lauki", "Bottle Gourd");  CROP_DICTIONARY.put("loki", "Bottle Gourd");
        CROP_DICTIONARY.put("louki", "Bottle Gourd");  CROP_DICTIONARY.put("ghia", "Bottle Gourd");
        CROP_DICTIONARY.put("ghiya", "Bottle Gourd");  CROP_DICTIONARY.put("dudhi", "Bottle Gourd");
        CROP_DICTIONARY.put("doodhi", "Bottle Gourd"); CROP_DICTIONARY.put("bottle gourd", "Bottle Gourd");
        // Bitter Gourd
        CROP_DICTIONARY.put("karela", "Bitter Gourd");  CROP_DICTIONARY.put("karella", "Bitter Gourd");
        CROP_DICTIONARY.put("kerela", "Bitter Gourd");  CROP_DICTIONARY.put("bitter gourd", "Bitter Gourd");
        // Cucumber
        CROP_DICTIONARY.put("kheera", "Cucumber"); CROP_DICTIONARY.put("khira", "Cucumber");
        CROP_DICTIONARY.put("kakdi", "Cucumber");  CROP_DICTIONARY.put("kakri", "Cucumber");
        CROP_DICTIONARY.put("kakadi", "Cucumber"); CROP_DICTIONARY.put("cucumber", "Cucumber");
        // Cauliflower
        CROP_DICTIONARY.put("gobhi", "Cauliflower");       CROP_DICTIONARY.put("phulgobi", "Cauliflower");
        CROP_DICTIONARY.put("phool gobhi", "Cauliflower"); CROP_DICTIONARY.put("phool gobi", "Cauliflower");
        CROP_DICTIONARY.put("phulgobhi", "Cauliflower");   CROP_DICTIONARY.put("cauliflower", "Cauliflower");
        // Cabbage
        CROP_DICTIONARY.put("bandgobi", "Cabbage");   CROP_DICTIONARY.put("bandgobhi", "Cabbage");
        CROP_DICTIONARY.put("band gobhi", "Cabbage"); CROP_DICTIONARY.put("band gobi", "Cabbage");
        CROP_DICTIONARY.put("bund gobhi", "Cabbage"); CROP_DICTIONARY.put("bundgobi", "Cabbage");
        CROP_DICTIONARY.put("patta gobhi", "Cabbage"); CROP_DICTIONARY.put("pattagobhi", "Cabbage");
        CROP_DICTIONARY.put("pattagonbi", "Cabbage"); CROP_DICTIONARY.put("cabbage", "Cabbage");
        // Peas
        CROP_DICTIONARY.put("matar", "Peas");      CROP_DICTIONARY.put("mattar", "Peas");
        CROP_DICTIONARY.put("mutter", "Peas");     CROP_DICTIONARY.put("muttar", "Peas");
        CROP_DICTIONARY.put("hari matar", "Peas"); CROP_DICTIONARY.put("peas", "Peas");
        // Ridge Gourd
        CROP_DICTIONARY.put("turai", "Ridge Gourd");  CROP_DICTIONARY.put("torai", "Ridge Gourd");
        CROP_DICTIONARY.put("turiya", "Ridge Gourd"); CROP_DICTIONARY.put("ridge gourd", "Ridge Gourd");
        // Drumstick
        CROP_DICTIONARY.put("sahjan", "Drumstick");  CROP_DICTIONARY.put("saijan", "Drumstick");
        CROP_DICTIONARY.put("moringa", "Drumstick"); CROP_DICTIONARY.put("drumstick", "Drumstick");
        // Sweet Potato
        CROP_DICTIONARY.put("shakarkand", "Sweet Potato"); CROP_DICTIONARY.put("shakarkandi", "Sweet Potato");
        CROP_DICTIONARY.put("sweet potato", "Sweet Potato");
        // Taro / Colocasia
        CROP_DICTIONARY.put("arbi", "Taro");  CROP_DICTIONARY.put("arvi", "Taro");
        CROP_DICTIONARY.put("taro", "Taro");  CROP_DICTIONARY.put("colocasia", "Taro");
        // Turnip
        CROP_DICTIONARY.put("shalgam", "Turnip"); CROP_DICTIONARY.put("turnip", "Turnip");
        // Beetroot
        CROP_DICTIONARY.put("chukandar", "Beetroot"); CROP_DICTIONARY.put("beetroot", "Beetroot");
        CROP_DICTIONARY.put("beet", "Beetroot");
        // French Beans / Green Beans
        CROP_DICTIONARY.put("beans", "French Beans");        CROP_DICTIONARY.put("french beans", "French Beans");
        CROP_DICTIONARY.put("green beans", "French Beans");  CROP_DICTIONARY.put("farasbi", "French Beans");
        CROP_DICTIONARY.put("pharasbi", "French Beans");     CROP_DICTIONARY.put("frasbi", "French Beans");
        CROP_DICTIONARY.put("sem", "French Beans");          CROP_DICTIONARY.put("seam", "French Beans");
        // Cowpea / Lobia / Bodi
        CROP_DICTIONARY.put("bodi", "Cowpea");    CROP_DICTIONARY.put("bori", "Cowpea");
        CROP_DICTIONARY.put("lobia", "Cowpea");   CROP_DICTIONARY.put("lobiya", "Cowpea");
        CROP_DICTIONARY.put("lobhia", "Cowpea");  CROP_DICTIONARY.put("cowpea", "Cowpea");
        CROP_DICTIONARY.put("long beans", "Cowpea");
        // Cluster Beans / Guar
        CROP_DICTIONARY.put("guar", "Cluster Beans");    CROP_DICTIONARY.put("gawar", "Cluster Beans");
        CROP_DICTIONARY.put("guvar", "Cluster Beans");   CROP_DICTIONARY.put("gavar", "Cluster Beans");
        CROP_DICTIONARY.put("cluster beans", "Cluster Beans");
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
