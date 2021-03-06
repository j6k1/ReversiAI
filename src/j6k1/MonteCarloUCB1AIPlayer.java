package j6k1;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

import xyz.hotchpotch.reversi.aiplayers.AIPlayerUtil;
import xyz.hotchpotch.reversi.core.Board;
import xyz.hotchpotch.reversi.core.Color;
import xyz.hotchpotch.reversi.core.Point;
import xyz.hotchpotch.reversi.framework.GameCondition;
import xyz.hotchpotch.reversi.framework.Player;

public class MonteCarloUCB1AIPlayer implements Player {
	private Random rnd;
	protected static final long marginT = 300L;
	protected final boolean debug;
	protected final int numberOfNodesThreshold;
	protected Comparator<GameNode> candidateComparator = (a,b) -> {

		/*
		 * ノードがendNode(全手読み切っている)でかつ全ノードで負けている（相手のノードなので自分の勝利）の場合、
		 * それが優先的に選択されるようにする。
		 * 一手目として生成されたノードのプレイヤーは自分ではなく相手であるため、winではなくlossを使って勝率を計算するのが正しい。
		 * 勝率が同率で敗北率で判定する場合、同様の理由によりlossではなくwinを使って計算する。
		 */
		if(a.endNode && a.loss == a.nodeCount && b.endNode && b.loss == b.nodeCount) return 0;
		else if(a.endNode && a.loss == a.nodeCount) return 1;
		else if(b.endNode && b.loss == b.nodeCount) return -1;
		else if((double)a.loss * (double)b.nodeCount > (double)b.loss * (double)a.nodeCount) return 1;
		else if((double)a.loss * (double)b.nodeCount < (double)b.loss * (double)a.nodeCount) return -1;
		else if((double)a.win * (double)b.nodeCount < (double)b.win * (double)a.nodeCount) return 1;
		else if((double)a.win * (double)b.nodeCount > (double)b.win * (double)a.nodeCount) return -1;
		else return 0;
	};


	public MonteCarloUCB1AIPlayer(Color color, GameCondition gameCondition)
	{
		rnd = new Random();
		debug = AIPlayerUtil.getBooleanParameter(gameCondition, "debug").orElse(false);
		numberOfNodesThreshold = AIPlayerUtil.getIntParameter(gameCondition, "threshold").orElse(100);
	}

	@Override
	public Point decide(Board board, Color color, long givenMillisPerTurn, long remainingMillisInGame) {
		long limit = Math.min(givenMillisPerTurn,
				remainingMillisInGame / (Arrays.stream(GameNode.points)
										.filter(p -> board.colorAt(p) == null)
										.count() + 1) / 2);
		try {
			return search(board, color, Instant.now().plusMillis(Math.max(0, limit - marginT))).orElse(null);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public Optional<Point> search(Board board, Color player, Instant deadline)
	{
		GameNode rootNode = new GameNode(player, board);

		if(!Instant.now().isBefore(deadline))
		{
			if(rootNode.nextPoints.size() == 0) return Optional.empty();
			else return Optional.of(rootNode.nextPoints.get(rnd.nextInt(rootNode.nextPoints.size())));
		}
		else
		{
			while(Instant.now().isBefore(deadline) && !rootNode.searchEnded())
			{
				if(rootNode.playout(rnd, deadline, numberOfNodesThreshold) == false) break;
			}

			if(debug)
			{
				rootNode.children
					.forEach(c -> System.out.println(String.format("win = %d, nodeCount = %d", c.loss, c.nodeCount)));
			}

			if(rootNode.children.size() == 0) return Optional.empty();
			else return Collections.max(rootNode.children, candidateComparator).move;
		}
	}
}
