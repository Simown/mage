/*
 *  Copyright 2010 BetaSteward_at_googlemail.com. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY BetaSteward_at_googlemail.com ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BetaSteward_at_googlemail.com OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and should not be interpreted as representing official policies, either expressed
 *  or implied, of BetaSteward_at_googlemail.com.
 */
package mage.client.deck.generator;

import java.util.*;

import mage.cards.Card;
import mage.cards.decks.Deck;
import mage.cards.repository.CardCriteria;
import mage.cards.repository.CardInfo;
import mage.cards.repository.CardRepository;
import mage.cards.repository.ExpansionInfo;
import mage.cards.repository.ExpansionRepository;
import mage.client.dialog.PreferencesDialog;
import mage.client.util.sets.ConstructedFormats;
import mage.constants.CardType;
import mage.constants.ColoredManaSymbol;
import mage.constants.Rarity;


/**
 * Generates random card pool and builds a deck.
 *
 * @author nantuko
 * @author Simown
 */
public class DeckGenerator {

    private static final int MAX_TRIES = 8196;
    private static DeckGeneratorDialog genDialog;
    private static DeckGeneratorPool genPool;

    public static String generateDeck() {

        genDialog = new DeckGeneratorDialog();
        if (genDialog.getSelectedColors() != null) {
            Deck deck = buildDeck();
            String deckPath = genDialog.saveDeck(deck);
            return deckPath;
        }
        // If the deck couldn't be generated or the user cancelled, repopulate the deck selection with its cached value
        return PreferencesDialog.getCachedValue(PreferencesDialog.KEY_NEW_TABLE_DECK_FILE, null);
    }

    protected static Deck buildDeck() {

        String selectedColors = genDialog.getSelectedColors();
        List<ColoredManaSymbol> allowedColors = new ArrayList<ColoredManaSymbol>();
        selectedColors = selectedColors != null ? selectedColors.toUpperCase() : getRandomColors("X");
        String format = genDialog.getSelectedFormat();

        List<String> setsToUse = ConstructedFormats.getSetsByFormat(format);
        if (setsToUse.isEmpty()) {
            // Default to using all sets
            setsToUse = ExpansionRepository.instance.getSetCodes();
        }

        int deckSize = genDialog.getDeckSize();

        if (selectedColors.contains("X")) {
            selectedColors = getRandomColors(selectedColors);
        }

        for (int i = 0; i < selectedColors.length(); i++) {
            char c = selectedColors.charAt(i);
            allowedColors.add(ColoredManaSymbol.lookup(c));
        }

        Deck finalDeck = generateDeck(deckSize, allowedColors, setsToUse);
        return finalDeck;
    }


    private static String getRandomColors(String selectedColors) {

        Random random = new Random();
        List<Character> availableColors = new ArrayList();
        for (ColoredManaSymbol cms : ColoredManaSymbol.values()) {
            availableColors.add(cms.toString().charAt(0));
        }

        StringBuilder generatedColors = new StringBuilder();
        int randomColors = 0;
        for (int i = 0; i < selectedColors.length(); i++) {
            char currentColor = selectedColors.charAt(i);
            if (currentColor != 'X') {
                generatedColors.append(currentColor);
                availableColors.remove(new Character(currentColor));
            } else {
                randomColors++;
            }
        }
        for (int i = 0; i < randomColors && !availableColors.isEmpty(); i++) {
            int index = random.nextInt(availableColors.size());
            generatedColors.append(availableColors.remove(index));
        }
        return generatedColors.toString();
    }

