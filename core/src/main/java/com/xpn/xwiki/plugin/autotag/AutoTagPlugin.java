package com.xpn.xwiki.plugin.autotag;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

import java.util.*;

public class AutoTagPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
    public static final int LANG_FRENCH = 0;
    public static final int LANG_ENGLISH = 1;
    public static String name = "autotag";

    public final static String[] FRENCH_STOP_WORDS = {
    "a", "afin", "ai", "ainsi", "apr�s", "attendu", "au", "aujourd", "auquel", "aussi",
    "autre", "autres", "aux", "auxquelles", "auxquels", "avait", "avant", "avec", "avoir",
    "c", "car", "ce", "ceci", "cela", "celle", "celles", "celui", "cependant", "certain",
    "certaine", "certaines", "certains", "ces", "cet", "cette", "ceux", "chez", "ci",
    "combien", "comme", "comment", "concernant", "contre", "d", "dans", "de", "debout",
    "dedans", "dehors", "del�", "depuis", "derri�re", "des", "d�sormais", "desquelles",
    "desquels", "dessous", "dessus", "devant", "devers", "devra", "divers", "diverse",
    "diverses", "doit", "donc", "dont", "du", "duquel", "durant", "d�s", "elle", "elles",
    "en", "entre", "environ", "est", "et", "etc", "etre", "eu", "eux", "except�", "hormis",
    "hors", "h�las", "hui", "il", "ils", "j", "je", "jusqu", "jusque", "l", "la", "laquelle",
    "le", "lequel", "les", "lesquelles", "lesquels", "leur", "leurs", "lorsque", "lui", "l�",
    "ma", "mais", "malgr�", "me", "merci", "mes", "mien", "mienne", "miennes", "miens", "moi",
    "moins", "mon", "moyennant", "m�me", "m�mes", "n", "ne", "ni", "non", "nos", "notre",
    "nous", "n�anmoins", "n�tre", "n�tres", "on", "ont", "ou", "outre", "o�", "par", "parmi",
    "partant", "pas", "pass�", "pendant", "plein", "plus", "plusieurs", "pour", "pourquoi",
    "proche", "pr�s", "puisque", "qu", "quand", "que", "quel", "quelle", "quelles", "quels",
    "qui", "quoi", "quoique", "revoici", "revoil�", "s", "sa", "sans", "sauf", "se", "selon",
    "seront", "ses", "si", "sien", "sienne", "siennes", "siens", "sinon", "soi", "soit",
    "son", "sont", "sous", "suivant", "sur", "ta", "te", "tes", "tien", "tienne", "tiennes",
    "tiens", "toi", "ton", "tous", "tout", "toute", "toutes", "tu", "un", "une", "va", "vers",
    "voici", "voil�", "vos", "votre", "vous", "vu", "v�tre", "v�tres", "y", "�", "�a", "�s",
    "�t�", "�tre", "�", "avez", "parce", "suis"
  };

    List ignoreList = new ArrayList();
    List dontignoreList = null;
    int maxTag = 100;
    int maxTagSize = 64;
    int minTagSize = 12;

    public AutoTagPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
        init(context);
    }

    public void init(XWikiContext context) {
        super.init(context);
    }    

    /**
     * Allow to get the plugin name
     *
     * @return plugin name
     */
    public String getName() {
        return name;
    }

    
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new AutoTagPluginAPI((AutoTagPlugin) plugin, context);
    }

    public TagCloud countWords(String text, int lang){
        Map wordList;
        TagCloud tagcloud = new TagCloud();
        tagcloud.setText(text);

        splitWords(tagcloud);
        wordList = countWords(tagcloud);
        wordList = clearStopWords(tagcloud, lang);
        wordList = stemmer(tagcloud, lang);
        return tagcloud;
    }

    public TagCloud generateTagCloud(String text, int lang){
        TagCloud tagcloud = countWords(text, lang);
        calculateTags(tagcloud);
        return tagcloud;
    }

    private Set calculateTags(TagCloud tagcloud){
        Map stemmedWords = tagcloud.getStemmedWordMap();
        Map stemmedWordFreqMap = new HashMap();

        Iterator it =  stemmedWords.keySet().iterator();

        // we calculate the frequency of each word
        while(it.hasNext()){
            Integer totalFreq = new Integer(0);
            String leadWord = "";
            Integer leadFreq = new Integer(0);

            String stemmedWord = (String) it.next();
            Map wordMap = (Map) stemmedWords.get(stemmedWord);

            Iterator itWordMap = wordMap.keySet().iterator();
            while(itWordMap.hasNext()){
                String word = (String) itWordMap.next();

                Integer freq = ((Integer)wordMap.get(word));

                totalFreq = new Integer(freq.intValue() + totalFreq.intValue());

                if (freq.intValue() > leadFreq.intValue()){
                    leadFreq = ((Integer)wordMap.get(word));
                    leadWord = word;
                }
            }
            stemmedWordFreqMap.put(leadWord, totalFreq);
        }
        tagcloud.setStemmedWordFreqMap(stemmedWordFreqMap);

        // we order the list by the value to select the most frequent tags
        LinkedHashMap orderedMap = sortMap(stemmedWordFreqMap);

        Object[] keyset = orderedMap.keySet().toArray();

        LinkedHashMap tagMap = new LinkedHashMap();

        for(int i = 0, pos = keyset.length - 1; i < keyset.length && i < maxTag; i++, pos--){
            tagMap.put(keyset[pos], orderedMap.get(keyset[pos]));
        }


        Object[] freqs = tagMap.values().toArray();

        Integer maxFreq = (Integer) freqs[0];
        Integer minFreq = (Integer) freqs[freqs.length - 1];

        Integer ftot = new Integer(0);

        for(int i = 0; i < freqs.length; i++)
            ftot = new Integer(((Integer)freqs[i]).intValue() + ftot.intValue());

        it = sortSet(tagMap.keySet()).iterator();
        SortedSet tagSet = new TreeSet();

        while(it.hasNext()){
            String tagName = (String) it.next();
            long size = getTagSize(((Integer)tagMap.get(tagName)).intValue(), maxFreq.intValue(), minFreq.intValue(), ftot.intValue());
            Tag tag = new Tag(tagName, size);
            tagSet.add(tag);
        }
        tagcloud.setTags(tagSet);
        return tagSet;
    }


    private long getTagSize(double freq,double fmax, double fmin, double ftot)
    {
      int fontrange = maxTagSize - minTagSize;

      // tweak this if all the words seem too similar in size or extremely different
      // rely on the cumulative by x% (0 = 0%, 1 = 100%)
      double cuml_importance = 0.7;

      // sizes based on word's frequency vs total/cumulative frequency
      double sumpx = ((fontrange * cuml_importance) + 1) * (fontrange * cuml_importance) / 2;
      double px = freq / ftot * sumpx;

      // sizes based on word's frequency deviation from max/min frequencies
      px += Math.pow((freq-fmin)/(1 > fmax-fmin ? 1 : fmax-fmin ), 0.8) * (fontrange * (1 - cuml_importance));
      double res = maxTagSize < px + minTagSize ? maxTagSize: px + minTagSize;
      return Math.round(res);
    }


    public static SortedSet sortSet(Set oSet){
        SortedSet set = new TreeSet();

        Iterator it = oSet.iterator();
        while(it.hasNext()){
            set.add(it.next());
        }
        return set;
    }

    public static LinkedHashMap sortMap(Map hmap){
		LinkedHashMap map = new LinkedHashMap();

        List mapKeys = new ArrayList(hmap.keySet());
		List mapValues = new ArrayList(hmap.values());

        //hmap.clear();

        TreeSet sortedSet = new TreeSet(mapValues);
		Object[] sortedArray = sortedSet.toArray();

		for (int i=0; i<sortedArray.length; i++)
		{
            for (int j = 0; j < mapValues.size(); j++){
                if (mapValues.get(j).equals(sortedArray[i]))
                    map.put(mapKeys.get(j), sortedArray[i]);
            }
		}
		return map;
	}

    private Map clearStopWords(TagCloud tagcloud, int lang){
        Map words = tagcloud.getCountedWordMap();
        if (lang == LANG_FRENCH){
            for (int i = 0; i < FRENCH_STOP_WORDS.length; i++){
                words.remove(FRENCH_STOP_WORDS[i]);
            }
        }

        Iterator wit = words.keySet().iterator();
        while(wit.hasNext()) {
            String word = (String) wit.next();
            if (word.contains("<")||word.contains(">")||word.contains("=")||word.contains("\"")||word.contains("/")||word.contains("�"))
                if (!ignoreList.contains(word))
                    ignoreList.add(word);
        }

        if (ignoreList != null){
            Iterator it = ignoreList.iterator();
            while(it.hasNext()){
                String word = (String) it.next();
                if (dontignoreList == null || !dontignoreList.contains(word))
                    words.remove(word);
            }
        }


        tagcloud.setCountedWordMap(words);
        return words;
    }

    private Map countWords(TagCloud tagcloud){
        String[] words = tagcloud.getWordList();
        Map wordsCnt = new HashMap();

        for (int i = 0; i < words.length; i++){
            String word = words[i];
            if (!wordsCnt.containsKey(word)){
                wordsCnt.put(word, new Integer(0));
            }
            wordsCnt.put(word, new Integer(((Integer)wordsCnt.get(word)).intValue() + 1));
        }
        tagcloud.setCountedWordMap(wordsCnt);
        return wordsCnt;
    }

    private Map stemmer(TagCloud tagcloud, int lang){
        Map words = tagcloud.getCountedWordMap();
        FrenchStemmer stemmer;
        Map stemmedWordMap = new HashMap();

            stemmer = new FrenchStemmer();


        Iterator it = words.keySet().iterator();
        while(it.hasNext()) {
            String word = (String) it.next();
            if (word.length() <= 2)
                continue;
            String stemmedWord = stemmer.stem(word);
            if (!stemmedWordMap.containsKey(stemmedWord))
                stemmedWordMap.put(stemmedWord, new HashMap());
            ((Map) stemmedWordMap.get(stemmedWord)).put(word, words.get(word));
        }
        tagcloud.setStemmedWordMap(stemmedWordMap);
        return stemmedWordMap;
    }

