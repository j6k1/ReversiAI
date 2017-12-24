package j6k1;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;
import xyz.hotchpotch.reversi.framework.GameCondition;

public class CornerTopPriorityNNAIPlayer extends BaseNNAIPlayer {
	public CornerTopPriorityNNAIPlayer(Color color, GameCondition gameCondition) {
		super(color, gameCondition, (self, player, board, st) -> {
			return st.sorted((a,b) -> {
				NNMoveEvaluation ma = self.evalute(board, player, Optional.of(a));
				NNMoveEvaluation mb = self.evalute(board, player, Optional.of(b));

				boolean isca = ((a.i == 0 && a.j == 0) || (a.i == 0 && a.j == 7) ||
								(a.i == 7 && a.j == 0) || (a.i == 7 && a.j == 7));
				boolean iscb = ((b.i == 0 && b.j == 0) || (b.i == 0 && b.j == 7) ||
								(b.i == 7 && b.j == 0) || (b.i == 7 && b.j == 7));

				if(isca && !iscb) return -1;
				else if(!isca && iscb) return 1;
				else return ma.score == mb.score ? 0 : ma.score > mb.score ? -1 : 1;
			}).iterator();
		});
	}

	protected Optional<Point> think(Board board,final Color player) {
		Iterator<Point> it = handIteratorFactory.apply(this, player, board, Arrays.stream(points)
				.filter(p -> Rule.canPutAt(board, player, p)));

		history.add(new NNInput(evalute(board, player.opposite(), Optional.empty()).input));
		NNMoveEvaluation passMove = evalute(board, player, Optional.empty());

		if(!it.hasNext())
		{
			history.add(new NNInput(passMove.input));
			return passMove.move;
		}
		else
		{
			Point p = it.next();

			NNMoveEvaluation me = evalute(board, player, Optional.of(p));

			history.add(new NNInput(me.input));
			return me.move;
		}
	}
}
