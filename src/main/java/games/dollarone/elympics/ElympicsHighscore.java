package games.dollarone.elympics;

import java.math.BigInteger;

public class ElympicsHighscore implements Comparable<ElympicsHighscore> {

    private String name;
    private BigInteger score;

    public ElympicsHighscore(String name, BigInteger score) {
        this.name = name;
        this.score = score;
    }

    public ElympicsHighscore(String name, long score) {
        this(name, BigInteger.valueOf(score));
    }

    protected ElympicsHighscore() {

    }

    public String getName() {
        return name;
    }

    public BigInteger getScore() {
        return score;
    }

    @Override
    public int compareTo(ElympicsHighscore other) {
        if (other.getScore() == null) {
            if (getScore() == null) {
                return 0;
            } else {
                return BigInteger.valueOf(0).compareTo(getScore());
            }
        } else if (getScore() == null) {
            return other.getScore().compareTo(BigInteger.valueOf(0));
        } else {
            return other.getScore().compareTo(getScore());
        }
    }
}
