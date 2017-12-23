package j6k1;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.Random;

import j6k1.ai.nn.FReLU;
import j6k1.ai.nn.FTanh;
import j6k1.ai.nn.NN;
import j6k1.ai.nn.NNUnit;
import j6k1.ai.nn.PersistenceWithTextFile;
import j6k1.ai.nn.TextFileInputReader;
import xyz.hotchpotch.reversi.aiplayers.AIPlayerUtil;
import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Move;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.core.Rule;
import xyz.hotchpotch.reversi.framework.GameCondition;
import xyz.hotchpotch.reversi.framework.GameResult;
import xyz.hotchpotch.reversi.framework.Player;

public class NNAIPlayer implements Player {
	protected static Point[] points = new Point[64];
	protected Color self;
	protected NN nn;
	protected final static int unitWidth = 192 * 3;
	protected final static int nnLayerDepth = 4;
	protected int[][] history;
	protected int historyCount;

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

	public NNAIPlayer(Color color, GameCondition gameCondition)
	{
		self = color;
		history = new int[32][];
		historyCount = 0;

		try {
			nn = new NN(new NNUnit[] {
				new NNUnit(192),
				new NNUnit(unitWidth, new FReLU()),
				new NNUnit(unitWidth, new FReLU()),
				new NNUnit(1, new FTanh()),
			}, new TextFileInputReader(new File("data/nn.txt")), () -> {
				double[][][] layers = new double[nnLayerDepth-1][][];

				Random r = new Random();

				layers[0] = new double[193][];

				layers[0][0] = new double[unitWidth];

				for(int k=0; k < unitWidth; k++)
				{
					layers[0][0][k] = 0.0;
				}

				for(int j=1; j < 193; j++)
				{
					layers[0][j] = new double[unitWidth];

					for(int k=0; k < unitWidth; k++)
					{
						layers[0][j][k] = r.nextDouble() * (r.nextInt(2) == 1 ? 1.0 : -1.0);
					}
				}

				for(int i=1; i < nnLayerDepth-2; i++)
				{
					layers[i] = new double[unitWidth+1][];

					layers[i][0] = new double[unitWidth];

					for(int k=0; k < unitWidth; k++)
					{
						layers[i][0][k] = 0.0;
					}

					for(int j=1; j < unitWidth+1; j++)
					{
						layers[i][j] = new double[unitWidth];

						for(int k=0; k < unitWidth; k++)
						{
							layers[i][j][k] = r.nextDouble() * (r.nextInt(2) == 1 ? 1.0 : -1.0);
						}
					}
				}

				layers[nnLayerDepth-2] = new double[unitWidth+1][];

				layers[nnLayerDepth-2][0] = new double[1];

				for(int k=0; k < 1; k++)
				{
					layers[nnLayerDepth-2][0][k] = 0.0;
				}

				for(int j=1; j < unitWidth+1; j++)
				{
					layers[nnLayerDepth-2][j] = new double[1];

					for(int k=0; k < 1; k++)
					{
						layers[nnLayerDepth-2][j][k] = r.nextDouble() * (r.nextInt(2) == 1 ? 1.0 : -1.0);
					}
				}

				return layers;
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Point decide(Board board, Color color, long givenMillisPerTurn, long remainingMillisInGame) {
		try {
			return think(board, color);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private Point think(Board board, Color player) {
		Iterator<Point> it = Arrays.stream(points)
								.filter(p -> Rule.canPutAt(board, player, p)).iterator();

		NNMoveEvalution bestMove = null;
		double bestscore = -Double.MAX_VALUE;

		NNMoveEvalution passEvalution = evalute(board, player, Optional.empty());

		while(it.hasNext())
		{
			Point p = it.next();

			NNMoveEvalution e = evalute(board, player, Optional.of(p));

			if(e.score >= bestscore || bestMove == null)
			{
				bestscore = e.score;
				bestMove = e;
			}
		}

		if(bestMove == null) history[historyCount++] = passEvalution.input;
		else history[historyCount++] = bestMove.input;

		return bestMove == null ? null : bestMove.move.orElse(null);
	}

	public void notifyOfResult(GameResult result)
	{
		double[] t = result.winner == self ? new double[] { 1.0 } : new double[] { 0.0 };

		for(int i=historyCount - 1; i >= 0; i--)
		{
			nn = nn.learn(history[i], t, 0.5);
		}

		(new PersistenceWithTextFile(new File("data/nn.txt"))).save(nn);
	}

	private NNMoveEvalution evalute(Board board, Color player, Optional<Point> move)
	{
		int[] input = new int[192];

		Board next = move.map(p ->{
			Move m = Move.of(player, p);
			Board n = new AIPlayerUtil.LightweightBoard(board);
			n.apply(m);
			return n;
		}).orElse(board);

		int kind = 0;

		int[][] map = mapOfBlank(next);

		for(int i=0; i < 8; i++)
		{
			for(int j=0; j < 8; j++)
			{
				input[kind * 64 + i * 8 + j] = map[i][j];
			}
		}

		++kind;

		map = mapOfColor(next, player);

		for(int i=0; i < 8; i++)
		{
			for(int j=0; j < 8; j++)
			{
				input[kind * 64 + i * 8 + j] = map[i][j];
			}
		}

		++kind;

		map = mapOfColor(next, player.opposite());

		for(int i=0; i < 8; i++)
		{
			for(int j=0; j < 8; j++)
			{
				input[kind * 64 + i * 8 + j] = map[i][j];
			}
		}

		double[] r = nn.solve(input);

		double score = r[0] * 100000000;

		return new NNMoveEvalution(input, score, move);
	}

	private int[][] mapOfBlank(Board board)
	{
		int[][] r = new int[8][];

		for(int i=0, l=r.length; i < l; i++) {
			r[i] = new int[8];
		}

		for(int i=0; i < 8; i++)
		{
			for(int j=0; j < 8; j++)
			{
				r[i][j] = board.colorAt(Point.of(i, j)) == null ? 1 : 0;
			}
		}

		return r;
	}

	private int[][] mapOfColor(Board board, Color color)
	{
		int[][] r = new int[8][];

		for(int i=0, l=r.length; i < l; i++) {
			r[i] = new int[8];
		}

		for(int i=0; i < 8; i++)
		{
			for(int j=0; j < 8; j++)
			{
				r[i][j] = board.colorAt(Point.of(i, j)) == color ? 1 : 0;
			}
		}

		return r;
	}
}
