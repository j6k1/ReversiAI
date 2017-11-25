package j6k1;

import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import xyz.hotchpotch.reversi.aiplayers.AIPlayerUtil;
import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;
import xyz.hotchpotch.reversi.framework.GameCondition;
import xyz.hotchpotch.reversi.framework.Player;

public abstract class BaseABAIPlayer implements Player {
	protected static Point[] points = new Point[64];
	protected static final double cost = 4.6;
	protected static final long marginT = 50L;
	protected BiFunction<Board, Stream<Point>, Iterator<Point>> handIteratorFactory;

	static {
		points[0] = Point.of(0,0);
		points[1] = Point.of(0,7);
		points[2] = Point.of(7,0);
		points[3] = Point.of(7,7);

		for(int i=4, j=1; j < 7; i++, j++) points[i] = Point.of(0, j);
		for(int i=10, j=1; j < 7; i++, j++) points[i] = Point.of(j, 0);
		for(int i=16, j=1; j < 7; i++, j++) points[i] = Point.of(j, 7);
		for(int i=22, j=1; j < 7; i++, j++) points[i] = Point.of(7, j);
		for(int i=28, j=1; j < 7; j++) for(int k=1; k < 7; i++, k++) points[i] = Point.of(j, k);
	}

	public BaseABAIPlayer(Color color, GameCondition gameCondition)
	{
		handIteratorFactory = null;
	}

	@Override
	public Point decide(Board board, Color color, long givenMillisPerTurn, long remainingMillisInGame) {
		try {
			long limit = Math.min(givenMillisPerTurn,
					remainingMillisInGame / (Arrays.stream(points)
											.filter(p -> board.colorAt(p) == null)
											.count() + 1) / 2) * 1000;
			int ply = calcDepth(limit, 1.0, 1);

			return alphabeta(board, ply, color, color.opposite(),
							MoveEvaluation.Minimum, MoveEvaluation.Maximum,
							0, 0, Instant.now().plusMillis(Math.max(0, limit / 1000 - marginT))).move.orElse(null);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	protected int calcDepth(long limit, double time, int depth)
	{
		assert limit >= 0;
		assert time > 0.0;
		assert depth > 0;

		if((double)limit < time) return depth - 1;
		else return calcDepth(limit, time * cost, depth + 1);
	}

	protected MoveEvaluation alphabeta(final Board board, final int ply,
												final Color player,
												final Color opponent,
												long alpha, final long beta,
												final int pscore, final int oscore, final Instant deadLine)
	{
		Iterator<Point> it = handIteratorFactory.apply(board, Arrays.stream(points)
								.filter(p -> Rule.canPutAt(board, player, p)));

		if(!Instant.now().isBefore(deadLine) || ply == 0 || !it.hasNext())
		{
			return new MoveEvaluation(pscore - oscore);
		}

		MoveEvaluation best = new MoveEvaluation(alpha);

		while(it.hasNext())
		{
			Point p = it.next();
			Move m = Move.of(player, p);

			Board next = new AIPlayerUtil.LightweightBoard(board);

			next.apply(m);

			MoveEvaluation me = alphabeta(next, ply - 1,
											opponent, player,
												-beta, -alpha,
												oscore, pscore + Rule.reversibles(board, m).size(), deadLine);

			if(-me.score > alpha)
			{
				alpha = -me.score;
				best = new MoveEvaluation(p, alpha);
			}

			if(alpha >= beta || !Instant.now().isBefore(deadLine)) return best;
		}

		return best;
	}
}
