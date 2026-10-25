package com.phatvv9.wwarning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.phatvv9.wwarning.util.ReputationPolicy;

import org.junit.Test;

/** Khóa các hằng số điểm uy tín để không bị đổi nhầm (README/rules phụ thuộc vào chúng). */
public class ReputationPolicyTest {

    @Test
    public void initialScore_is100() {
        assertEquals(100, ReputationPolicy.INITIAL_SCORE);
    }

    @Test
    public void voteRewardIsPositive_hiddenPenaltyIsNegative() {
        assertTrue(ReputationPolicy.POINTS_PER_VOTE > 0);
        assertTrue(ReputationPolicy.HIDDEN_PENALTY < 0);
    }
}