/*
    private Map stemmer(TagCloud tagcloud, int lang){
        Map words = tagcloud.getCountedWordMap();
        SnowballProgram stemmer;
        Map stemmedWordMap = new HashMap();

        if (lang == LANG_FRENCH){
            stemmer = new frenchStemmer();
        }
        else {
            stemmer = new englishStemmer();
        }


        Iterator it = words.keySet().iterator();
        while(it.hasNext()) {
            String word = (String) it.next();
            if (word.length() <= 2)
                continue;
            stemmer.setCurrent(word);
            stemmer.stem();
            String stemmedWord = stemmer.getCurrent();
            if (!stemmedWordMap.containsKey(stemmedWord))
                stemmedWordMap.put(stemmedWord, new HashMap());
            ((Map) stemmedWordMap.get(stemmedWord)).put(word, words.get(word));
        }
        tagcloud.setStemmedWordMap(stemmedWordMap);
        return stemmedWordMap;
    }
    */
    private String[] splitWords(TagCloud tagcloud){
        String text = tagcloud.getText();
        text = text.replaceAll("\n", " ");
        text = text.replaceAll("\r", " ");
        text = text.replaceAll("'", " ");
        text = text.replaceAll("\u0092", " ");
        text = text.toLowerCase();
        String[] words = text.split("[\\s,.;:!\\?]+");
        tagcloud.setWordList(words);
        return words;
    }

}