    private static Deck generateDeck(int deckSize, List<ColoredManaSymbol> allowedColors, List<String> setsToUse) {
        genPool = new DeckGeneratorPool(deckSize, allowedColors, genDialog.isSingleton());

        final String[] sets = setsToUse.toArray(new String[0]);

        // Creatures
        final CardCriteria creatureCriteria = new CardCriteria();
        creatureCriteria.setCodes(sets);
        creatureCriteria.notTypes(CardType.LAND);
        creatureCriteria.types(CardType.CREATURE);
        if (!(genDialog.useArtifacts()))
            creatureCriteria.notTypes(CardType.ARTIFACT);

        // Non-creatures (sorcery, instant, enchantment, artifact etc.)
        final CardCriteria nonCreatureCriteria = new CardCriteria();
        nonCreatureCriteria.setCodes(sets);
        nonCreatureCriteria.notTypes(CardType.LAND);
        nonCreatureCriteria.notTypes(CardType.CREATURE);
        if (!(genDialog.useArtifacts()))
            nonCreatureCriteria.notTypes(CardType.ARTIFACT);

        // Non-basic land
        final CardCriteria nonBasicLandCriteria = new CardCriteria();
        nonBasicLandCriteria.setCodes(sets);
        nonBasicLandCriteria.types(CardType.LAND);
        nonBasicLandCriteria.notSupertypes("Basic");

        // Generate basic land cards
        Map<String, Card> basicLands = generateBasicLands(setsToUse);

        generateSpells(creatureCriteria, genPool.getCreatureCount());
        generateSpells(nonCreatureCriteria, genPool.getNonCreatureCount());
        generateLands(nonBasicLandCriteria, genPool.getLandCount(), basicLands);

        // Reconstructs the final deck and adjusts for Math rounding and/or missing cards
        return genPool.getDeck();
    }

    private static Map<String, Card> generateBasicLands(List<String> setsToUse) {
        return null;
    }

    private static void generateSpells(CardCriteria criteria, int spellCount) {
        List<CardInfo> cardPool = CardRepository.instance.findCards(criteria);
        int retrievedCount = cardPool.size();
        List<DeckGeneratorCMC> deckCMCs = genPool.getCMCsForSpellCount(spellCount);
        Random random = new Random();
        int count = 0;
        int reservesAdded = 0;
        if (retrievedCount > 0 && retrievedCount >= spellCount) {
            int tries = 0;
            while (count < spellCount) {
                Card card = cardPool.get(random.nextInt(retrievedCount)).getMockCard();
                if (genPool.isValidSpellCard(card)) {
                    int cardCMC = card.getManaCost().convertedManaCost();
                    for (DeckGeneratorCMC deckCMC : deckCMCs) {
                        if (cardCMC >= deckCMC.min && cardCMC <= deckCMC.max) {
                            int currentAmount = deckCMC.getAmount();
                            if (currentAmount > 0) {
                                deckCMC.setAmount(currentAmount - 1);
                                genPool.addCard(card.copy());
                                count++;
                            }
                        } else {
                            if (reservesAdded < genPool.getDeckSize() / 2) {
                                genPool.tryAddReserve(card, cardCMC);
                                reservesAdded++;
                            }
                        }
                    }
                }
                tries++;
                if (tries > MAX_TRIES) {
                    // Break here, we'll fill in random missing ones later
                    break;
                }
            }
        } else {
            throw new IllegalStateException("Not enough cards to generate deck.");
        }

    }

    private static void generateLands(CardCriteria criteria, int landsCount, Map<String, Card> basicLands) {

        int tries = 0;
        int countNonBasic = 0;
        // Store the nonbasic lands (if any) we'll add
        List<Card> deckLands = new ArrayList<>();

        // Calculates the percentage of coloured mana symbols over all spells in the deck
        Map<String, Double> percentage = genPool.calculateSpellColourPercentages();

        // Only dual/tri colour lands are generated for now, and not non-basic lands that only produce colourless mana.
        if (!genPool.isMonoColoredDeck() && genDialog.useNonBasicLand()) {
            List<Card> landCards = genPool.filterLands(CardRepository.instance.findCards(criteria));
            int allCount = landCards.size();
            Random random = new Random();
            if (allCount > 0) {
                while (countNonBasic < landsCount / 2) {
                    Card card = landCards.get(random.nextInt(allCount));
                    if (genPool.isValidLandCard(card)) {
                        Card addedCard = card.copy();
                        deckLands.add(addedCard);
                        genPool.addCard(addedCard);
                        countNonBasic++;
                    }
                    tries++;
                    // to avoid infinite loop
                    if (tries > MAX_TRIES) {
                        // Not a problem, just use what we have
                        break;
                    }
                }
            }
        }
        // Calculate the amount of coloured mana already can be produced by the non-basic lands
        Map<String, Integer> count = genPool.calculateManaCounts(deckLands);
        // Fill up the rest of the land quota with basic lands adjusted to fit the deck's mana costs
        addBasicLands(landsCount - countNonBasic, percentage, count, basicLands);
    }

