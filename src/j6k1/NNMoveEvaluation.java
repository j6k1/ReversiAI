package j6k1;

import java.util.Optional;

import xyz.hotchpotch.reversi.core.Point;

public class NNMoveEvaluation {
	public final int[] input;
	public final double score;
	public final Optional<Point> move;

	public NNMoveEvaluation(int[] input, double score, Optional<Point> move)
	{
		this.input = input;
		this.score = score;
		this.move = move;
	}

	public NNMoveEvaluation(double score)
	{
		this.input = null;
		this.score = score;
		this.move = Optional.empty();
	}
}
