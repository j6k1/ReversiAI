package j6k1;

import java.util.Optional;

import xyz.hotchpotch.reversi.core.Point;

public class MoveEvaluation {
	public final Optional<Point> move;
	public final long score;

	public static final long Minimum = -Integer.MAX_VALUE;
	public static final long Maximum = Integer.MAX_VALUE;

	public MoveEvaluation(Point move, long score)
	{
		this.move = Optional.of(move);
		this.score = score;
	}

	public MoveEvaluation(long score)
	{
		this.move = Optional.empty();
		this.score = score;
	}
}
