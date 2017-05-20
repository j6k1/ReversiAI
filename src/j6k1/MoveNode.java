package j6k1;

import java.util.Optional;

import xyz.hotchpotch.reversi.aiplayers.AIPlayerUtil;
import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;

public class MoveNode {
	protected final Optional<MoveNode> parent;
	protected final int ply;
	protected final int pscore;
	protected final int oscore;
	protected final Optional<Point> move;
	protected static final ICalcPriority[] calculatorPair = new ICalcPriority[] {
		(p, o) -> -(p - o),
		(p, o) -> p - o
	};
	public final Board board;
	public final Color player;
	public final Color opponent;
	public final long priority;

	protected MoveNode(final Optional<MoveNode> parent, final Board board, final int ply,
						final Color player, final Color opponent,
						final Optional<Point> move, final int pscore, final int oscore, final long priority)
	{
		this.parent = parent;
		this.board = board;
		this.ply = ply;
		this.player = player;
		this.opponent = opponent;
		this.pscore = pscore;
		this.oscore = oscore;
		this.priority = priority;
		this.move = move;
	}

	public static MoveNode of(Board board, Color player, Color opponent)
	{
		return new MoveNode(Optional.empty(),
							board, 0, player, opponent,
										Optional.empty(), 0, 0, Integer.MAX_VALUE);
	}

	public MoveNode createOpposite(Point p)
	{
		Move m = Move.of(player, p);

		Board next = new AIPlayerUtil.LightweightBoard(board);

		next.apply(m);

		int addscore =  + Rule.reversibles(board, m).size();

		return new MoveNode(Optional.of(this), next, ply + 1,
							opponent, player, Optional.of(p),
							oscore, pscore + addscore,
							calculatorPair[(ply + 1) % 2].apply(pscore + addscore, oscore));
	}

	public Optional<Point> firstMove()
	{
		MoveNode current = this;

		while(current.ply > 1 && current.parent.isPresent())
		{
			current = current.parent.get();
		}

		return current.move;
	}
}
