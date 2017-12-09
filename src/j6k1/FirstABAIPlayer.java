package j6k1;

import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.framework.GameCondition;

public class FirstABAIPlayer extends BaseABAIPlayer {
	public FirstABAIPlayer(Color color, GameCondition gameCondition)
	{
		super(color, gameCondition);

		handIteratorFactory = (player, board, st) -> st.iterator();
	}
}
