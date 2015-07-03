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
package mage.sets.magicorigins;

import java.util.UUID;
import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.common.EntersBattlefieldTriggeredAbility;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.keyword.FlashAbility;
import mage.cards.CardImpl;
import mage.constants.CardType;
import mage.constants.Outcome;
import mage.constants.Rarity;
import mage.game.Game;
import mage.game.stack.Spell;
import mage.game.stack.StackAbility;
import mage.game.stack.StackObject;
import mage.players.Player;
import mage.target.Target;
import mage.target.TargetStackObject;
import mage.target.Targets;

/**
 *
 * @author fireshoes
 */
public class MizziumMeddler extends CardImpl {

    public MizziumMeddler(UUID ownerId) {
        super(ownerId, 64, "Mizzium Meddler", Rarity.RARE, new CardType[]{CardType.CREATURE}, "{2}{U}");
        this.expansionSetCode = "ORI";
        this.subtype.add("Vedalken");
        this.subtype.add("Wizard");
        this.power = new MageInt(1);
        this.toughness = new MageInt(4);

        // Flash
        this.addAbility(FlashAbility.getInstance());
        
        // When Mizzium Meddler enters the battlefield, change a target of target spell or ability to Mizzium Meddler.
        Ability ability = new EntersBattlefieldTriggeredAbility(new MizziumMeddlerEffect());
        ability.addTarget(new TargetStackObject());
        this.addAbility(ability);
    }

    public MizziumMeddler(final MizziumMeddler card) {
        super(card);
    }

    @Override
    public MizziumMeddler copy() {
        return new MizziumMeddler(this);
    }
}

class MizziumMeddlerEffect extends OneShotEffect {

    public MizziumMeddlerEffect() {
        super(Outcome.Neutral);
        staticText = "Change a target of target spell or ability to {this}";
    }

    public MizziumMeddlerEffect(final MizziumMeddlerEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        StackObject stackObject = game.getStack().getStackObject(source.getFirstTarget());
        MageObject sourceObject = game.getObject(source.getSourceId());
        if (stackObject != null && sourceObject != null) {
            Targets targets;
            Ability sourceAbility;
            MageObject oldTarget = null;
            if (stackObject instanceof Spell) {
                Spell spell = (Spell)stackObject;
                sourceAbility = spell.getSpellAbility();
                targets = spell.getSpellAbility().getTargets();
            } else if (stackObject instanceof StackAbility) {
                StackAbility stackAbility = (StackAbility)stackObject;
                sourceAbility = stackAbility;
                targets = stackAbility.getTargets();
            } else {
                return false;
            }
            boolean twoTimesTarget = false;
            if (targets.size() == 1 && targets.get(0).getTargets().size() == 1) {
                Target target = targets.get(0);
                if (target.canTarget(stackObject.getControllerId(), source.getSourceId(), sourceAbility, game)) {
                    oldTarget = game.getObject(targets.getFirstTarget());
                    target.clearChosen();
                    // The source is still the spell on the stack
                    target.addTarget(source.getSourceId(), stackObject.getStackAbility(), game);
                }                
            } else {
                Player player = game.getPlayer(source.getControllerId());
                for (Target target: targets) {
                    for (UUID targetId: target.getTargets()) {
                        MageObject object = game.getObject(targetId);
                        String name;
                        if (object == null) {
                            Player targetPlayer = game.getPlayer(targetId);
                            name = targetPlayer.getLogName();
                        } else {
                            name = object.getName();
                        }
                        if (!targetId.equals(source.getSourceId()) && target.getTargets().contains(source.getSourceId())) {
                            // you can't change this target to MizziumMeddler because MizziumMeddler is already another targetId of that target.
                            twoTimesTarget = true;
                            continue;
                        }
                        if (name != null && player.chooseUse(Outcome.Neutral, new StringBuilder("Change target from ").append(name).append(" to ").append(sourceObject.getName()).append("?").toString(), source, game)) {
                            if (target.canTarget(stackObject.getControllerId(), source.getSourceId(), sourceAbility, game)) {
                                oldTarget = game.getObject(targets.getFirstTarget());
                                target.remove(targetId);
                                // The source is still the spell on the stack
                                target.addTarget(source.getSourceId(), stackObject.getStackAbility(), game);
                                break;
                            }
                        }
                    }
                }
            }
            if (oldTarget != null) {
                game.informPlayers(sourceObject.getLogName() + ": Changed target of " +stackObject.getLogName() + " from " + oldTarget.getLogName() + " to " + sourceObject.getLogName());
            } else {
                if (twoTimesTarget) {
                    game.informPlayers(sourceObject.getLogName() + ": Target not changed to " + sourceObject.getLogName() + " because its not valid to target it twice for " + stackObject.getName());
                } else {
                    game.informPlayers(sourceObject.getLogName() + ": Target not changed to " + sourceObject.getLogName() + " because its no valid target for " + stackObject.getName());
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public MizziumMeddlerEffect copy() {
        return new MizziumMeddlerEffect(this);
    }

}