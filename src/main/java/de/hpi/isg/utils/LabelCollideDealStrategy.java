package de.hpi.isg.utils;

/**
 * @author Lan Jiang
 * @since 9/17/19
 */
public enum LabelCollideDealStrategy {
    /**
     * Overwrite the collided lines with the new line type.
     */
    OVERWRITE,

    /**
     * Do not change the line type, pop up a prompt dialog to the user
     */
    PROMPT
}
