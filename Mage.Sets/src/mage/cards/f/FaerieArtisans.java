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
package mage.cards.f;

import java.util.StringTokenizer;
import java.util.UUID;
import mage.MageInt;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldAllTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.CreateTokenCopyTargetEffect;
import mage.abilities.keyword.FlyingAbility;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.cards.Cards;
import mage.cards.CardsImpl;
import mage.constants.*;
import mage.filter.common.FilterCreaturePermanent;
import mage.filter.predicate.Predicates;
import mage.filter.predicate.permanent.ControllerPredicate;
import mage.filter.predicate.permanent.TokenPredicate;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.targetpointer.FixedTarget;

/**
 *
 * @author LevelX2
 */
public class FaerieArtisans extends CardImpl {

    private static final FilterCreaturePermanent filterNontoken = new FilterCreaturePermanent("nontoken creature");

    static {
        filterNontoken.add(Predicates.not(new TokenPredicate()));
        filterNontoken.add(new ControllerPredicate(TargetController.OPPONENT));
    }

    public FaerieArtisans(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{3}{U}");

        this.subtype.add(SubType.FAERIE);
        this.subtype.add(SubType.ARTIFICER);
        this.power = new MageInt(2);
        this.toughness = new MageInt(2);

        // Flying
        this.addAbility(FlyingAbility.getInstance());
        // Whenever a nontoken creature enters the battlefield under an opponent's control, create a token that's a copy of that creature except it's an artifact in addition to its other types. Then exile all other tokens created with Faerie Artisans.
        Ability ability = new EntersBattlefieldAllTriggeredAbility(Zone.BATTLEFIELD, new FaerieArtisansEffect(), filterNontoken, false, SetTargetPointer.PERMANENT,
                "Whenever a nontoken creature enters the battlefield under an opponent's control, create a token that's a copy of that creature except it's an artifact in addition to its other types. Then exile all other tokens created with {this}.");
        this.addAbility(ability);
    }

    public FaerieArtisans(final FaerieArtisans card) {
        super(card);
    }

    @Override
    public FaerieArtisans copy() {
        return new FaerieArtisans(this);
    }
}

class FaerieArtisansEffect extends OneShotEffect {

    public FaerieArtisansEffect() {
        super(Outcome.PutCreatureInPlay);
        this.staticText = "create a token that's a copy of that creature except it's an artifact in addition to its other types. "
                + "Then exile all other tokens created with {this}";
    }

    public FaerieArtisansEffect(final FaerieArtisansEffect effect) {
        super(effect);
    }

    @Override
    public FaerieArtisansEffect copy() {
        return new FaerieArtisansEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent permanentToCopy = game.getPermanentOrLKIBattlefield(getTargetPointer().getFirst(game, source));
        Player controller = game.getPlayer(source.getControllerId());
        if (controller != null && permanentToCopy != null) {
            CreateTokenCopyTargetEffect effect = new CreateTokenCopyTargetEffect(null, CardType.ARTIFACT, false);
            effect.setTargetPointer(new FixedTarget(permanentToCopy, game));
            if (effect.apply(game, source)) {
                String oldTokens = (String) game.getState().getValue(source.getSourceId().toString() + source.getSourceObjectZoneChangeCounter());
                StringBuilder sb = new StringBuilder();
                for (Permanent permanent : effect.getAddedPermanent()) {
                    if (sb.length() > 0) {
                        sb.append(';');
                    }
                    sb.append(permanent.getId());
                }
                game.getState().setValue(source.getSourceId().toString() + source.getSourceObjectZoneChangeCounter(), sb.toString());

                if (oldTokens != null) {
                    Cards cards = new CardsImpl();
                    StringTokenizer tokenizer = new StringTokenizer(oldTokens, ";");
                    while (tokenizer.hasMoreTokens()) {
                        String tokenId = tokenizer.nextToken();
                        cards.add(UUID.fromString(tokenId));
                    }
                    controller.moveCards(cards, Zone.EXILED, source, game);
                }
                return true;
            }
        }

        return false;
    }
}
