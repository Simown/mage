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
        Deck deck = buildDeck();
        String deckPath = genDialog.saveDeck(deck);
        return deckPath;
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

    private static Deck generateDeck(int deckSize, List<ColoredManaSymbol> allowedColors, List<String> setsToUse)
    {
        genPool = new DeckGeneratorPool(deckSize, allowedColors);

        final String [] sets = setsToUse.toArray(new String[0]);

        // Creatures
        final  CardCriteria creatureCriteria = new CardCriteria();
        creatureCriteria.setCodes(sets);
        creatureCriteria.notTypes(CardType.LAND);
        creatureCriteria.types(CardType.CREATURE);

        // Non-creatures (sorcery, instant, enchantment, artifact etc.)
        final CardCriteria nonCreatureCriteria = new CardCriteria();
        nonCreatureCriteria.setCodes(sets);
        nonCreatureCriteria.notTypes(CardType.LAND);
        nonCreatureCriteria.notTypes(CardType.CREATURE);
        nonCreatureCriteria.notTypes(CardType.ARTIFACT);

        // Non-basic land
        final CardCriteria nonBasicLandCriteria = new CardCriteria();
        nonBasicLandCriteria.setCodes(sets);
        nonBasicLandCriteria.types(CardType.LAND);
        //nonBasicLandCriteria.notSupertypes("Basic");

        generateSpells(creatureCriteria, genPool.getCreatureCount());
        generateSpells(nonCreatureCriteria, genPool.getNonCreatureCount());
        generateLands(nonBasicLandCriteria, genPool.getLandCount(), setsToUse);

        // Reconstructs the final deck and adjusts for Math rounding and/or missing cards
        return genPool.getDeck();
    }

    private static void generateSpells(CardCriteria criteria, int spellCount)
    {
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
                    for(DeckGeneratorCMC deckCMC: deckCMCs) {
                        if(cardCMC >= deckCMC.min && cardCMC <= deckCMC.max) {
                            int currentAmount = deckCMC.getAmount();
                            if(currentAmount > 0) {
                                deckCMC.setAmount(currentAmount - 1);
                                genPool.addCard(card);
                                count++;
                            }
                        }
                        else {
                            // Needs working out more - what number is best?
                            if(reservesAdded < genPool.getDeckSize()/2) {
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

    private static void generateLands(CardCriteria criteria, int landsCount, List<String> setsToUse) {

        int tries = 0;
        int countNonBasic = 0;
        // Store the nonbasic lands (if any) we'll add
        List<Card> deckLands = new ArrayList<>();

        // TODO: Make this work
//        // If it's a monocolored deck, don't include any nonbasic/dual lands
//        if(!genPool.isMonoColoredDeck()) {
//
//            // Add all nonbasic?
//            List<CardInfo> landCards = CardRepository.instance.findCards(criteria);
//
//            int allCount = landCards.size();
//            Random random = new Random();
//            if (allCount > 0) {
//                // Up to 50% of lands can be dual lands
//                while (countNonBasic < landsCount/2) {
//                    Card card = landCards.get(random.nextInt(allCount)).getMockCard();
//                    if (genPool.isValidLandCard(card)) {
//                        genPool.addCard(card);
//                        countNonBasic++;
//                    }
//                    tries++;
//                    // to avoid infinite loop
//                    if (tries > MAX_TRIES) {
//                        // Not a problem, just use what we have
//                        break;
//                    }
//                }
//            }
//        }

        // Calculates the percentage of colors over all spells in the deck
        Map<String, Double> percentage = genPool.calculateSpellColourPercentages();
        // Calculate the number of manas already can be produced
        Map<String, Integer> count = genPool.calculateManaCounts(deckLands);
        // The remaining lands are basic lands
        addBasicLands(landsCount - countNonBasic, percentage, count, setsToUse);
    }

    private static void addBasicLands(int landsNeeded, Map<String, Double> percentage, Map<String, Integer> count, List<String> setsToUse)
    {
        int colorTotal = 0;
        ColoredManaSymbol colourToAdd = null;
        for(Map.Entry<String, Integer> c : count.entrySet()) {
            colorTotal += c.getValue();
        }
        while(landsNeeded > 0) {

            double minPercentage = Integer.MIN_VALUE;

            for(ColoredManaSymbol color: ColoredManaSymbol.values()) {
                double neededPercentage = percentage.get(color.toString());
                if(neededPercentage <= 0) {
                    continue;
                }
                int currentCount = count.get(color.toString());
                double thisPercentage = 0.0;
                if(currentCount > 0)
                    thisPercentage = (currentCount/(double)colorTotal) * 100.0;
                if(neededPercentage-thisPercentage > minPercentage) {
                    colourToAdd = color;
                    minPercentage = (neededPercentage-thisPercentage);
                }
            }
            genPool.addCard(getBestBasicLand(colourToAdd, setsToUse));
            count.put(colourToAdd.toString(), count.get(colourToAdd.toString())+1);
            colorTotal++;
            landsNeeded--;
        }
        System.out.print("Done!");
    }

    private static String getRandomColors(String _selectedColors) {
        Random random = new Random();
        List<Character> availableColors = new ArrayList();
        availableColors.add('R');
        availableColors.add('G');
        availableColors.add('B');
        availableColors.add('U');
        availableColors.add('W');

        StringBuilder generatedColors = new StringBuilder();
        int randomColors = 0;
        for (int i = 0; i < _selectedColors.length(); i++) {
            char currentColor = _selectedColors.charAt(i);
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


    private static Card getBestBasicLand(ColoredManaSymbol color, List<String> setsToUse) {
        String cardName = "";
        switch(color) {
            case G:
                cardName = "Forest";
                break;
            case W:
                cardName = "Plains";
                break;
            case R:
                cardName = "Mountain";
                break;
            case B:
                cardName = "Swamp";
                break;
            case U:
                cardName = "Island";
                break;
        }

        List<String> landSets = new LinkedList<String>();

        // decide from which sets basic lands are taken from
        for (String setCode :setsToUse) {
            ExpansionInfo expansionInfo = ExpansionRepository.instance.getSetByCode(setCode);
            if (expansionInfo.hasBasicLands()) {
                landSets.add(expansionInfo.getCode());
            }
        }

        // if sets have no basic land, take land from block
        if (landSets.isEmpty()) {
            for (String setCode :setsToUse) {
                ExpansionInfo expansionInfo = ExpansionRepository.instance.getSetByCode(setCode);
                ExpansionInfo [] blockSets = ExpansionRepository.instance.getSetsFromBlock(expansionInfo.getBlockName());
                for (ExpansionInfo blockSet: blockSets) {
                    if (blockSet.hasBasicLands()) {
                        landSets.add(blockSet.getCode());
                    }
                }
            }
        }
        // if still no set with lands found, take one by random
        if (landSets.isEmpty()) {
            // if sets have no basic lands and also it has no parent or parent has no lands get last set with lands
            // select a set with basic lands by random
            Random generator = new Random();
            List<ExpansionInfo> basicLandSets = ExpansionRepository.instance.getSetsWithBasicLandsByReleaseDate();
            if (basicLandSets.size() > 0) {
                landSets.add(basicLandSets.get(generator.nextInt(basicLandSets.size())).getCode());
            }
        }

        if (landSets.isEmpty()) {
            throw new IllegalArgumentException("No set with basic land was found");
        }

        CardCriteria criteria = new CardCriteria();
        if (!landSets.isEmpty()) {
            criteria.setCodes(landSets.toArray(new String[landSets.size()]));
        }
        criteria.rarities(Rarity.LAND).name(cardName);
        List<CardInfo> cards = CardRepository.instance.findCards(criteria);

        if (cards.isEmpty() && !setsToUse.isEmpty()) {
            cards = CardRepository.instance.findCards(cardName);
        }

        int randomInt = new Random().nextInt(cards.size());
        return cards.get(randomInt).getMockCard();

    }

}
