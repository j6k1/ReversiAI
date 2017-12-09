package j6k1;

import java.util.Iterator;
import java.util.Set;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;

public class ABScoreEvalutor {
	public static int evalute(Board board, Move move)
	{
		Set<Point> points = Rule.reversibles(board, move);
		int score = points.size();

		Point p = move.point;

		if( (p.i == 0 && p.j == 0) ||
			(p.i == Point.HEIGHT - 1 && p.j == Point.WIDTH - 1) ||
			(p.i == 0 && p.j == Point.WIDTH - 1) ||
			(p.i == Point.HEIGHT - 1 && p.j == 0)) {
			return score + 40;
		}
		else if((board.colorAt(Point.of(0, 0)) == null &&
					(p.i == 1 && p.j == 1) || (p.i == 1 && p.j == 0) || (p.i == 0 && p.j == 1)) ||
				(board.colorAt(Point.of(Point.HEIGHT - 1, 0)) == null &&
					(p.i == Point.HEIGHT - 2 && p.j == 1) || (p.i == Point.HEIGHT - 2 && p.j == 0) ||
					(p.i == Point.HEIGHT - 1 && p.j == 1)) ||
				(board.colorAt(Point.of(0, Point.WIDTH - 1)) == null &&
					(p.j == Point.WIDTH - 2 && p.i == 1) || (p.j == Point.WIDTH - 2 && p.i == 0) ||
					(p.j == Point.WIDTH - 1 && p.i == 1)) ||
				(board.colorAt(Point.of(Point.HEIGHT - 1, Point.WIDTH - 1)) == null &&
					(p.j == Point.WIDTH - 2 && p.i == Point.HEIGHT - 2) ||
					(p.j == Point.WIDTH - 1 && p.i == Point.HEIGHT - 2) ||
					(p.j == Point.WIDTH - 2 && p.i == Point.HEIGHT - 1)))
		{
			return score - 20;
		}
		else if(p.i == 0 || p.i == Point.HEIGHT - 1)
		{
			Iterator<Point> it = points.iterator();
			return score + 10 + (it.hasNext() && it.next().i == p.i ? points.size() * 5 : 0);
		}
		else if(p.j == 0 || p.j == Point.WIDTH - 1)
		{
			Iterator<Point> it = points.iterator();
			return score + 10 + (it.hasNext() && it.next().j == p.j ? points.size() * 5 : 0);
		}
		else
		{
			return score;
		}
	}
}
