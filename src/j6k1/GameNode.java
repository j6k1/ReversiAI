package j6k1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

import xyz.hotchpotch.reversi.aiplayers.AIPlayerUtil;
import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;

public class GameNode implements IOnWon, IOnLost, IOnAddNode {
	public final static int NumberOfNodesThreshold = 10;
	public final static Point[] points = new Point[64];
	protected Comparator<GameNode> ucb1Comparator = (a,b) -> {
		double ucb1A = a.applyUcb1();
		double ucb1B = b.applyUcb1();

		return ucb1A < ucb1B ? -1 : ucb1A > ucb1B ? 1 : 0;
	};

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

	protected final Candidate owner;
	protected final ArrayList<GameNode> children;
	protected final Optional<GameNode> parent;
	protected long nodeCount;
	protected long win;
	protected long loss;
	public boolean endNode;
	protected final Color player;
	protected final Board board;
	protected final Optional<Point> move;

	public GameNode(Candidate owner, Optional<GameNode> parent, Color player, Board board, Optional<Point> move)
	{
		this.owner = owner;
		this.parent = parent;
		this.player = player;
		this.move = move;
		this.children = new ArrayList<>();
		this.nodeCount = 0;
		this.win = 0;
		this.loss = 0;

		this.board = new AIPlayerUtil.LightweightBoard(board);

		this.move.ifPresent(p -> this.board.apply(Move.of(player, p)));

		endNode = judgment();
	}

	protected boolean judgment()
	{
		if(Rule.isGameOngoing(board))
		{
			return false;
		}
		else
		{
			if(Rule.winner(board) == player)
			{
				onWon();
			}
			else
			{
				onLost();
			}

			return true;
		}
	}

	public void playout(Random rnd, Instant deadline)
	{
		if(!Instant.now().isBefore(deadline)) return;

		Point[] validPoints = Arrays.stream(points)
				.filter(p -> Rule.canPutAt(board, player, p)).toArray(Point[]::new);

		assert validPoints.length > 0;

		Optional<Point> p;

		if(validPoints.length > 0)
		{
			p = Optional.of(validPoints[rnd.nextInt(validPoints.length)]);
		}
		else
		{
			p = Optional.empty();
		}

		GameNode node = new GameNode(owner, Optional.of(this), player.opposite(), board, p);
		children.add(node);

		onAddNode();

		if(!node.endNode) node.playout(rnd, deadline);

		if(!Instant.now().isBefore(deadline)) return;

		assert nodeCount <= 10;

		if(nodeCount == NumberOfNodesThreshold)
		{
			this.owner.update(Collections.max(children, ucb1Comparator));
		}
	}

	protected double applyUcb1()
	{
		return (double)win / (double)nodeCount +
				(Math.log(10.0) / Math.log(2.0)) *
				(double)owner
					.candidates.stream().mapToLong(c -> c.getNode().nodeCount).sum() / (double)nodeCount;
	}

	public GameNode getRoot()
	{
		if(!parent.isPresent()) return this;
		else return parent.map(p -> p.getRoot()).get();
	}

	@Override
	public void onAddNode() {
		this.nodeCount++;
		parent.ifPresent(p -> p.onAddNode());
	}
	@Override
	public void onLost() {
		loss++;
		parent.ifPresent(p -> p.onWon());
	}

	@Override
	public void onWon() {
		win++;
		parent.ifPresent(p -> p.onLost());
	}
}
