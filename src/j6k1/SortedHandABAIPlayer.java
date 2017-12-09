package j6k1;

import bin.j6k1.BaseABAIPlayer;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.framework.GameCondition;

public class SortedHandABAIPlayer extends BaseABAIPlayer {
	public SortedHandABAIPlayer(Color color, GameCondition gameCondition)
	{
		super(color, gameCondition);
		handIteratorFactory = (player, board, st) -> {
			return st.sorted((a,b) -> {
				Move ma = Move.of(player, a);
				Move mb = Move.of(player, b);

				int sa = ABScoreEvalutor.evalute(board, ma);
				int sb = ABScoreEvalutor.evalute(board, mb);

				return (sa == sb) ? 0 : sa > sb ? -1 : 1;
			}).iterator();
		};
	}
}
