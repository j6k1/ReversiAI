package j6k1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

import xyz.hotchpotch.reversi.aiplayers.AIPlayerUtil;
import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;

public class GameNode implements IOnWon, IOnLost, IOnAddNode, IOnNodeTerminated {
	public final static Point[] points = new Point[64];
	protected final ArrayList<Point> nextPoints;
	protected int candidateCount;
	protected boolean pass;
	protected Comparator<GameNode> ucb1Comparator = (a,b) -> {
		double ucb1A = (a.endNode || a.candidateCount <= a.tryCount) ? -1.0 : a.applyUcb1();
		double ucb1B = (b.endNode || b.candidateCount <= b.tryCount) ? -1.0 : b.applyUcb1();

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
	protected final GameNode root;
	protected long nodeCount;
	protected int visitedCount;
	protected int tryCount;
	protected long win;
	protected long loss;
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
		this.tryCount = 1;
		this.win = 0;
		this.loss = 0;
		this.root = getRoot();

		this.board = new AIPlayerUtil.LightweightBoard(board);

		this.move.ifPresent(p -> this.board.apply(Move.of(player.opposite(), p)));

		Iterator<Point> it = Arrays.stream(points)
								.filter(p -> Rule.canPutAt(board, player, p)).iterator();

		this.nextPoints = new ArrayList<>();

		while(it.hasNext()) this.nextPoints.add(it.next());

		this.pass = this.nextPoints.size() == 0;

		this.candidateCount = !this.pass ? this.nextPoints.size() : 1;

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

	public boolean playout(Random rnd, Instant deadline, int numberOfNodesThreshold)
	{
		if(!Instant.now().isBefore(deadline)) return false;

		if(visitedCount > 0 && visitedCount % numberOfNodesThreshold  == 0 &&
				nextPoints.size() > 0 && this.player == this.root.player)
		{
			GameNode node = Collections.max(children, ucb1Comparator);

			if(node.tryCount < node.nextPoints.size()) ++node.tryCount;
			if(!Instant.now().isBefore(deadline)) return false;
		}

		++visitedCount;

		if(nextPoints.size() > 0 && (this == root || tryCount >= nextPoints.size()))
		{
			for(int i=0, l=nextPoints.size(); i < l; i++)
			{
				Optional<Point> p;

				p = Optional.of(nextPoints.get(i));

				GameNode node = null;

				int k = p.map(pt -> pt.i * 8 + pt.j).orElse(64);

				if(childrenMap[k] == null)
				{
					node = new GameNode(Optional.of(this), player.opposite(), board, p, false);
					children.add(node);
					childrenMap[k] = node;
				}
				else
				{
					node = childrenMap[k];
				}

				if(!node.endNode)
				{
					if(!node.playout(rnd, deadline, numberOfNodesThreshold)) return false;
				}
				else node.onAddNode();

				if(node.endNode)
				{
					if(node.onNodeTerminated(i) == 0) this.endNode = true;
					--i;
					--l;
				}

				if(!Instant.now().isBefore(deadline)) return false;
			}
		}
		else if(nextPoints.size() > 0)
		{
			for(int i=0; i < tryCount; i++)
			{
				Optional<Point> p;

				final int moveIndex = rnd.nextInt(nextPoints.size());
				p = Optional.of(nextPoints.get(moveIndex));

				GameNode node = null;

				int k = p.map(pt -> pt.i * 8 + pt.j).orElse(64);

				if(childrenMap[k] == null)
				{
					node = new GameNode(Optional.of(this), player.opposite(), board, p, false);
					children.add(node);
					childrenMap[k] = node;
				}
				else
				{
					node = childrenMap[k];
				}

				if(!node.endNode)
				{
					if(!node.playout(rnd, deadline, numberOfNodesThreshold)) return false;
				}
				else node.onAddNode();

				if(node.endNode && node.onNodeTerminated(moveIndex) == 0) this.endNode = true;

				if(!Instant.now().isBefore(deadline)) return false;

				if(nextPoints.size() == 0) break;
			}
		}
		else if(pass && candidateCount == 1)
		{
			GameNode node = null;

			int k = 64;

			boolean newNode = true;

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

			if(!node.endNode)
			{
				if(!node.playout(rnd, deadline, numberOfNodesThreshold)) return false;
			}
			else if(newNode) node.onAddNode();

			if(node.endNode && node.onNodeTerminated(-1) == 0) this.endNode = true;

			if(!Instant.now().isBefore(deadline)) return false;
		}

		return true;
	}

	protected double applyUcb1()
	{
		// 計算しなければならないのはchildrenの各ノードに対して打ったプレイヤー、つまりchildrenの各ノードのplayerの相手なので、
		// winではなくlossを使って計算するのが正しい。
		return (double)loss / (double)nodeCount +
				Math.sqrt(2.0 * Math.log(
						parent.map(p ->	p.nodeCount).orElse(nodeCount)) / (double)nodeCount) * 0.5;
	}

	public GameNode getRoot()
	{
		if(!parent.isPresent()) return this;
		else return parent.map(p -> p.getRoot()).get();
	}

	public boolean searchEnded()
	{
		return candidateCount == 0;
	}

	@Override
	public void onAddNode() {
		this.nodeCount++;
		parent.ifPresent(p -> p.onAddNode());
	}

	@Override
	public int onNodeTerminated(int index) {
		if(index != -1) parent.ifPresent(p -> p.nextPoints.remove(index));

		return parent.map(p -> {
			p.candidateCount--;
			return p.candidateCount;
		}).orElse(0);
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
