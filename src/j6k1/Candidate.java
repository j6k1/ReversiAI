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

	public Candidate(ArrayList<Candidate> candidate, Color player, Board board, Point move)
	{
		this.candidates = candidate;
		this.node = new GameNode(this, Optional.empty(), player, board, Optional.of(move));
		this.candidates.add(this);
	}

	public void update(GameNode node)
	{
		this.node = node;
	}

	public GameNode getNode()
	{
		return this.node;
	}

	public void playout(Random rnd, Instant deadline)
	{
		if(!this.node.endNode)
		{
			this.node.playout(rnd, deadline);
		}
	}
}