    private static void addBasicLands(int landsNeeded, Map<String, Double> percentage, Map<String, Integer> count, Map<String, Card> basicLands) {
        int colorTotal = 0;
        ColoredManaSymbol colourToAdd = null;

        // Add up the totals for all colors, to keep track of the percentage a color is.
        for (Map.Entry<String, Integer> c : count.entrySet()) {
            colorTotal += c.getValue();
        }

        // Keep adding basic lands until we fill the deck
        while (landsNeeded > 0) {

            double minPercentage = Integer.MIN_VALUE;

            for (ColoredManaSymbol color : ColoredManaSymbol.values()) {
                // What percentage of this color is requested
                double neededPercentage = percentage.get(color.toString());
                // If there is a 0% need for basic lands of this colour, skip it
                if (neededPercentage <= 0) {
                    continue;
                }
                int currentCount = count.get(color.toString());
                double thisPercentage = 0.0;
                // Calculate the percentage of lands so far that produce this colour
                if (currentCount > 0)
                    thisPercentage = (currentCount / (double) colorTotal) * 100.0;
                // Check if the color is the most "needed" (highest percentage) we have seen so far
                if (neededPercentage - thisPercentage > minPercentage) {
                    // Put this color land forward to be added
                    colourToAdd = color;
                    minPercentage = (neededPercentage - thisPercentage);
                }
            }
            // TODO: Massive loops for each land selected, fix this
            genPool.addCard(getBasicLand(colourToAdd, basicLands));
            count.put(colourToAdd.toString(), count.get(colourToAdd.toString()) + 1);
            colorTotal++;
            landsNeeded--;
        }
    }

    private static Card getBasicLand(String landName) {
        return null;
    }

    private static Card getBasicLand(ColoredManaSymbol color, Map<String, Card> basicLands) {

        String landName = genPool.getBasicLandName(color.toString());
        return getBasicLand(landName);
    }

//    private static Card refactorThisNow()
//    {
//        List<String> landSets = new LinkedList<String>();
//
//        // decide from which sets basic lands are taken from
//        for (String setCode :setsToUse) {
//            ExpansionInfo expansionInfo = ExpansionRepository.instance.getSetByCode(setCode);
//            if (expansionInfo.hasBasicLands()) {
//                landSets.add(expansionInfo.getCode());
//            }
//        }
//
//        // if sets have no basic land, take land from block
//        if (landSets.isEmpty()) {
//            for (String setCode :setsToUse) {
//                ExpansionInfo expansionInfo = ExpansionRepository.instance.getSetByCode(setCode);
//                List<ExpansionInfo> blockSets = ExpansionRepository.instance.getSetsFromBlock(expansionInfo.getBlockName());
//                for (ExpansionInfo blockSet: blockSets) {
//                    if (blockSet.hasBasicLands()) {
//                        landSets.add(blockSet.getCode());
//                    }
//                }
//            }
//        }
//        // if still no set with lands found, take one by random
//        if (landSets.isEmpty()) {
//            // if sets have no basic lands and also it has no parent or parent has no lands get last set with lands
//            // select a set with basic lands by random
//            Random generator = new Random();
//            List<ExpansionInfo> basicLandSets = ExpansionRepository.instance.getSetsWithBasicLandsByReleaseDate();
//            if (basicLandSets.size() > 0) {
//                landSets.add(basicLandSets.get(generator.nextInt(basicLandSets.size())).getCode());
//            }
//        }
//
//        if (landSets.isEmpty()) {
//            throw new IllegalArgumentException("No set with basic land was found");
//        }
//
//        CardCriteria criteria = new CardCriteria();
//        if (!landSets.isEmpty()) {
//            criteria.setCodes(landSets.toArray(new String[landSets.size()]));
//        }
//        criteria.rarities(Rarity.LAND).name(landName);
//        List<CardInfo> cards = CardRepository.instance.findCards(criteria);
//
//        if (cards.isEmpty() && !setsToUse.isEmpty()) {
//            cards = CardRepository.instance.findCards(landName);
//        }
//
//        int randomInt = new Random().nextInt(cards.size());
//        return cards.get(randomInt).getMockCard();
//
//    }

}
