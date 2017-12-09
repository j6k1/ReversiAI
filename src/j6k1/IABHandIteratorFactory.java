package j6k1;

import java.util.Iterator;
import java.util.stream.Stream;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;

@FunctionalInterface
public interface IABHandIteratorFactory {
	public Iterator<Point> apply(Color player, Board board, Stream<Point> st);
}
