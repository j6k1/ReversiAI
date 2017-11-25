package j6k1;

import java.util.List;
import java.util.stream.Collectors;

import bin.j6k1.BaseABAIPlayer;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;
import xyz.hotchpotch.reversi.framework.GameCondition;

public class SortedHandABAIPlayer extends BaseABAIPlayer {
	public SortedHandABAIPlayer(Color color, GameCondition gameCondition)
	{
		super(color, gameCondition);
		handIteratorFactory = (board, st) -> {
			final List<Point> points = st.sorted((a,b) -> {
				Move ma = Move.of(color, a);
				Move mb = Move.of(color, b);

				int sa = Rule.reversibles(board, ma).size();
				int sb = Rule.reversibles(board, mb).size();

				return (sa == sb) ? 0 : sa > sb ? -1 : 1;
			}).collect(Collectors.toList());

			return points.iterator();
		};
	}
}
