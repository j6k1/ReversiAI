package j6k1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;
import xyz.hotchpotch.reversi.framework.Player;

public class MonteCarloUCB1AIPlayer implements Player {
	private Random rnd;
	protected static final long marginT = 100L;
	protected Comparator<Candidate> candidateComparator = (a,b) -> {
		GameNode aNode = a.getRoot();
		GameNode bNode = b.getRoot();

		if(aNode.win > bNode.win) return 1;
		else if(aNode.win < bNode.win) return -1;
		else if(aNode.loss < bNode.loss) return 1;
		else if(aNode.loss > bNode.loss) return -1;
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
		ArrayList<Candidate> candidates = new ArrayList<>();

		Iterator<Point> it = Arrays.stream(GameNode.points)
				.filter(p -> Rule.canPutAt(board, player, p)).iterator();

		if(!it.hasNext()) return Optional.empty();

		while(it.hasNext())
		{
			Point p = it.next();

			new Candidate(candidates, player, board, p);
		}

		while(Instant.now().isBefore(deadline))
		{
			for(Candidate c: candidates)
			{
				if(!Instant.now().isBefore(deadline)) break;
				c.playout(rnd, deadline);
			}
		}

		return Collections.max(candidates, candidateComparator).getRoot().move;
	}
}
