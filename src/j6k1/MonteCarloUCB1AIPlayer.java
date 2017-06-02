package j6k1;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.framework.Player;

public class MonteCarloUCB1AIPlayer implements Player {
	private Random rnd;
	protected static final long marginT = 300L;
	protected Comparator<GameNode> candidateComparator = (a,b) -> {

		if(a.win > b.win) return 1;
		else if(a.win < b.win) return -1;
		else if(a.loss < b.loss) return 1;
		else if(a.loss > b.loss) return -1;
		else return 0;
	};


	public MonteCarloUCB1AIPlayer()
	{
		rnd = new Random();
	}

	@Override
	public Point decide(Board board, Color color, long givenMillisPerTurn, long remainingMillisInGame) {
		long limit = Math.min(givenMillisPerTurn,
				remainingMillisInGame / (Arrays.stream(GameNode.points)
										.filter(p -> board.colorAt(p) == null)
										.count() + 1) / 2);
		try {
			return search(board, color, Instant.now().plusMillis(Math.max(0, limit - marginT))).orElse(null);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public Optional<Point> search(Board board, Color player, Instant deadline)
	{
		GameNode rootNode = new GameNode(player, board);

		if(!Instant.now().isBefore(deadline))
		{
			if(rootNode.children.size() == 0) return Optional.empty();
			else return rootNode.children.get(rnd.nextInt(rootNode.children.size())).move;
		}
		else
		{
			while(Instant.now().isBefore(deadline))
			{
				if(!Instant.now().isBefore(deadline)) break;
				rootNode.playout(rnd, deadline);
			}

			if(rootNode.children.size() == 0) return Optional.empty();
			else return Collections.max(rootNode.children, candidateComparator).move;
		}
	}
}
