package j6k1;

import java.util.Arrays;
import java.util.Iterator;
import java.util.PriorityQueue;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;
import xyz.hotchpotch.reversi.framework.Player;

public class PrioritiySearchAIPlayer implements Player {
	protected static Point[] points = new Point[64];
	protected static final long marginT = 10L;

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


	@Override
	public Point decide(Board board, Color color, long givenMillisPerTurn, long remainingMillisInGame) {
		try {
			long limit = Math.min(givenMillisPerTurn,
					remainingMillisInGame / (Arrays.stream(points)
											.filter(p -> board.colorAt(p) == null)
											.count() + 1) / 2);
			return search(board, color, color.opposite(), System.currentTimeMillis() + limit);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	protected Point search(Board board, Color player, Color opponent, long endTime)
	{
		MoveNode best = MoveNode.of(board, player, opponent);

		PriorityQueue<MoveNode> q = new PriorityQueue<MoveNode>((a,b) -> {
			if(a.priority > b.priority) return -1;
			else if(a.priority < b.priority) return 1;
			else return 0;
		});

		q.add(best);

		while(!q.isEmpty())
		{
			final MoveNode n = q.poll();
			best = n;

			Iterator<Point> it = Arrays.stream(points)
					.filter(p -> Rule.canPutAt(n.board, n.player, p)).iterator();

			if(endTime - System.currentTimeMillis() < marginT) break;

			while(it.hasNext())
			{
				q.add(n.createOpposite(it.next()));
			}
		}

		return best.firstMove().orElse(null);
	}
}
