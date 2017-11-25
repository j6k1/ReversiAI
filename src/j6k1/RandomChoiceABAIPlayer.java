package j6k1;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.framework.GameCondition;

public class RandomChoiceABAIPlayer extends BaseABAIPlayer {
	public RandomChoiceABAIPlayer(Color color, GameCondition gameCondition)
	{
		super(color, gameCondition);

		handIteratorFactory = (board, st) -> {
			final List<Point> points = st.collect(Collectors.toList());
			final Random rnd = new Random();;

			return new Iterator<Point>() {
				@Override
				public boolean hasNext() {
					return !points.isEmpty();
				}

				@Override
				public Point next() {
					int i = rnd.nextInt(points.size());

					Point p = points.get(i);

					points.remove(i);

					return p;
				}
			};
		};
	}
}
