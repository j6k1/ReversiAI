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
	protected final Point[] nextPoints;
	protected Comparator<GameNode> ucb1Comparator = (a,b) -> {
		double ucb1A = a.endNode ? -1.0 : a.applyUcb1();
		double ucb1B = b.endNode ? -1.0 : b.applyUcb1();

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

	protected final ArrayList<GameNode> children;
	protected final GameNode[] childrenMap;
	protected final Optional<GameNode> parent;
	protected long nodeCount;
	protected int visitedCount;
	protected long win;
	protected long loss;
	protected boolean isExpand;
	protected Optional<GameNode> lastExpandNode;
	public boolean endNode;
	protected final Color player;
	protected final Board board;
	protected final Optional<Point> move;

	public GameNode(Color player, Board board)
	{
		this(Optional.empty(), player, board, Optional.empty(), true);
	}

	protected GameNode(Optional<GameNode> parent, Color player,
						Board board, Optional<Point> move, boolean isExpand)
	{
		this.parent = parent;
		this.player = player;
		this.move = move;
		this.children = new ArrayList<>();
		this.childrenMap = new GameNode[65];
		this.nodeCount = 0;
		this.visitedCount = 0;
		this.win = 0;
		this.loss = 0;

		this.board = new AIPlayerUtil.LightweightBoard(board);

		this.move.ifPresent(p -> this.board.apply(Move.of(player, p)));

		this.nextPoints = Arrays.stream(points)
						.filter(p -> Rule.canPutAt(board, player, p)).toArray(Point[]::new);

		this.isExpand = isExpand;
		this.lastExpandNode = Optional.empty();
		this.endNode = judgment();
	}

	protected boolean judgment()
	{
		if(Rule.isGameOngoing(board))
		{
			return false;
		}
		else if(Rule.winner(board) == null)
		{
			return true;
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

	public boolean playout(Random rnd, Instant deadline)
	{
		if(!Instant.now().isBefore(deadline)) return false;

		visitedCount++;

		if(visitedCount == NumberOfNodesThreshold + 1 && nextPoints.length > 0)
		{
			visitedCount = 0;
			lastExpandNode.ifPresent(n -> {
				n.isExpand = false;
			});
			GameNode node = Collections.max(children, ucb1Comparator);
			node.isExpand = true;
			if(!lastExpandNode.filter(n -> n == node).isPresent())
			{
				lastExpandNode = Optional.of(node);
			}
			if(!Instant.now().isBefore(deadline)) return false;
		}

		if(!isExpand)
		{
			Optional<Point> p;

			if(nextPoints.length > 0)
			{
				p = Optional.of(nextPoints[rnd.nextInt(nextPoints.length)]);
			}
			else
			{
				p = Optional.empty();
			}

			GameNode node = null;

			int k = p.map(pt -> pt.i * 8 + pt.j).orElse(64);
			boolean newNode = true;

			if(childrenMap[k] == null)
			{
				node = new GameNode(Optional.of(this), player.opposite(), board, p, false);
				children.add(node);
				childrenMap[k] = node;
			}
			else
			{
				node = childrenMap[k];
				newNode = false;
			}

			if(!node.endNode) if(!node.playout(rnd, deadline)) return false;
			else if(newNode) onAddNode();

			if(!Instant.now().isBefore(deadline)) return false;
		}
		else if(nextPoints.length > 0)
		{
			for(Point p: nextPoints)
			{

				int k = p.i * 8 + p.j;

				boolean newNode = true;

				GameNode node = null;

				if(childrenMap[k] == null)
				{
					node = new GameNode(Optional.of(this), player.opposite(), board, Optional.of(p), false);
					children.add(node);
					childrenMap[k] = node;
				}
				else
				{
					node = childrenMap[k];
					newNode = false;
				}

				if(!node.endNode) if(!node.playout(rnd, deadline)) return false;
				else if(newNode) onAddNode();

				if(!Instant.now().isBefore(deadline)) return false;
			}
		}
		else
		{
			int k = 64;

			boolean newNode = true;

			GameNode node = null;

			if(childrenMap[k] == null)
			{
				node = new GameNode(Optional.of(this), player.opposite(), board, Optional.empty(), false);
				children.add(node);
				childrenMap[k] = node;
			}
			else
			{
				node = childrenMap[k];
				newNode = false;
			}

			if(!node.endNode) if(!node.playout(rnd, deadline)) return false;
			else if(newNode) onAddNode();

			if(!Instant.now().isBefore(deadline)) return false;
		}

		return true;
	}

	protected double applyUcb1()
	{
		return (double)win / (double)nodeCount +
				(Math.log(10.0) / Math.log(2.0)) *
				getRoot()
					.children.stream().mapToLong(n -> n.nodeCount).sum() / (double)nodeCount;
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
