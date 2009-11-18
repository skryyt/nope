package games.stendhal.server.maps.semos.tavern.market;

import games.stendhal.common.Grammar;
import games.stendhal.server.entity.npc.ChatAction;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.parser.Expression;
import games.stendhal.server.entity.npc.parser.Sentence;
import games.stendhal.server.entity.player.Player;
import games.stendhal.server.entity.trade.Market;
import games.stendhal.server.entity.trade.Offer;

/**
 * puts a new offer to the market
 * @author madmetzger
 *
 */
public class PrepareOfferChatAction implements ChatAction {
	
	public void fire(Player player, Sentence sentence, SpeakerNPC npc) {
		if (sentence.hasError()) {
			npc.say("Sorry, I did not understand that strange offer.");
			npc.setCurrentState(ConversationStates.ATTENDING);
		} else if (sentence.getExpressions().iterator().next().toString().equals("sell")){
			handleSentence(player,sentence,npc);
		}
	}

	private void handleSentence(Player player, Sentence sentence, SpeakerNPC npc) {
		if(TradingUtility.isPlayerWithinOfferLimit(player)) {
			if (sentence.getExpressions().size() < 3 || sentence.getNumeralCount() != 1) {
				npc.say("I did not understand you. Please say \"sell item price\".");
				npc.setCurrentState(ConversationStates.ATTENDING);
				return;
			}
			String itemName = determineItemName(sentence);
			int price = determinePrice(sentence);
			Integer fee = Integer.valueOf(TradingUtility.calculateFee(player, price).intValue());
			if(TradingUtility.canPlayerAffordTradingFee(player, price)) {
				if (createOffer(player, itemName, price)) {
					TradingUtility.substractTradingFee(player, price);
					npc.say("I added your offer to the trading center and took the fee of "+fee.toString()+".");
					npc.setCurrentState(ConversationStates.ATTENDING);
					return;
				}
				// Needs some feedback for the player
				return;
			}
			npc.say("You cannot afford the trading fee of "+fee.toString());
			return;
		}
		npc.say("You may not place more than "+Integer.valueOf(TradingUtility.MAX_NUMBER_OFF_OFFERS).toString()+" offers.");
	}
	
	/**
	 * Try creating an offer.
	 * 
	 * @param player the player who makes the offer
	 * @param itemName name of the item for sale
	 * @param price price for the item
	 * @return true if making the offer was successful, false otherwise
	 */
	private boolean createOffer(Player player, String itemName, int price) {
		Market shop = TradeCenterZoneConfigurator.getShopFromZone(player.getZone());
		if(shop != null) {
			Offer o = shop.createOffer(player,itemName,Integer.valueOf(price));
			if (o == null) {
				return false;
			}
			TradingUtility.addTurnNotifiers(player, o);
			StringBuilder message = new StringBuilder("Offer for ");
			message.append(itemName);
			message.append(" at ");
			message.append(price);
			message.append(" created. ");
			String messageNumberOfOffers = "You have now made "
				+ Grammar.quantityplnoun(Integer.valueOf(shop.countOffersOfPlayer(player)),"offer") + ".";
			player.sendPrivateText(message.toString() + messageNumberOfOffers);
			return true;
		}
		return false;
	}

	private String determineItemName(Sentence sentence) {
		Expression expression = sentence.getExpression(1,"");
		return expression.getNormalized();
	}

	private int determinePrice(Sentence sentence) {
		return sentence.getNumeral().getAmount();
	}
}
