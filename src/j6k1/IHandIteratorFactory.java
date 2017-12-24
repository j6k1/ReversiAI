package j6k1;

import java.util.Iterator;
import java.util.stream.Stream;

import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.framework.Player;

@FunctionalInterface
public interface IHandIteratorFactory<T extends Player> {
	public Iterator<Point> apply(T self, Color player, Board board, Stream<Point> st);
}
