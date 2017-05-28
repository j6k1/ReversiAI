package j6k1;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;

public class Candidate {
	public final ArrayList<Candidate> candidates;
	protected GameNode node;
	protected GameNode rootNode;
	protected boolean tryAll;

	public Candidate(ArrayList<Candidate> candidate, Color player, Board board, Point move)
	{
		this.candidates = candidate;
		this.rootNode = this.node = new GameNode(this, Optional.empty(), player, board, Optional.of(move));
		this.tryAll = false;
		this.candidates.add(this);
	}

	public GameNode update(GameNode node)
	{
		this.tryAll = true;
		return (this.node = node);
	}

	public GameNode getNode()
	{
		return this.node;
	}

	public GameNode getRoot()
	{
		return this.rootNode;
	}

	public void playout(Random rnd, Instant deadline)
	{
		if(!this.node.endNode)
		{
			this.node.playout(rnd, deadline, tryAll);
		}
	}
}
