package com.tyndalehouse.step.core.service.impl;

import static com.tyndalehouse.step.core.utils.ValidateUtils.notBlank;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.avaje.ebean.EbeanServer;
import com.tyndalehouse.step.core.data.entities.LexiconDefinition;
import com.tyndalehouse.step.core.exceptions.UserExceptionType;
import com.tyndalehouse.step.core.service.VocabularyService;

/**
 * defines all vocab related queries
 * 
 * @author chrisburrell
 * 
 */
@Singleton
public class VocabularyServiceImpl implements VocabularyService {
    private static final String STRONG_SEPARATORS = "[ ,]+";
    private static final String HIGHER_STRONG = "STRONG:";
    private static final String LOWER_STRONG = "strong:";
    private static final int START_STRONG_KEY = HIGHER_STRONG.length();
    private final EbeanServer ebean;

    // define a few extraction methods
    private final LexiconDataProvider transliterationProvider = new LexiconDataProvider() {
        @Override
        public String getData(final LexiconDefinition l) {
            return l.getSimpleTransliteration();
        }
    };
    private final LexiconDataProvider englishVocabProvider = new LexiconDataProvider() {
        @Override
        public String getData(final LexiconDefinition l) {
            return l.getShortDefinition();
        }
    };
    private final LexiconDataProvider greekVocabProvider = new LexiconDataProvider() {
        @Override
        public String getData(final LexiconDefinition l) {
            return l.getOriginal();
        }
    };

    /**
     * @param ebean the database server
     */
    @Inject
    public VocabularyServiceImpl(final EbeanServer ebean) {
        this.ebean = ebean;
    }

    @Override
    public List<LexiconDefinition> getDefinitions(final String vocabIdentifiers) {
        notBlank(vocabIdentifiers, "Vocab identifiers was null", UserExceptionType.SERVICE_VALIDATION_ERROR);

        final List<String> idList = getKeys(vocabIdentifiers);

        if (!idList.isEmpty()) {
            return this.ebean.find(LexiconDefinition.class).where().idIn(idList).findList();
        }
        return new ArrayList<LexiconDefinition>();

    }

    @Override
    public String getEnglishVocab(final String vocabIdentifiers) {
        return getDataFromLexiconDefinition(vocabIdentifiers, this.englishVocabProvider);
    }

    @Override
    public String getGreekVocab(final String vocabIdentifiers) {
        return getDataFromLexiconDefinition(vocabIdentifiers, this.greekVocabProvider);
    }

    @Override
    public String getDefaultTransliteration(final String vocabIdentifiers) {
        return getDataFromLexiconDefinition(vocabIdentifiers, this.transliterationProvider);
    }

    /**
     * gets data from the matched lexicon definitions
     * 
     * @param vocabIdentifiers the identifiers
     * @param provider the provider used to get data from it
     * @return the data in String form
     */
    private String getDataFromLexiconDefinition(final String vocabIdentifiers,
            final LexiconDataProvider provider) {
        final List<String> keys = getKeys(vocabIdentifiers);
        if (keys.isEmpty()) {
            return "";
        }

        // else we lookup and concatenate
        final List<LexiconDefinition> lds = getLexiconDefinitions(keys);

        // TODO - if nothing there, for now we just return the ids we got
        if (lds.isEmpty()) {
            return vocabIdentifiers;
        }

        final StringBuilder sb = new StringBuilder(lds.size() * 32);
        for (final LexiconDefinition l : lds) {
            sb.append(provider.getData(l));
        }

        return sb.toString();
    }

    /**
     * returns the lexicon definitions
     * 
     * @param keys the keys to match
     * @return the lexicon definitions that were found
     */
    private List<LexiconDefinition> getLexiconDefinitions(final List<String> keys) {
        final List<LexiconDefinition> lds = this.ebean.find(LexiconDefinition.class)
                .select("original,simpleTransliteration,shortDefinition").where().idIn(keys).findList();
        return lds;
    }

    /**
     * Extracts a compound key into several keys
     * 
     * @param vocabIdentifiers the vocabulary identifiers
     * @return the list of all keys to lookup
     */
    List<String> getKeys(final String vocabIdentifiers) {
        final List<String> idList = new ArrayList<String>();
        final String[] ids = vocabIdentifiers.split(STRONG_SEPARATORS);

        for (final String i : ids) {

            if (i.length() > START_STRONG_KEY + 1
                    && (i.startsWith(LOWER_STRONG) || i.startsWith(HIGHER_STRONG))) {
                idList.add(padStrongNumber(i, true));
            }
        }
        return idList;
    }

    /**
     * Pads a strong number with the correct number of 0s
     * 
     * @param strongNumber the strong number
     * @param prefix true to indicate the strongNumber is preceded with strong:
     * @return the padded strong number
     */
    public static String padStrongNumber(final String strongNumber, final boolean prefix) {
        final int baseIndex = prefix ? START_STRONG_KEY : 0;
        return String.format("%c%04d", strongNumber.charAt(baseIndex),
                Integer.parseInt(strongNumber.substring(baseIndex + 1)));
    }
}